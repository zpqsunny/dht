package me.zpq.dht;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.Log4JLoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * @author zpq
 * @date 2019-08-28
 */
public class Main {

    private static InternalLogger LOGGER = Log4JLoggerFactory.getInstance(Main.class);

    public static void main(String[] args) throws InterruptedException, IOException {

        ClassLoader classLoader = Main.class.getClassLoader();
        URL url = classLoader.getResource("config.yaml");
        if (url == null) {

            LOGGER.error("error config");
            return;
        }
        InputStream config = Files.newInputStream(Paths.get(url.getFile()));
        Yaml yaml = new Yaml();
        Map configMap = yaml.load(config);
        String host = (String) configMap.get("serverIp");
        Integer port = (Integer) configMap.get("serverPort");
        String peerId = (String) configMap.get("peerId");
        String transactionID = (String) configMap.get("transactionID");
        Integer minNodes = (Integer) configMap.get("minNodes");
        Integer maxNodes = (Integer) configMap.get("maxNodes");

        Bootstrap bootstrap = new Bootstrap();
        byte[] nodeId = Helper.nodeId();
        Map<byte[], NodeTable> table = new Hashtable<>();
        table.put(nodeId, new NodeTable(nodeId, host, port, System.currentTimeMillis()));
        bootstrap.group(new NioEventLoopGroup())
                .channel(NioDatagramChannel.class)
                .option(ChannelOption.SO_BROADCAST, true)
                .handler(new DiscardServerHandler(table, nodeId, peerId, maxNodes));
//        final Channel channel = bootstrap.bind("202.81.242.169", 6882).sync().channel();
        final Channel channel = bootstrap.bind(host, port).sync().channel();

        LOGGER.info("server ok");
        LOGGER.info("start autoFindNode");
        Timer autoFindNode = new Timer();
        autoFindNode.schedule(new AutoFindNode(channel, nodeId, table, minNodes), 2000, 2000);
        LOGGER.info("start ok autoFindNode");
    }

}
