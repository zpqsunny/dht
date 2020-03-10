package me.zpq.dht.scheduled;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.socket.DatagramPacket;
import me.zpq.dht.model.BootstrapAddress;
import me.zpq.dht.protocol.DhtProtocol;
import me.zpq.dht.util.Utils;
import me.zpq.dht.model.NodeTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FindNode implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(FindNode.class);

    private Channel channel;

    private byte[] transactionId;

    private byte[] nodeId;

    private Map<String, NodeTable> tableMap;

    private int minNodes;

    private List<BootstrapAddress> list = new ArrayList<>();

    public FindNode(Channel channel, byte[] transactionId, byte[] nodeId, Map<String, NodeTable> tableMap, int minNodes) {
        this.channel = channel;
        this.transactionId = transactionId;
        this.nodeId = nodeId;
        this.tableMap = tableMap;
        this.minNodes = minNodes;
        list.add(new BootstrapAddress("router.bittorrent.com", 6881));
        list.add(new BootstrapAddress("router.utorrent.com", 6881));
        list.add(new BootstrapAddress("dht.transmissionbt.com", 6881));
    }

    @Override
    public void run() {

        try {

            byte[] findNodeQuery = DhtProtocol.findNodeQuery(transactionId, nodeId, Utils.nodeId());
            int size = tableMap.size();
            log.info("tableMap size: {} ", size);
            if (size < minNodes) {

                log.info("do find Node in BootstrapAddress, minNodes: {} ", minNodes);
                list.forEach(bootstrapAddress -> channel.writeAndFlush(
                        new DatagramPacket(Unpooled.copiedBuffer(findNodeQuery),
                                new InetSocketAddress(bootstrapAddress.getHost(), bootstrapAddress.getPort())
                        )));
            } else {

                log.info("do find Node in NodeTable");
                List<NodeTable> nodeTables = new ArrayList<>(tableMap.values());
                nodeTables.forEach(nodeTable -> channel.writeAndFlush(
                        new DatagramPacket(Unpooled.copiedBuffer(findNodeQuery),
                                new InetSocketAddress(nodeTable.getIp(), nodeTable.getPort())
                        )));
            }

        } catch (IOException e) {

            log.error(e.getMessage(), e);
        }
    }
}
