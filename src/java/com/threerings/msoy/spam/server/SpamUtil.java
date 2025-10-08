//
// $Id$

package com.threerings.msoy.spam.server;

import java.io.StringWriter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;

import com.samskivert.util.StringUtil;

import com.samskivert.velocity.VelocityUtil;

import com.threerings.msoy.server.ServerConfig;

import static com.threerings.msoy.Log.log;

/**
 * Contains utilities relating to our mass mailing services.
 */
public class SpamUtil
{
    /**
     * Generates an opt-out hash for the supplied member.
     */
    public static String generateOptOutHash (int memberId, String email)
    {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA");
            String text = memberId + email + ServerConfig.sharedSecret;
            digest.update(text.getBytes());
            return StringUtil.hexlate(digest.digest());
        } catch (NoSuchAlgorithmException nsa) {
            throw new RuntimeException(nsa.getMessage());
        }
    }

    /**
     * Wraps the supplied (HTML) spam body in some basic necessaries.
     */
    public static String formatSpam (String body)
    {
        // convert the body into proper-ish HTML
        try {
            StringWriter swout = new StringWriter();
            VelocityContext ctx = new VelocityContext();
            ctx.put("base_url", ServerConfig.getServerURL());
            ctx.put("content", body);
            VelocityEngine ve = VelocityUtil.createEngine();
            ve.mergeTemplate("rsrc/email/wrapper/message.html", "UTF-8", ctx, swout);
            return swout.toString();

        } catch (Exception e) {
            log.warning("Unable to format spam message", e);
            return null;
        }
    }

    public static String customizeSpam (String body, int memberId, String email)
    {
        return body.replace("%OPTOUTBITS%", generateOptOutHash(memberId, email) + "_" + memberId);
    }

    /**
     * Generates the headers needed by Return Path to track our mails.
     */
    public static String[] makeSpamHeaders (String subject)
    {
        return new String[] {
            RP_CAMPAIGN_HEADER, RP_CAMPAIGN_PREFIX + subject.toLowerCase().replace(" ", "_"),
        };
    }

    /**
     * Returns the list of Return Path addresses to which we should also send our mass mailings so
     * that we can get information on how well they are getting delivered.
     */
    public static String[] getReturnPathAddrs ()
    {
        return RETURNPATH_ADDRS;
    }

    protected static final String RP_CAMPAIGN_HEADER = "X-campaignid";
    protected static final String RP_CAMPAIGN_PREFIX = "threeringsdesign_";

    /** 980 email accounts that we include in our user spammage so that we can have Return Path
     * tell us whether or not our mails are getting through. */
    protected static final String[] RETURNPATH_ADDRS = new String[] {
    };
}
