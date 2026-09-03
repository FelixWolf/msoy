//
// $Id$

package com.threerings.msoy.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.java_websocket.WebSocket;
import org.java_websocket.framing.CloseFrame;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import static com.threerings.msoy.Log.log;

/**
 * Proxies WebSocket connections through to our raw game server ports, for clients (like Ruffle)
 * that can only speak WebSocket and cannot open a raw TCP socket from the browser. Requests come
 * in as <code>/ws/&lt;host&gt;/&lt;port&gt;</code>; we validate the requested target against our
 * own configured ports before opening a real TCP connection to it and pumping bytes through
 * unmodified (as binary WebSocket frames) in both directions.
 */
public class MsoyWebSocketProxy extends WebSocketServer
{
    public MsoyWebSocketProxy (InetSocketAddress address)
    {
        super(address);
    }

    @Override
    public void onOpen (WebSocket conn, ClientHandshake handshake)
    {
        String path = handshake.getResourceDescriptor();
        Matcher m = TARGET_PATTERN.matcher(path == null ? "" : path);
        if (!m.matches()) {
            log.warning("Rejecting WebSocket proxy request with unparseable path", "path", path);
            conn.close(CloseFrame.POLICY_VALIDATION, "invalid target");
            return;
        }

        String host = m.group(1);
        int port = Integer.parseInt(m.group(2));
        if (!isValidTarget(host, port)) {
            log.warning("Rejecting WebSocket proxy request for disallowed target",
                "host", host, "port", port);
            conn.close(CloseFrame.POLICY_VALIDATION, "invalid target");
            return;
        }

        // attach a target immediately, synchronously, so onMessage never sees a null attachment
        // for a connection that passed validation: any bytes that arrive before the downstream
        // connect (on its own thread, below) finishes get buffered by the target and flushed
        // once it's actually connected
        Target target = new Target();
        conn.setAttachment(target);

        // connect to the real target on its own thread: this is expected to be a handful of
        // concurrent connections at most, so a thread-per-connection blocking connect+pump is
        // simpler and safer here than adding a second NIO layer on top of the one this library
        // already runs internally
        new Thread(new ConnectAndPump(conn, target, host, port),
            "ws-proxy-connect-" + host + ":" + port).start();
    }

    @Override
    public void onMessage (WebSocket conn, ByteBuffer message)
    {
        Target target = (Target)conn.getAttachment();
        if (target == null) {
            return; // shouldn't happen: onOpen always attaches a target before we could get here
        }
        byte[] bytes = new byte[message.remaining()];
        message.get(bytes);
        try {
            target.send(bytes);
        } catch (IOException ioe) {
            log.warning("Failed to forward WebSocket proxy message to target", ioe);
            conn.close();
        }
    }

    @Override
    public void onMessage (WebSocket conn, String message)
    {
        // our proxy protocol is binary-only (it carries raw socket bytes); ignore stray text
        // frames rather than treating them as a protocol error
    }

    @Override
    public void onClose (WebSocket conn, int code, String reason, boolean remote)
    {
        Target target = (Target)conn.getAttachment();
        if (target != null) {
            target.close();
        }
    }

    @Override
    public void onError (WebSocket conn, Exception ex)
    {
        log.warning("WebSocket proxy error", ex);
        if (conn != null) {
            Target target = (Target)conn.getAttachment();
            if (target != null) {
                target.close();
            }
        }
    }

    @Override
    public void onStart ()
    {
        log.info("WebSocket proxy listening", "port", getPort());
    }

    /**
     * Returns true if we're willing to proxy a connection to the given host/port: it must be one
     * of our own configured game ports, and (for now, single-node only) this node's own host.
     */
    public boolean isValidTarget (String host, int port)
    {
        if (!(host.equalsIgnoreCase(ServerConfig.serverHost) ||
                host.equalsIgnoreCase(ServerConfig.backChannelHost) ||
                host.equalsIgnoreCase("localhost"))) {
            return false;
        }
        if (port == ServerConfig.gameServerPort || port == ServerConfig.socketPolicyPort) {
            return true;
        }
        for (int serverPort : ServerConfig.serverPorts) {
            if (port == serverPort) {
                return true;
            }
        }
        return false;
    }

    /**
     * Connects to the real proxy target and, once connected, pumps bytes read from it back to
     * the WebSocket connection until either side closes.
     */
    protected class ConnectAndPump implements Runnable
    {
        public ConnectAndPump (WebSocket conn, Target target, String host, int port)
        {
            _conn = conn;
            _target = target;
            _host = host;
            _port = port;
        }

        public void run ()
        {
            Socket socket = new Socket();
            InputStream in;
            try {
                socket.connect(new InetSocketAddress(_host, _port), CONNECT_TIMEOUT);
                in = socket.getInputStream();
                _target.connected(socket, socket.getOutputStream());
            } catch (IOException ioe) {
                log.warning("Failed to connect WebSocket proxy target",
                    "host", _host, "port", _port, ioe);
                closeQuietly(socket);
                _conn.close(CloseFrame.UNEXPECTED_CONDITION, "connect failed");
                return;
            }

            byte[] buffer = new byte[8192];
            try {
                int read;
                while ((read = in.read(buffer)) != -1) {
                    _conn.send(ByteBuffer.wrap(buffer, 0, read));
                }
            } catch (IOException ioe) {
                // expected once the target (or our own onClose) closes the underlying socket
            } finally {
                _target.close();
                _conn.close();
            }
        }

        protected final WebSocket _conn;
        protected final Target _target;
        protected final String _host;
        protected final int _port;
    }

    /**
     * The real TCP connection to a proxy target, attached to its WebSocket connection. Created
     * (unconnected) synchronously in {@code onOpen} so that {@code onMessage} always has
     * something to hand bytes to; those bytes are buffered here until the downstream connect
     * (which happens on another thread) actually completes.
     */
    protected static class Target
    {
        /**
         * Sends (or, if we're not connected yet, buffers) a chunk of bytes to the target.
         */
        public synchronized void send (byte[] bytes)
            throws IOException
        {
            if (_closed) {
                return;
            }
            if (_out == null) {
                _pending.add(bytes);
                return;
            }
            _out.write(bytes);
            _out.flush();
        }

        /**
         * Called once the downstream connect succeeds: flushes any bytes that arrived while we
         * were still connecting, then allows {@link #send} to write straight through.
         */
        public synchronized void connected (Socket socket, OutputStream out)
            throws IOException
        {
            if (_closed) {
                closeQuietly(socket);
                return;
            }
            _socket = socket;
            _out = out;
            for (byte[] bytes : _pending) {
                _out.write(bytes);
            }
            _out.flush();
            _pending.clear();
        }

        public synchronized void close ()
        {
            if (_closed) {
                return;
            }
            _closed = true;
            _pending.clear();
            if (_socket != null) {
                closeQuietly(_socket);
            }
        }

        protected Socket _socket;
        protected OutputStream _out;
        protected boolean _closed;
        protected final List<byte[]> _pending = new ArrayList<byte[]>();
    }

    protected static void closeQuietly (Socket socket)
    {
        try {
            socket.close();
        } catch (IOException ioe) {
            // ignored: we're already tearing this connection down
        }
    }

    /** Matches the "/ws/&lt;host&gt;/&lt;port&gt;" request path. */
    public static final Pattern TARGET_PATTERN = Pattern.compile("^/ws/([^/]+)/([0-9]+)$");

    /** How long we'll wait for the downstream TCP connect to succeed. */
    protected static final int CONNECT_TIMEOUT = 5000;
}
