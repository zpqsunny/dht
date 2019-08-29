package me.zpq.dht;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.socket.DatagramPacket;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.TimerTask;

/**
 * @author zpq
 * @date 2019-08-29
 */
public class Ping extends TimerTask {

    private Channel channel;

    private Map<String, NodeTable> table;

    private String transactionId;

    private byte[] nodeId;

    private DHTProtocol dhtProtocol = new DHTProtocol();

    public Ping(Channel channel, String transactionId, byte[] nodeId, Map<String, NodeTable> table) {

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
