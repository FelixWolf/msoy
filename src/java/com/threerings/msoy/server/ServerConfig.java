//
// $Id$

package com.threerings.msoy.server;

import java.io.File;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.primitives.Ints;

import com.samskivert.util.Config;
import com.samskivert.util.StringUtil;

import com.samskivert.jdbc.ConnectionProvider;

import com.threerings.presents.client.Client;

import com.threerings.messaging.amqp.AMQPMessageConfig;

import com.threerings.msoy.game.gwt.GameInfo;
import com.threerings.msoy.server.persist.RepositoryUtil;

import static com.threerings.msoy.Log.log;

/**
 * Provides access to installation specific configuration. Properties that
 * are specific to a particular Whirled server installation are accessed via
 * this class.
 */
public class ServerConfig
{
    /** Provides access to our config properties. */
    public static final Config config = new Config("msoy-server");

    /** The name assigned to this server node. */
    public static String nodeName;

    /** The unique id associated with this node. */
    public static int nodeId;

    /** The root directory of the server installation. */
    public static File serverRoot;

    /** The publicly accessible DNS name of the host on which this server is running. */
    public static String serverHost;

    /** The back-channel DNS name of the host on which this server is running. */
    public static String backChannelHost;

    /** The ports on which we are listening for client connections. */
    public static int[] serverPorts;

    /** The port on which we are listening for HTTP connections. */
    public static int httpPort;

    /** The port on which game servers start listening. */
    public static int gameServerPort;

    /** A secret used wherever we need secret information shared between servers or between the
     * server now and the server in the near future. */
    public static String sharedSecret;

    /** The local directory where dictionary files are stored. */
    public static File dictionaryDir;

    /** The local directory into which uploaded media is stored. */
    public static File mediaDir;

    /** The URL from which we instruct clients to load their media. */
    public static String mediaURL;

    /** Enables S3 media storage. */
    public static boolean mediaS3Enable;

    /** The remote S3 bucket in which media is stored. */
    public static String mediaS3Bucket;

    /** The user ID used for S3 authentication. */
    public static String mediaS3Id;

    /** The secret key used for S3 authentication. */
    public static String mediaS3Key;
    
    /** The host for the S3 server. */
    public static String mediaS3Host;
    
    /** The port for the S3 server. */
    public static int mediaS3Port;
    
    /** If we use HTTPS. */
    public static boolean mediaS3Secure;

    /** The name of our distribution, if any, in the Cloudfront CDN. */
    public static String cloudDistribution;

    /** The user ID used for cloudfront authentication. */
    public static String cloudId;

    /** The secret key used for cloudfront authentication. */
    public static String cloudKey;

    /** The ID of the public key pair used exclusively to sign Cloudfront URLs. */
    public static String cloudSigningKeyId;

    /** The actual PEM-encoded private key identiied by {@link #cloudSigningKeyId}. */
    public static String cloudSigningKey;

    /** Event logging server spooling directory. */
    public static String eventRemoteURI;

    /** Event logging server spooling directory. */
    public static String eventLogSpoolDir;

    /** True if we want to display events in the log files as well. */
    public static boolean eventLogDebugDisplay;

    /** The port on which we listen to socket policy requests. */
    public static int socketPolicyPort;

    /** The ReCaptcha public key. */
    public static String recaptchaPublicKey;

    /** The ReCaptcha private key. */
    public static String recaptchaPrivateKey;

    /** The secret used to authenticate the bureau launching client. */
    public static String bureauSharedSecret;

    /** True if the game server kicks off local proceses for bureaus. */
    public static boolean localBureaus;

    /** The secret used to authenticate bureau window clients. */
    public static String windowSharedSecret;

    /** True if the server should restart when code changes. */
    public static boolean autoRestart;

    /** How many milliseconds an invoker unit should run before it's logged as a warning. */
    public static int longInvokerUnit;

    /**
     * Returns a provider of JDBC connections.
     */
    public static ConnectionProvider createConnectionProvider ()
        throws Exception
    {
        return RepositoryUtil.createConnectionProvider(config);
    }

    /**
     * Returns a URL that can be used to make HTTP requests from this server. The returned URL will
     * not need a slash appended to it.
     */
    public static String getServerURL ()
    {
        String defurl = "http://" + serverHost + (httpPort != 80 ? ":" + httpPort : "") + "/";
        return config.getValue("server_url", defurl);
    }

    /**
     * Returns the address from which automated emails are sent.
     */
    public static String getFromAddress ()
    {
        return config.getValue("from_address", "\"Whirled Mailbot\" <peas@whirled.com>");
    }

