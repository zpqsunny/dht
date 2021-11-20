package me.zpq.server.schedule;

import be.adaxisoft.bencode.BEncodedValue;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;
import me.zpq.dht.common.Utils;
import me.zpq.krpc.KrpcProtocol;
import me.zpq.route.IRoutingTable;
import me.zpq.server.BootstrapAddress;
import me.zpq.server.DHTResponse;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Slf4j
public class FindNode implements Runnable {

    private final Channel channel;

    private final byte[] transactionId;

    private final byte[] nodeId;

    private final IRoutingTable routingTable;

    private final int minNodes;

    private final List<BootstrapAddress> list = Arrays.asList(
            new BootstrapAddress("router.bittorrent.com", 6881),
            new BootstrapAddress("router.utorrent.com", 6881),
            new BootstrapAddress("dht.transmissionbt.com", 6881),
            new BootstrapAddress("dht.libtorrent.org", 25401)
    );

    public FindNode(final Channel channel, byte[] transactionId, byte[] nodeId, IRoutingTable routingTable, int minNodes) {

        this.channel = channel;
        this.transactionId = transactionId;
        this.nodeId = nodeId;
        this.routingTable = routingTable;
        this.minNodes = minNodes;
    }

    @Override
    public void run() {

        try {

            Map<String, BEncodedValue> findNodeQuery;
            int size = routingTable.size();
            log.info("tableMap size: {} ", size);
            if (size < minNodes) {

                log.info("do find Node in BootstrapAddress, minNodes: {} ", minNodes);
                findNodeQuery = KrpcProtocol.findNodeQuery(transactionId, nodeId, Utils.nodeId());
                list.forEach(bootstrapAddress -> channel.writeAndFlush(
                        DHTResponse.builder()
                                .data(findNodeQuery)
                                .sender(new InetSocketAddress(bootstrapAddress.getHost(), bootstrapAddress.getPort()))
                                .build())
                );
            } else {

                log.info("do find Node in NodeTable");
                findNodeQuery = KrpcProtocol.findNodeQuery(transactionId, nodeId, Utils.nearNodeId(nodeId));
                routingTable.values().forEach(nodeTable -> channel.writeAndFlush(
                        DHTResponse.builder()
                                .data(findNodeQuery)
                                .sender(new InetSocketAddress(nodeTable.getIp(), nodeTable.getPort()))
                                .build())
                );
            }

        } catch (IOException e) {

            // ignore
        }
    }
}
