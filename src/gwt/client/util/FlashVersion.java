//
// $Id$

package client.util;

import com.threerings.gwt.ui.WidgetUtil;

/**
 * Utility methods for checking the browser's current Flash version.
 */
public class FlashVersion
{
    /** Our required minimum flash client version. */
    public static final int[] MIN_FLASH_VERSION = { 9, 0, 115, 0 };

    /**
     * Configure the WidgetUtil to use the appropriate flash player version.
     */
    static {
        WidgetUtil.FLASH_VERSION =  "" + MIN_FLASH_VERSION[0] + "," + MIN_FLASH_VERSION[1] +
                                    "," + MIN_FLASH_VERSION[2] + "," + MIN_FLASH_VERSION[3];
    }

    /**
     * Checks that the Flash player that is installed is sufficiently new.
     *
     * @return null if everything is OK (including when we've fallen back to Ruffle), or a string
     * of HTML to display instead of the Flash client in the event that neither a working Flash
     * Player nor Ruffle can render it.
     */
    public static String checkFlashVersion (int width, int height)
    {
        // If they have the required flash, we're happy
        // If they're using IE, we'll let it handle upgrading/installing the flash activex control
        // since that works in most cases and we can't easily detect the cases where it doesn't
        if (hasFlashVersionNative(FULL_VERSION) || isIeNative()) {
            return null;
        }
        // some mac flash plugins are booched but work fine as installed, leave them alone
        if (isMacNative() && !hasFlashVersionNative(MAC_PASSTHROUGH_MAX) &&
                hasFlashVersionNative(MAC_PASSTHROUGH_MIN)) {
            return null;
        }
        // otherwise there's no sufficient native Flash Player: fall back to Ruffle instead of
        // showing an install/upgrade prompt (Flash Player itself is long EOL and no longer
        // installable), and proceed with the normal embed so Ruffle's polyfill can pick it up
        RuffleSupport.ensureRuffle();
        return null;
    }

    /**
     * Returns true if the browser has some version of Flash Player installed at all.
     */
    public static boolean hasFlashPlayer ()
    {
        return hasFlashVersionNative(ANY_VERSION);
    }

    /**
     * Checks for a minimum flash version.
     */
    protected static native boolean hasFlashVersionNative (String version) /*-{
        return $wnd.swfobject.hasFlashPlayerVersion(version);
    }-*/;

    /**
     * Returns true if we're in internet explorer.
     */
    protected static native boolean isIeNative () /*-{
        return $wnd.swfobject.ua.ie;
    }-*/;

    /**
     * Returns true if we're on a mac.
     */
    protected static native boolean isMacNative () /*-{
        return $wnd.swfobject.ua.mac;
    }-*/;

    /** The minimum flash for full functionality. */
    protected static final String FULL_VERSION =
        "" + MIN_FLASH_VERSION[0] + "." + MIN_FLASH_VERSION[1] + "." + MIN_FLASH_VERSION[2];

    /** A range of mac versions we'll let through since they have problems with express install. */
    protected static final String MAC_PASSTHROUGH_MIN = "9.0.0";
    protected static final String MAC_PASSTHROUGH_MAX = "9.0.49";

    /** See if they have any flash at all. */
    protected static final String ANY_VERSION = "0.0.1";
}
