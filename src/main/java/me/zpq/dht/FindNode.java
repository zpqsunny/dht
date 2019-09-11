package me.zpq.dht;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.socket.DatagramPacket;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TimerTask;

public class FindNode extends TimerTask {

    private Channel channel;

    private byte[] transactionId;

    private byte[] nodeId;

    private Map<String, NodeTable> tableMap;

    private Integer minNodes;

    private List<BootstrapAddress> list = new ArrayList<>();

    private DHTProtocol dhtProtocol = new DHTProtocol();

    public FindNode(Channel channel, byte[] transactionId, byte[] nodeId, Map<String, NodeTable> tableMap, Integer minNodes) {
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

        if (tableMap.size() >= minNodes) {

            return;
        }

        try {

            final byte[] findNodeQuery = dhtProtocol.findNodeQuery(transactionId, nodeId, Helper.nodeId());
            list.forEach(bootstrapAddress -> {

                channel.writeAndFlush(new DatagramPacket(Unpooled.copiedBuffer(findNodeQuery),
                        new InetSocketAddress(bootstrapAddress.getHost(), bootstrapAddress.getPort())));
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
