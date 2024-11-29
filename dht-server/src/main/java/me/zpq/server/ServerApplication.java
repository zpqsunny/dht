package me.zpq.server;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;
import me.zpq.dht.common.MemoryQueue;
import me.zpq.dht.common.Utils;
import me.zpq.route.IRoutingTable;
import me.zpq.route.RoutingTable;
import me.zpq.server.peer.Peer;
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

    // peer
    private static int CORE_POOL_SIZE = 5;

    private static int MAX_POOL_SIZE = 10;

    private static String MONGODB_URL = "mongodb://localhost";


    public static void main(String[] args) throws InterruptedException, IOException {

        readConfig();

        Bootstrap bootstrap = new Bootstrap();

        RoutingTable routingTable = new RoutingTable();

        MemoryQueue memoryQueue = new MemoryQueueImpl();

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
                                    .addLast(new DHTServerHandler(routingTable, NODE_ID, MAX_NODES, memoryQueue))
                            ;
                        }

                    });
            final Channel channel = bootstrap.bind(PORT).sync().channel();

            scheduled(channel, routingTable);

            startPeer(memoryQueue);

            log.info("server ok pid: {}", ManagementFactory.getRuntimeMXBean().getName());

            channel.closeFuture().await();

        } finally {
            group.shutdownGracefully();
        }

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

            CORE_POOL_SIZE = Integer.parseInt(properties.getProperty("peers.core.pool.size", String.valueOf(CORE_POOL_SIZE)));
            MAX_POOL_SIZE = Integer.parseInt(properties.getProperty("peers.maximum.pool.size", String.valueOf(MAX_POOL_SIZE)));
            MONGODB_URL = properties.getProperty("mongodb.url", MONGODB_URL);
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
        log.info("=> peers.core.pool.size: {}", CORE_POOL_SIZE);
        log.info("=> peers.maximum.pool.size: {}", MAX_POOL_SIZE);
        log.info("=> mongodb.url: {}", MONGODB_URL);

    }

    private static void readEnv() {
        // docker
        String port = System.getenv("PORT");
        String minNodes = System.getenv("MIN_NODES");
        String maxNodes = System.getenv("MAX_NODES");
        String mongodbUrl = System.getenv("MONGODB_URL");
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
        if (mongodbUrl != null && !mongodbUrl.isEmpty()) {
            log.info("=> env MONGODB_URL: {}", mongodbUrl);
            MONGODB_URL = mongodbUrl;
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

    private static MongoClient mongo(String mongoUri) {

        MongoClientSettings.Builder mongoClientSettings = MongoClientSettings.builder();
        ConnectionString connectionString = new ConnectionString(mongoUri);
        mongoClientSettings.applyConnectionString(connectionString);
        mongoClientSettings.applyToSocketSettings(builder ->
                builder.connectTimeout(30, TimeUnit.SECONDS)
                        .readTimeout(1, TimeUnit.MINUTES)
        );
        mongoClientSettings.applyToConnectionPoolSettings(builder ->
                builder.minSize(2).maxSize(5)
        );
        return MongoClients.create(mongoClientSettings.build());
    }

    private static void startPeer(MemoryQueue memoryQueue) {

        MongoClient mongoClient = mongo(MONGODB_URL);

        ThreadFactory threadFactory = Executors.defaultThreadFactory();
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(CORE_POOL_SIZE, MAX_POOL_SIZE,
                0L, TimeUnit.MINUTES, new LinkedBlockingQueue<>(), threadFactory);

        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(2);
        EventLoopGroup group = new NioEventLoopGroup(MAX_POOL_SIZE);
        Bootstrap b = new Bootstrap();
        b.group(group)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.AUTO_READ, true)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
        ;
        scheduledExecutorService.scheduleWithFixedDelay(new Peer(memoryQueue, mongoClient, threadPoolExecutor, b), 1L, 1L, TimeUnit.SECONDS);

    }
}
