//
// $Id$

package client.util;

import com.threerings.msoy.data.all.DeploymentConfig;

/**
 * Loads Ruffle (a Flash Player emulator) onto the page when a native Flash Player isn't
 * available. Ruffle's own polyfill picks up the {@code <object>}/{@code <embed>} markup that
 * {@link FlashClients} already produces, so this class only needs to (a) get its loader script
 * onto the page, and (b) for content that opens a raw socket (the world client), tell Ruffle to
 * tunnel that connection through our server-side WebSocket proxy, since browsers can't open raw
 * TCP sockets themselves.
 */
public class RuffleSupport
{
    /**
     * Ensures Ruffle's loader script is present on the page. Safe to call repeatedly or from
     * multiple call sites; only the first call actually injects anything.
     */
    public static void ensureRuffle ()
    {
        setBaseConfigNative();
        maybeInject();
    }

    /**
     * Like {@link #ensureRuffle()}, but also registers <code>host</code>/<code>port</code> with
     * Ruffle's WebSocket socket-proxy config, so that an embedded SWF's
     * <code>flash.net.Socket</code> connection to that host/port gets tunneled through our
     * server-side proxy. Must be called before Ruffle's loader script has actually executed
     * (i.e. as part of the same embed call that needs it, not after the fact) -- once Ruffle has
     * loaded, later config changes don't retroactively apply to it.
     */
    public static void ensureRuffle (String host, int port)
    {
        setBaseConfigNative();
        addSocketProxyNative(host, port, "ws://" + DeploymentConfig.serverHost + ":" +
            DeploymentConfig.wsProxyPort + "/ws/" + host + "/" + port);
        maybeInject();
    }

    protected static void maybeInject ()
    {
        if (_injected) {
            return;
        }
        _injected = true;
        injectScriptNative();
    }

    protected static native void setBaseConfigNative () /*-{
        $wnd.RufflePlayer = $wnd.RufflePlayer || {};
        var config = $wnd.RufflePlayer.config = $wnd.RufflePlayer.config || {};
        // hide Ruffle's own "click to unmute" overlay: we don't want an extra prompt on top of
        // the world client
        config.unmuteOverlay = 'hidden';
        // disable Ruffle's right-click menu, which otherwise steals the world client's own
        // context menu
        config.contextMenu = 'off';
        // keep running (audio, sockets, etc.) when the tab is backgrounded/hidden, instead of
        // Ruffle pausing playback, since this is a persistent world/chat client, not a video
        config.backgroundExecutionMode = 'mainThread';
        // always start playing immediately instead of showing Ruffle's own click-to-play
        // overlay (the default 'auto' mode shows it unless it can confirm autoplay is allowed)
        config.autoplay = 'on';
    }-*/;

    protected static native void addSocketProxyNative (String host, int port, String proxyUrl) /*-{
        $wnd.RufflePlayer = $wnd.RufflePlayer || {};
        var config = $wnd.RufflePlayer.config = $wnd.RufflePlayer.config || {};
        var proxies = config.socketProxy = config.socketProxy || [];
        proxies.push({ host: host, port: port, proxyUrl: proxyUrl });
    }-*/;

    protected static native void injectScriptNative () /*-{
        var script = $doc.createElement('script');
        script.src = '/js/ruffle/ruffle.js';
        $doc.head.appendChild(script);
    }-*/;

    /** Whether we've already injected Ruffle's loader script onto this page. */
    protected static boolean _injected;
}