    /**
     * Returns the address for the Whirled Agents.
     */
    public static String getAgentsAddress ()
    {
        return config.getValue("agents_address", "\"Whirled Agents\" <msoy-agents@threerings.net>");
    }

    /**
     * Returns the address for bling cash out requests.
     */
    public static String getCashOutAddress ()
    {
        return config.getValue("cashout_address",
            "\"Whirled Cashouts\" <blingcashout@threerings.net>");
    }

    /**
     * Returns the HTTP port on which the specified node should be listening.
     */
    public static int getHttpPort (String nodeName)
    {
        return config.getValue(nodeName + ".http_port", httpPort);
    }

    /**
     * Returns the server and game ports the specified node should be using.  This is only used on
     * individual developers servers where they're running multiple nodes on one machine.
     */
    public static int[] getServerPorts (String nodeName)
    {
        String nodePortOffset = config.getValue("node_port_offset", "");
        Set<Integer> ports = Sets.newHashSet();
        if (!StringUtil.isBlank(nodePortOffset)) {
            try {
                // obtain the node id from the node name
                Matcher m = NODE_ID_PATTERN.matcher(nodeName);
                if (m.matches()) {
                    int nodeId = Integer.parseInt(m.group(1));
                    int offset = Integer.valueOf(nodePortOffset);
                    int[] serverPorts = config.getValue("server_ports", Client.DEFAULT_SERVER_PORTS);
                    int gameServerPort = config.getValue("game_server_port", 47265);
                    for (int port : serverPorts) {
                        ports.add(port + offset + nodeId);
                    }
                    ports.add(gameServerPort + offset + nodeId);
                }
            } catch (Exception e) {
                log.error("Bad nodePortOffset when asked for serverPorts", e);
            }
        }
        return Ints.toArray(ports);
    }

    /**
     * Returns the group id of the announcement group, or 0 if none is configured.
     */
    public static int getAnnounceGroupId ()
    {
        return config.getValue("announce_group_id", 0);
    }

    /**
     * Returns the group id of the bug tracking group, or 0 if none is configured.
     */
    public static int getIssueGroupId ()
    {
        return config.getValue("issue_group_id", 0);
    }

    /**
     * Returns the supplied group id if it is non-zero or the default groupId to use for games that
     * have no groupId configured if it is zero.
     */
    public static int getGameGroupId (int groupId)
    {
        return (groupId == GameInfo.NO_GROUP) ?
            config.getValue("default_game_group_id", 0) : groupId;
    }

    /**
     * Returns a list of sceneIds for winners of the most recent Design Your Whirled contest.
     * These are in order: first is "First Place", second "Second Place", third "Third Place", the
     * next 5 are "Honorable Mentions", and the rest (a varying number) are "Too Cool for School".
     * Defaults to 10 early ids if not configured.
     */
    public static int[] getContestWinningSceneIds ()
    {
        int[] defaultSceneIds = { 9, 7, 4, 12, 11, 5, 6, 8, 10 };
        return config.getValue("contest_winning_scene_ids", defaultSceneIds);
    }

    /**
     * Returns the configuration of the AMQP messaging server. If no messaging server is
     * configured, this will return null.
     */
    public static AMQPMessageConfig getAMQPMessageConfig ()
    {
        String addresses = config.getValue("messaging.server.addresses", "");
        if ("".equals(addresses)) {
            // No messaging server configured.
            return null;
        }
        return new AMQPMessageConfig(addresses,
            config.getValue("messaging.server.virtualhost", ""),
            config.getValue("messaging.server.username", ""),
            config.getValue("messaging.server.password", ""),
            config.getValue("messaging.server.realm", ""),
            config.getValue("messaging.server.heartbeat", 0));
    }

    /** The pattern via which we obtain our node id from our name. */
    protected static final Pattern NODE_ID_PATTERN = Pattern.compile("msoy([0-9]+)");

