package me.zpq.dht.server;

import be.adaxisoft.bencode.BDecoder;
import be.adaxisoft.bencode.BEncodedValue;
import be.adaxisoft.bencode.InvalidBEncodingException;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import me.zpq.dht.protocol.DhtProtocol;
import me.zpq.dht.model.MetaInfoRequest;
import me.zpq.dht.model.NodeTable;
import me.zpq.dht.util.Helper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * @author zpq
 * @date 2019-08-21
 */
public class DiscardServerHandler extends SimpleChannelInboundHandler<DatagramPacket> {

    private DhtProtocol dhtProtocol = new DhtProtocol();

    private static final Logger LOGGER = LoggerFactory.getLogger(DiscardServerHandler.class);

    private byte[] nodeId;

    private Map<String, NodeTable> nodeTable;

    private Integer maxNodes;

    private Jedis jedis;

    public DiscardServerHandler(Map<String, NodeTable> nodeTable, byte[] nodeId, Integer maxNodes, Jedis jedis) {

        this.nodeId = nodeId;
        this.nodeTable = nodeTable;
        this.maxNodes = maxNodes;
        this.jedis = jedis;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, DatagramPacket datagramPacket) {

        try {

            ByteBuf content = datagramPacket.copy().content();
            byte[] req = new byte[content.readableBytes()];
            content.readBytes(req);
            content.release();

            BEncodedValue data = BDecoder.decode(new ByteArrayInputStream(req));
            byte[] transactionId = data.getMap().get("t").getBytes();

            switch (data.getMap().get("y").getString()) {

                case "q":

                    Map<String, BEncodedValue> a = data.getMap().get("a").getMap();
                    switch (data.getMap().get("q").getString()) {

                        case "ping":
                            this.queryPing(channelHandlerContext, datagramPacket, transactionId, a);
                            break;
                        case "find_node":
                            this.queryFindNode(channelHandlerContext, datagramPacket, transactionId);
                            break;
                        case "get_peers":
                            this.queryGetPeers(channelHandlerContext, datagramPacket, transactionId, a);
                            break;
                        case "announce_peer":
                            this.queryAnnouncePeer(channelHandlerContext, datagramPacket, transactionId, a);
                            break;
                        default:
//                            this.queryMethodUnknown(channelHandlerContext, datagramPacket, transactionId);
                            break;
                    }

                    break;
                case "r":

                    Map<String, BEncodedValue> r = data.getMap().get("r").getMap();
                    if (r.get("a") != null) {

                        this.responseHasId(r, datagramPacket);
                    }

                    if (r.get("nodes") != null) {

                        this.responseHasNodes(r);
                    }

                    break;

                case "e":

                    this.responseError(data);

                    break;
                default:
                    break;
            }


        } catch (Exception e) {

            // nothing to do
        }

    }

    private void queryPing(ChannelHandlerContext channelHandlerContext, DatagramPacket datagramPacket, byte[] transactionId, Map<String, BEncodedValue> a) throws IOException {

        channelHandlerContext.writeAndFlush(new DatagramPacket(
                Unpooled.copiedBuffer(dhtProtocol.pingResponse(transactionId, nodeId)),
                datagramPacket.sender()));
        String id = a.get("id").getString();
        if (nodeTable.containsKey(id)) {

            NodeTable nodeTable = this.nodeTable.get(id);
            nodeTable.setTime(System.currentTimeMillis());
            this.nodeTable.put(id, nodeTable);
        }
    }

    private void queryFindNode(ChannelHandlerContext channelHandlerContext, DatagramPacket datagramPacket, byte[] transactionId) throws IOException {

        List<NodeTable> table = new ArrayList<>(nodeTable.values());
        channelHandlerContext.writeAndFlush(new DatagramPacket(
                Unpooled.copiedBuffer(
                        dhtProtocol.findNodeResponse(transactionId, nodeId, Helper.nodesEncode(table))),
                datagramPacket.sender()));
    }

