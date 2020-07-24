package me.zpq.server.schedule;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.socket.DatagramPacket;
import me.zpq.krpc.KrpcProtocol;
import me.zpq.route.IRoutingTable;

import java.io.IOException;
import java.net.InetSocketAddress;

/**
 * @author zpq
 * @date 2019-08-29
 */
public class Ping implements Runnable {

    private final Channel channel;

    private final IRoutingTable routingTable;

    private final byte[] transactionId;

    private final byte[] nodeId;

    public Ping(Channel channel, byte[] transactionId, byte[] nodeId, IRoutingTable routingTable) {

        this.channel = channel;
        this.routingTable = routingTable;
        this.transactionId = transactionId;
        this.nodeId = nodeId;
    }

    @Override
    public void run() {

        routingTable.values().forEach(nodeTable -> {

            try {
                channel.writeAndFlush(new DatagramPacket(Unpooled.copiedBuffer(KrpcProtocol.pingQuery(transactionId, nodeId)),
                        new InetSocketAddress(nodeTable.getIp(), nodeTable.getPort())));
            } catch (IOException e) {
                // ignore
            }
        });
    }
}