    /**
     * Configures server bits when this class is resolved.
     */
    static {
        // these will be overridden if we're running in cluster mode
        serverHost = config.getValue("server_host", "localhost");
        serverPorts = config.getValue("server_ports", Client.DEFAULT_SERVER_PORTS);
        httpPort = config.getValue("http_port", 8080);
        gameServerPort = config.getValue("game_server_port", 47625);
        socketPolicyPort = config.getValue("socket_policy_port", 47623);

        String hostname = System.getProperty("hostname");
        // we tell other local machines to connect to us using our back channel hostname
        backChannelHost = hostname;

        // if we're a server node (not the webapp or a tool) do some extra stuff
        if (Boolean.getBoolean("is_node")) {
            List<String> errors = Lists.newArrayList();

            // in a clustered configuration we must have a back channel
            if (StringUtil.isBlank(backChannelHost)) {
                errors.add("Missing 'hostname' system property.");
            }

            // if we have a host node pattern, use that to determine our node id and name
            String hostNodePattern = config.getValue("host_node_pattern", "");
            if (!StringUtil.isBlank(hostNodePattern)) {
                try {
                    Matcher m = Pattern.compile(hostNodePattern).matcher(hostname);
                    if (!m.matches()) {
                        errors.add("Unable to determine node from hostname [hostname=" + hostname +
                                   ", pattern=" + hostNodePattern + "].");
                    } else {
                        nodeId = Integer.parseInt(m.group(1));
                        nodeName = "msoy" + nodeId;
                    }
                } catch (Exception e) {
                    errors.add("Unable to determine node from hostname [hostname=" + hostname +
                               ", pattern=" + hostNodePattern + "].");
                    errors.add(e.getMessage());
                }

            } else {
                // otherwise we should get our node name as a system property
                nodeName = System.getProperty("node", "");
                // ...or we'll determine it from our hostname
                if (StringUtil.isBlank(nodeName)) {
                    errors.add("Must have either 'node' sysprop or 'host_node_pattern' config.");
                }

                // obtain our node id from the node name
                Matcher m = NODE_ID_PATTERN.matcher(nodeName);
                if (m.matches()) {
                    nodeId = Integer.parseInt(m.group(1));
                } else {
                    errors.add("Unable to determine node id from name [name=" + nodeName + "]. " +
                               "Node name must match pattern '" + NODE_ID_PATTERN + "'.");
                }
            }

            // configure our server hostname based on our server_host_pattern
            String hostPattern = config.getValue("server_host_pattern", "");
            if (!StringUtil.isBlank(hostPattern)) {
                try {
                    serverHost = String.format(hostPattern, nodeId);
                } catch (Exception e) {
                    errors.add("Invalid 'server_host_pattern' supplied: " + hostPattern);
                }
            }

            // if node_port_offset is specified, adjust our various ports
            String nodePortOffset = config.getValue("node_port_offset", "");
            if (!StringUtil.isBlank(nodePortOffset)) {
                try {
                    int offset = Integer.valueOf(nodePortOffset);
                    for (int ii = 0; ii < serverPorts.length; ii++) {
                        serverPorts[ii] = serverPorts[ii] + offset + nodeId;
                    }
                    httpPort = httpPort + offset + nodeId;
                    gameServerPort = gameServerPort + offset + nodeId;
                } catch (Exception e) {
                    errors.add("Invalid 'node_port_offset' supplied: " + nodePortOffset);
                }
            }

            // if we have any errors, report them and exit
            if (errors.size() > 0) {
                for (String error : errors) {
                    log.warning(error);
                }
                System.exit(255);
            }
        }

        // fill in our standard properties
        serverRoot = new File(config.getValue("server_root", "/tmp"));
        mediaDir = new File(config.getValue("media_dir", "/tmp"));
        mediaURL = config.getValue("media_url", "http://localhost:" + httpPort + "/media");
        mediaS3Enable = config.getValue("media_s3enable", false);
        mediaS3Bucket = config.getValue("media_s3bucket", "msoy");
        mediaS3Id = config.getValue("media_s3id", "id");
        mediaS3Key = config.getValue("media_s3key", "key");
        mediaS3Host = config.getValue("media_s3host", "s3-us-west-2.amazonaws.com");
        mediaS3Port = config.getValue("media_s3port", 80);
        mediaS3Secure = config.getValue("media_s3secure", false);
        cloudDistribution = config.getValue("cloud_distribution", "");
        cloudId = config.getValue("cloud_id", "id");
        cloudKey = config.getValue("cloud_key", "key");
        cloudSigningKeyId = config.getValue("cloud_signing_key_id", "id");
        cloudSigningKey = config.getValue("cloud_signing_key", "");
        sharedSecret = config.getValue("server_secret", "");
        eventRemoteURI = config.getValue("event_remote_uri", "");
        eventLogSpoolDir = config.getValue("event_log_spool_dir", "");
        eventLogDebugDisplay = config.getValue("event_log_debug", false);
        recaptchaPublicKey = config.getValue("recaptcha_public", "");
        recaptchaPrivateKey = config.getValue("recaptcha_private", "");
        bureauSharedSecret = config.getValue("bureau_secret", "");
        localBureaus = config.getValue("local_bureaus", true);
        windowSharedSecret = config.getValue("window_secret", "");
        autoRestart = config.getValue("auto_restart", false);
        longInvokerUnit = config.getValue("long_invoker_unit", 3000);
    }
}
