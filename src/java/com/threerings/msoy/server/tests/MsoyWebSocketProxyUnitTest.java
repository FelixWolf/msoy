//
// $Id$

package com.threerings.msoy.server.tests;

import java.net.InetSocketAddress;

import org.junit.Test;

import com.threerings.msoy.server.MsoyWebSocketProxy;
import com.threerings.msoy.server.ServerConfig;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link MsoyWebSocketProxy}.
 */
public class MsoyWebSocketProxyUnitTest
{
    @Test public void testTargetPatternParsing ()
    {
        assertTrue(MsoyWebSocketProxy.TARGET_PATTERN.matcher("/ws/localhost/47624").matches());
        assertFalse(MsoyWebSocketProxy.TARGET_PATTERN.matcher("/ws/localhost").matches());
        assertFalse(
            MsoyWebSocketProxy.TARGET_PATTERN.matcher("/ws/localhost/47624/extra").matches());
        assertFalse(MsoyWebSocketProxy.TARGET_PATTERN.matcher("/ws/localhost/notaport").matches());
        assertFalse(MsoyWebSocketProxy.TARGET_PATTERN.matcher("nope").matches());
    }

    @Test public void testIsValidTarget ()
    {
        MsoyWebSocketProxy proxy = new MsoyWebSocketProxy(new InetSocketAddress(0));

        // one of our own configured game ports, on a host we recognize as ourselves
        assertTrue(proxy.isValidTarget("localhost", ServerConfig.serverPorts[0]));
        assertTrue(proxy.isValidTarget(ServerConfig.serverHost, ServerConfig.gameServerPort));
        assertTrue(proxy.isValidTarget("localhost", ServerConfig.socketPolicyPort));

        // wrong port, or a host that isn't us: rejected
        assertFalse(proxy.isValidTarget("localhost", 9999));
        assertFalse(proxy.isValidTarget("evil.example.com", ServerConfig.serverPorts[0]));
    }
}
