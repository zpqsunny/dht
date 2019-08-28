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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.*;

/**
 * @author zpq
 * @date 2019-08-28
 */
public class Main {

    private static InternalLogger LOGGER = Log4JLoggerFactory.getInstance(Main.class);

    public static void main(String[] args) throws InterruptedException {

        List<BootstrapAddress> list = new ArrayList<>();
        list.add(new BootstrapAddress("10.1.1.2", 6881));
//        list.add(new BootstrapAddress("router.bittorrent.com", 6881));
//        list.add(new BootstrapAddress("router.utorrent.com", 6881));
//        list.add(new BootstrapAddress("dht.transmissionbt.com", 6881));
//
        Bootstrap bootstrap = new Bootstrap();
        byte[] nodeId = Helper.nodeId();
        String peerId = "-WW0001-123456789012";
        Map<byte[], NodeTable> table = new Hashtable<>();
        table.put(nodeId, new NodeTable(nodeId, "10.1.1.1", 1, 1L));
        bootstrap.group(new NioEventLoopGroup())
                .channel(NioDatagramChannel.class)
                .option(ChannelOption.SO_BROADCAST, true)
                .handler(new DiscardServerHandler(table, nodeId, peerId));
//        final Channel channel = bootstrap.bind("202.81.242.169", 6882).sync().channel();
        final Channel channel = bootstrap.bind("10.1.1.1", 10000).sync().channel();

        LOGGER.info("server ok");

        LOGGER.info("start timeTask");

//        Timer timer = new Timer();
//        timer.schedule(new TimerTask() {
//            @Override
//            public void run() {
//                DHTProtocol dhtProtocol = new DHTProtocol();
//                try {
//
//                    final byte[] pingQuery = dhtProtocol.findNodeQuery("zpq", nodeId, Helper.nodeId());
//                    list.forEach(bootstrapAddress -> {
//
//                        channel.writeAndFlush(new DatagramPacket(Unpooled.copiedBuffer(pingQuery),
//                                new InetSocketAddress(bootstrapAddress.getHost(), bootstrapAddress.getPort())));
//                    });
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//        }, 2000,2000);
        LOGGER.info("end timeTask");
    }
}
