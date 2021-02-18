package me.zpq.server.schedule;

import io.netty.channel.Channel;
import me.zpq.krpc.KrpcProtocol;
import me.zpq.route.IRoutingTable;
import me.zpq.server.DHTResponse;

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
                channel.writeAndFlush(DHTResponse.builder()
                        .data(KrpcProtocol.pingQuery(transactionId, nodeId))
                        .sender(new InetSocketAddress(nodeTable.getIp(), nodeTable.getPort()))
                        .build());

            } catch (IOException e) {
                // ignore
            }
        });
    }
}
