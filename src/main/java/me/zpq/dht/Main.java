package me.zpq.dht;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import me.zpq.dht.client.PeerClient;
import me.zpq.dht.impl.JsonMetaInfoImpl;
import me.zpq.dht.impl.MongoMetaInfoImpl;
import me.zpq.dht.exception.TryAgainException;
import me.zpq.dht.model.MetaInfoRequest;
import me.zpq.dht.model.NodeTable;
import me.zpq.dht.scheduled.FindNode;
import me.zpq.dht.scheduled.Peer;
import me.zpq.dht.scheduled.Ping;
import me.zpq.dht.scheduled.RemoveNode;
import me.zpq.dht.server.DiscardServerHandler;
import me.zpq.dht.util.Helper;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;

/**
 * @author zpq
 * @date 2019-08-28
 */
public class Main {

    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws InterruptedException, IOException {

        ((LoggerContext) LoggerFactory.getILoggerFactory()).getLogger("org.mongodb.driver").setLevel(Level.ERROR);
        ClassLoader classLoader = Main.class.getClassLoader();
        URL url = classLoader.getResource("config.properties");
        if (url == null) {

            LOGGER.error("error config");
            return;
        }
        InputStream inputStream = Files.newInputStream(Paths.get(url.getFile()));
        Properties properties = new Properties();
        properties.load(inputStream);
        String host = properties.getProperty("serverIp");
        Integer port = Integer.valueOf(properties.getProperty("serverPort"));
        String peerId = properties.getProperty("peerId");
        byte[] transactionId = properties.getProperty("transactionID").getBytes();
        Integer minNodes = Integer.valueOf(properties.getProperty("minNodes"));
        Integer maxNodes = Integer.valueOf(properties.getProperty("maxNodes"));
        Integer timeout = Integer.valueOf(properties.getProperty("timeout"));
        Integer corePoolSize = Integer.valueOf(properties.getProperty("corePoolSize"));
        Integer maximumPoolSize = Integer.valueOf(properties.getProperty("maximumPoolSize"));
        String redisHost = properties.getProperty("redis.host");
        Integer redisPort = Integer.valueOf(properties.getProperty("redis.port"));
        GenericObjectPoolConfig genericObjectPoolConfig = new GenericObjectPoolConfig();
        genericObjectPoolConfig.setMaxTotal(maximumPoolSize * 2);
        genericObjectPoolConfig.setMaxIdle(maximumPoolSize);
        genericObjectPoolConfig.setMinIdle(corePoolSize);
        JedisPool jedisPool = new JedisPool(genericObjectPoolConfig, redisHost, redisPort);
        Bootstrap bootstrap = new Bootstrap();
        byte[] nodeId = Helper.nodeId();
        Map<String, NodeTable> table = new Hashtable<>();
        table.put(new String(nodeId), new NodeTable(Helper.bytesToHex(nodeId), host, port, System.currentTimeMillis()));
        bootstrap.group(new NioEventLoopGroup())
                .channel(NioDatagramChannel.class)
                .option(ChannelOption.SO_BROADCAST, true)
                .handler(new DiscardServerHandler(table, nodeId, maxNodes, jedisPool.getResource()));
        final Channel channel = bootstrap.bind(host, port).sync().channel();

        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(5);
        LOGGER.info("start autoFindNode");
        scheduledExecutorService.scheduleWithFixedDelay(new FindNode(channel, transactionId, nodeId, table, minNodes), 2, 2, TimeUnit.SECONDS);
        LOGGER.info("start ok autoFindNode");

        LOGGER.info("start Ping");
        scheduledExecutorService.scheduleWithFixedDelay(new Ping(channel, transactionId, nodeId, table), 5, 20, TimeUnit.SECONDS);
        LOGGER.info("start ok Ping");

        LOGGER.info("start RemoveNode");
        scheduledExecutorService.scheduleWithFixedDelay(new RemoveNode(table, timeout), 30, 60, TimeUnit.SECONDS);
        LOGGER.info("start ok RemoveNode");

        LOGGER.info("start peerRequestTask");
        ThreadFactory threadFactory = Executors.defaultThreadFactory();
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(corePoolSize, maximumPoolSize,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(), threadFactory);
        MetaInfo mongoMetaInfo = new MongoMetaInfoImpl("mongodb://localhost");
        scheduledExecutorService.scheduleWithFixedDelay(new Peer(threadPoolExecutor, mongoMetaInfo, jedisPool, peerId), 2, 2, TimeUnit.SECONDS);
        LOGGER.info("start ok peerRequestTask");
        LOGGER.info("server ok");
    }
}
