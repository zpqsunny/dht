package me.zpq.server;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.resource.DefaultClientResources;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import lombok.extern.slf4j.Slf4j;
import me.zpq.dht.common.Utils;
import me.zpq.route.IRoutingTable;
import me.zpq.route.RoutingTable;
import me.zpq.server.schedule.FindNode;
import me.zpq.server.schedule.Ping;
import me.zpq.server.schedule.RemoveNode;

import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.concurrent.*;

/**
 * @author zpq
 * @date 2020/7/23
 */
@Slf4j
public class ServerApplication {

    private static int PORT = 6881;

    private static final byte[] NODE_ID = Utils.nodeId();

    private static final byte[] TRANSACTION_ID = Utils.transactionId();

    private static int MIN_NODES = 20;

    private static int MAX_NODES = 5000;

    private static int FIND_NODE_INTERVAL = 60;

    private static int PING_INTERVAL = 300;

    private static int REMOVE_NODE_INTERVAL = 300;

    private static boolean FRESH = false;

    private static String REDIS_HOST = "127.0.0.1";

    private static int REDIS_PORT = 6379;

    private static String REDIS_PASSWORD = "";

    private static int REDIS_DATABASE = 0;

    public static void main(String[] args) throws InterruptedException, IOException {

        readConfig();

        Bootstrap bootstrap = new Bootstrap();

        RoutingTable routingTable = new RoutingTable();

        RedisCommands<String, String> redis = redis();

        NioEventLoopGroup group = new NioEventLoopGroup();

        try {

            bootstrap.group(group)
                    .channel(NioDatagramChannel.class)
                    .option(ChannelOption.SO_BROADCAST, true)
                    .handler(new ChannelInitializer<NioDatagramChannel>() {
                        @Override
                        protected void initChannel(NioDatagramChannel ch) {
                            ch.pipeline()
                                    .addLast(new DHTRequestDecoder())
                                    .addLast(new DHTResponseEncoder())
                                    .addLast(new DHTServerHandler(routingTable, NODE_ID, MAX_NODES, FRESH, redis))
                            ;
                        }

                    });
            final Channel channel = bootstrap.bind(PORT).sync().channel();

            scheduled(channel, routingTable);

            log.info("server ok pid: {}", ManagementFactory.getRuntimeMXBean().getName());

            channel.closeFuture().await();

        } finally {
            group.shutdownGracefully();
        }

    }

    private static RedisCommands<String, String> redis() {

        DefaultClientResources.Builder resourceBuild = DefaultClientResources.builder();
        RedisURI.Builder builder = RedisURI.builder();
        builder.withHost(REDIS_HOST);
        builder.withPort(REDIS_PORT);
        builder.withPassword(REDIS_PASSWORD.toCharArray());
        builder.withDatabase(REDIS_DATABASE);
        RedisClient redisClient = RedisClient.create(resourceBuild.build(), builder.build());
        StatefulRedisConnection<String, String> connection = redisClient.connect();
        return connection.sync();
    }

    private static void readConfig() throws IOException {

        String dir = System.getProperty("user.dir");
        Path configFile = Paths.get(dir + "/config.properties");
        if (Files.exists(configFile)) {

            log.info("=> read config...");
            InputStream inputStream = Files.newInputStream(configFile);
            Properties properties = new Properties();
            properties.load(inputStream);
            PORT = Integer.parseInt(properties.getProperty("server.port", String.valueOf(PORT)));
            MIN_NODES = Integer.parseInt(properties.getProperty("server.nodes.min"));
            MAX_NODES = Integer.parseInt(properties.getProperty("server.nodes.max"));
            FIND_NODE_INTERVAL = Integer.parseInt(properties.getProperty("server.findNode.interval"));
            PING_INTERVAL = Integer.parseInt(properties.getProperty("server.ping.interval"));
            REMOVE_NODE_INTERVAL = Integer.parseInt(properties.getProperty("server.removeNode.interval"));
            FRESH = Boolean.parseBoolean(properties.getProperty("server.fresh"));
            REDIS_HOST = properties.getProperty("redis.host", REDIS_HOST);
            REDIS_PORT = Integer.parseInt(properties.getProperty("redis.port", String.valueOf(REDIS_PORT)));
            REDIS_PASSWORD = properties.getProperty("redis.password", REDIS_PASSWORD);
            REDIS_DATABASE = Integer.parseInt(properties.getProperty("redis.database", String.valueOf(REDIS_DATABASE)));
            inputStream.close();
        }

        readEnv();

        log.info("==========>");
        log.info("=> server.port: {}", PORT);
        log.info("=> server.nodes.min: {}", MIN_NODES);
        log.info("=> server.nodes.max: {}", MAX_NODES);
        log.info("=> server.findNode.interval: {}", FIND_NODE_INTERVAL);
        log.info("=> server.ping.interval: {}", PING_INTERVAL);
        log.info("=> server.removeNode.interval: {}", REMOVE_NODE_INTERVAL);
        log.info("=> server.fresh: {}", FRESH);
        log.info("=> redis.host: {}", REDIS_HOST);
        log.info("=> redis.port: {}", REDIS_PORT);
        log.info("=> redis.password: {}", REDIS_PASSWORD);
        log.info("=> redis.database: {}", REDIS_DATABASE);

    }

