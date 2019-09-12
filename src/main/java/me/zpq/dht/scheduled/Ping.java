package me.zpq.dht.scheduled;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.socket.DatagramPacket;
import me.zpq.dht.protocol.DhtProtocol;
import me.zpq.dht.model.NodeTable;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Map;

/**
 * @author zpq
 * @date 2019-08-29
 */
public class Ping implements Runnable {

    private Channel channel;

    private Map<String, NodeTable> table;

    private byte[] transactionId;

    private byte[] nodeId;

    private DhtProtocol dhtProtocol = new DhtProtocol();

    public Ping(Channel channel, byte[] transactionId, byte[] nodeId, Map<String, NodeTable> table) {

        this.channel = channel;
        this.table = table;
        this.transactionId = transactionId;
        this.nodeId = nodeId;
    }

    @Override
    public void run() {

        table.values().forEach(nodeTable -> {

            try {
                channel.writeAndFlush(new DatagramPacket(Unpooled.copiedBuffer(dhtProtocol.pingQuery(transactionId, nodeId)),
                        new InetSocketAddress(nodeTable.getIp(), nodeTable.getPort())));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}