    private void queryGetPeers(ChannelHandlerContext channelHandlerContext, DatagramPacket datagramPacket, byte[] transactionId, Map<String, BEncodedValue> a) throws IOException {

        byte[] infoHash = a.get("info_hash").getBytes();
        byte[] token = Arrays.copyOfRange(infoHash, 0, 5);
//        List<NodeTable> nodes = new ArrayList<>(nodeTable.values());
        channelHandlerContext.writeAndFlush(new DatagramPacket(
                Unpooled.copiedBuffer(
                        dhtProtocol.getPeersResponseNodes(transactionId, nodeId, token, new byte[0])),
                datagramPacket.sender()));

    }

    private void queryAnnouncePeer(ChannelHandlerContext channelHandlerContext, DatagramPacket datagramPacket, byte[] transactionId, Map<String, BEncodedValue> a) throws IOException {

        String address = datagramPacket.sender().getAddress().getHostAddress();

        int port = datagramPacket.sender().getPort();

        MetaInfoRequest metaInfoRequest;

        if (a.get("implied_port") != null && a.get("implied_port").getInt() != 0) {

            LOGGER.info("implied_port: {} , info_hash: {} , host: {} , p: {} ,  port: {}",
                    a.get("implied_port").getInt(), Helper.bytesToHex(a.get("info_hash").getBytes()), address, port, a.get("port").getInt());

            metaInfoRequest = new MetaInfoRequest(address, port, a.get("info_hash").getBytes());

        } else {

            LOGGER.info("implied_port: {} , info_hash: {} , host: {} , p: {} ,  port: {}",
                    null, Helper.bytesToHex(a.get("info_hash").getBytes()), address, port, a.get("port").getInt());

            metaInfoRequest = new MetaInfoRequest(address, a.get("port").getInt(), a.get("info_hash").getBytes());
        }

        jedis.lpush("meta_info", metaInfoRequest.toString());

        channelHandlerContext.writeAndFlush(new DatagramPacket(
                Unpooled.copiedBuffer(
                        dhtProtocol.announcePeerResponse(transactionId, nodeId)),
                datagramPacket.sender()));
    }

    private void queryMethodUnknown(ChannelHandlerContext channelHandlerContext, DatagramPacket datagramPacket, byte[] transactionId) throws IOException {

        channelHandlerContext.writeAndFlush(new DatagramPacket(
                Unpooled.copiedBuffer(
                        dhtProtocol.error(transactionId, 204, "Method Unknown")),
                datagramPacket.sender()));
    }

    private void responseHasId(Map<String, BEncodedValue> r, DatagramPacket datagramPacket) throws InvalidBEncodingException {

        String id = r.get("id").getString();

        if (this.nodeTable.containsKey(id)) {

            String address = datagramPacket.sender().getAddress().getHostAddress();

            int port = datagramPacket.sender().getPort();

            this.nodeTable.put(id, new NodeTable(Helper.bytesToHex(r.get("id").getBytes()), address, port, System.currentTimeMillis()));
        }
    }

    private void responseHasNodes(Map<String, BEncodedValue> r) throws InvalidBEncodingException {

        byte[] nodes = r.get("nodes").getBytes();

        List<NodeTable> nodeTableList = Helper.nodesDecode(nodes);

        if (nodeTable.size() >= maxNodes) {

            return;
        }
        nodeTableList.forEach(nodeTable -> {

            this.nodeTable.put(nodeTable.getNid(), new NodeTable(nodeTable.getNid(), nodeTable.getIp(),
                    nodeTable.getPort(), System.currentTimeMillis()));
        });

    }

    private void responseError(BEncodedValue data) throws InvalidBEncodingException {

        List<BEncodedValue> e = data.getMap().get("e").getList();

        LOGGER.error(" r : error Code: {} , Description: {}", e.get(0).getInt(), e.get(1).getString());

    }
}
