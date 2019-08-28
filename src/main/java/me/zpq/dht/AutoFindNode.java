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

public class AutoFindNode extends TimerTask {

    private Channel channel;

    private byte[] nodeId;

    private Map<byte[], NodeTable> tableMap;

    private Integer minNodes;

    private Integer maxNodes;

    private List<BootstrapAddress> list = new ArrayList<>();

    private DHTProtocol dhtProtocol = new DHTProtocol();

    public AutoFindNode(Channel channel, byte[] nodeId, Map<byte[], NodeTable> tableMap, Integer minNodes, Integer maxNodes) {
        this.channel = channel;
        this.nodeId = nodeId;
        this.tableMap = tableMap;
        this.minNodes = minNodes;
        this.maxNodes = maxNodes;
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

            final byte[] pingQuery = dhtProtocol.findNodeQuery("zpq", nodeId, Helper.nodeId());
            list.forEach(bootstrapAddress -> {

                channel.writeAndFlush(new DatagramPacket(Unpooled.copiedBuffer(pingQuery),
                        new InetSocketAddress(bootstrapAddress.getHost(), bootstrapAddress.getPort())));
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