    private static void readEnv() {
        // docker
        String port = System.getenv("PORT");
        String minNodes = System.getenv("MIN_NODES");
        String maxNodes = System.getenv("MAX_NODES");
        String fresh = System.getenv("FRESH");
        String redisHost = System.getenv("REDIS_HOST");
        String redisPort = System.getenv("REDIS_PORT");
        String redisPassword = System.getenv("REDIS_PASSWORD");
        String redisDatabase = System.getenv("REDIS_DATABASE");
        if (port != null && !port.isEmpty()) {
            log.info("=> env PORT: {}", port);
            PORT = Integer.parseInt(port);
        }
        if (minNodes != null && !minNodes.isEmpty()) {
            log.info("=> env MIN_NODES: {}", minNodes);
            MIN_NODES = Integer.parseInt(minNodes);
        }
        if (maxNodes != null && !maxNodes.isEmpty()) {
            log.info("=> env MAX_NODES: {}", maxNodes);
            MAX_NODES = Integer.parseInt(maxNodes);
        }
        if (fresh != null && !fresh.isEmpty()) {
            log.info("=> env FRESH: {}", fresh);
            FRESH = Boolean.parseBoolean(fresh);
        }
        if (redisHost != null && !redisHost.isEmpty()) {
            log.info("=> env REDIS_HOST: {}", redisHost);
            REDIS_HOST = redisHost;
        }
        if (redisPort != null && !redisPort.isEmpty()) {
            log.info("=> env REDIS_PORT: {}", redisPort);
            REDIS_PORT = Integer.parseInt(redisPort);
        }
        if (redisPassword != null && !redisPassword.isEmpty()) {
            log.info("=> env REDIS_PASSWORD: {}", redisPassword);
            REDIS_PASSWORD = redisPassword;
        }
        if (redisDatabase != null && !redisDatabase.isEmpty()) {
            log.info("=> env REDIS_DATABASE: {}", redisDatabase);
            REDIS_DATABASE = Integer.parseInt(redisDatabase);
        }

    }

    private static void scheduled(final Channel channel, IRoutingTable routingTable) {

        log.info("start autoFindNode");
        channel.eventLoop().scheduleWithFixedDelay(new FindNode(channel, TRANSACTION_ID, NODE_ID, routingTable, MIN_NODES), FIND_NODE_INTERVAL, FIND_NODE_INTERVAL, TimeUnit.SECONDS);
        log.info("start ok autoFindNode");
        log.info("start Ping");
        channel.eventLoop().scheduleWithFixedDelay(new Ping(channel, TRANSACTION_ID, NODE_ID, routingTable), PING_INTERVAL, PING_INTERVAL, TimeUnit.SECONDS);
        log.info("start ok Ping");
        log.info("start RemoveNode");
        channel.eventLoop().scheduleWithFixedDelay(new RemoveNode(routingTable), REMOVE_NODE_INTERVAL, REMOVE_NODE_INTERVAL, TimeUnit.SECONDS);
        log.info("start ok RemoveNode");
    }
}
