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

    private Map<byte[], NodeTable> table;

    private String transactionId;

    private DHTProtocol dhtProtocol = new DHTProtocol();

    public Ping(Channel channel, String transactionId, Map<byte[], NodeTable> table) {

        this.channel = channel;
        this.table = table;
        this.transactionId = transactionId;
    }

    @Override
    public void run() {

        table.values().forEach(nodeTable -> {

            try {
                channel.writeAndFlush(new DatagramPacket(Unpooled.copiedBuffer(dhtProtocol.pingQuery(transactionId, nodeTable.getNid())),
                        new InetSocketAddress(nodeTable.getIp(), nodeTable.getPort())));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}
