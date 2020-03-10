package me.zpq.dht;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import me.zpq.dht.model.NodeTable;
import me.zpq.dht.scheduled.FindNode;
import me.zpq.dht.scheduled.Ping;
import me.zpq.dht.scheduled.RemoveNode;
import me.zpq.dht.server.DiscardServerHandler;
import me.zpq.dht.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;

/**
 * @author zpq
 * @date 2019-08-28
 */
public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws InterruptedException, IOException {

        String dir = System.getProperty("user.dir");
        Path configFile = Paths.get(dir + "/config.properties");
        if (!Files.exists(configFile)) {

            log.error("dir {} no find config.properties", dir);
            return;
        }

        int port = 6881;
        byte[] transactionId = new byte[5];
        new Random().nextBytes(transactionId);
        int minNodes = 100;
        int maxNodes = 5000;
        int timeout = 600000;
        int findNodeInterval = 60;
        int pingInterval = 300;
        int removeNodeInterval = 300;

        InputStream inputStream = Files.newInputStream(configFile);
        Properties properties = new Properties();
        properties.load(inputStream);
        String host = properties.getProperty("server.ip");
        int corePoolSize = Integer.parseInt(properties.getProperty("server.peers.core.pool.size"));
        int maximumPoolSize = Integer.parseInt(properties.getProperty("server.peers.maximum.pool.size"));
        inputStream.close();

        Bootstrap bootstrap = new Bootstrap();
        byte[] nodeId = Utils.nodeId();
        Map<String, NodeTable> table = new ConcurrentHashMap<>(6);
        table.put(new String(nodeId), new NodeTable(Utils.bytesToHex(nodeId), host, port, System.currentTimeMillis()));
        ThreadFactory threadFactory = Executors.defaultThreadFactory();
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(corePoolSize, maximumPoolSize,
                0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(), threadFactory);
        bootstrap.group(new NioEventLoopGroup())
                .channel(NioDatagramChannel.class)
                .option(ChannelOption.SO_BROADCAST, true)
                .handler(new DiscardServerHandler(table, nodeId, maxNodes, threadPoolExecutor));
        final Channel channel = bootstrap.bind(port).sync().channel();

        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(5);
        log.info("start autoFindNode");
        scheduledExecutorService.scheduleWithFixedDelay(new FindNode(channel, transactionId, nodeId, table, minNodes), findNodeInterval, findNodeInterval, TimeUnit.SECONDS);
        log.info("start ok autoFindNode");
        log.info("start Ping");
        scheduledExecutorService.scheduleWithFixedDelay(new Ping(channel, transactionId, nodeId, table), pingInterval, pingInterval, TimeUnit.SECONDS);
        log.info("start ok Ping");
        log.info("start RemoveNode");
        scheduledExecutorService.scheduleWithFixedDelay(new RemoveNode(table, timeout), removeNodeInterval, removeNodeInterval, TimeUnit.SECONDS);
        log.info("start ok RemoveNode");
        log.info("server ok");
    }
}
