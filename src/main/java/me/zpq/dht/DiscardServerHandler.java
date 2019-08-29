package me.zpq.dht;

import be.adaxisoft.bencode.BDecoder;
import be.adaxisoft.bencode.BEncodedValue;
import be.adaxisoft.bencode.InvalidBEncodingException;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author zpq
 * @date 2019-08-21
 */
public class DiscardServerHandler extends SimpleChannelInboundHandler<DatagramPacket> {

    private DHTProtocol dhtProtocol = new DHTProtocol();

    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    private byte[] nodeId;

    private Map<byte[], NodeTable> nodeTable;

    private Integer maxNodes;

    private Jedis jedis;

    public DiscardServerHandler(Map<byte[], NodeTable> nodeTable, byte[] nodeId, Integer maxNodes, Jedis jedis) {

        this.nodeId = nodeId;
        this.nodeTable = nodeTable;
        this.maxNodes = maxNodes;
        this.jedis = jedis;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, DatagramPacket datagramPacket) throws Exception {

        try {

            ByteBuf content = datagramPacket.copy().content();
            byte[] req = new byte[content.readableBytes()];
            content.readBytes(req);

            BEncodedValue data = BDecoder.decode(new ByteArrayInputStream(req));

            String address = datagramPacket.sender().getAddress().getHostAddress();

            int port = datagramPacket.sender().getPort();

            LOGGER.info("from {} port {}", address, port);

            String transactionId = data.getMap().get("t").getString();

            switch (data.getMap().get("y").getString()) {

                case "q":

                    Map<String, BEncodedValue> a = data.getMap().get("a").getMap();
                    switch (data.getMap().get("q").getString()) {

                        case "ping":
                            LOGGER.info(" q : ping");
                            this.queryPing(channelHandlerContext, datagramPacket, transactionId);
                            break;
                        case "find_node":
                            LOGGER.info(" q : find_node");
                            this.queryFindNode(channelHandlerContext, datagramPacket, transactionId);
                            break;
                        case "get_peers":
                            LOGGER.info(" q : get_peers");
                            this.queryGetPeers(channelHandlerContext, datagramPacket, transactionId, a);
                            break;
                        case "announce_peer":
                            LOGGER.info(" q : announce_peer");
                            this.queryAnnouncePeer(channelHandlerContext, datagramPacket, transactionId, a);
                            break;
                        case "sample_infohashes":
                            LOGGER.info(" q : sample_infohashes");
                            break;
                        case "vote":
                            LOGGER.info(" q : vote");
                            break;
                        default:
                            this.queryMethodUnknown(channelHandlerContext, datagramPacket, transactionId);
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
                    if (r.get("values") != null) {

                        this.responseHasValues(r);
                    }

                    break;

                case "e":

                    this.responseError(data);

                    break;
                default:
                    break;
            }


        } catch (Exception e) {

            e.printStackTrace();
        }

    }

    private void queryPing(ChannelHandlerContext channelHandlerContext, DatagramPacket datagramPacket, String transactionId) throws IOException {

        channelHandlerContext.writeAndFlush(new DatagramPacket(
                Unpooled.copiedBuffer(dhtProtocol.pingResponse(transactionId, nodeId)),
                datagramPacket.sender()));
    }

    private void queryFindNode(ChannelHandlerContext channelHandlerContext, DatagramPacket datagramPacket, String transactionId) throws IOException {

        List<NodeTable> table = new ArrayList<>(nodeTable.values());
        channelHandlerContext.writeAndFlush(new DatagramPacket(
                Unpooled.copiedBuffer(
                        dhtProtocol.findNodeResponse(transactionId, nodeId, Helper.nodesEncode(table))),
                datagramPacket.sender()));
    }

    private void queryGetPeers(ChannelHandlerContext channelHandlerContext, DatagramPacket datagramPacket, String transactionId, Map<String, BEncodedValue> a) throws IOException {

        String token = String.valueOf(a.get("info_hash")).substring(0, 2);
        List<NodeTable> nodes = new ArrayList<>(nodeTable.values());
        channelHandlerContext.writeAndFlush(new DatagramPacket(
                Unpooled.copiedBuffer(
                        dhtProtocol.getPeersResponseNodes(transactionId, nodeId, token, Helper.nodesEncode(nodes))),
                datagramPacket.sender()));

    }

    private void queryAnnouncePeer(ChannelHandlerContext channelHandlerContext, DatagramPacket datagramPacket, String transactionId, Map<String, BEncodedValue> a) throws IOException {

        String address = datagramPacket.sender().getAddress().getHostAddress();

        int port = datagramPacket.sender().getPort();
        LOGGER.info("implied_port: {} , info_hash: {} , host: {} , p: {} ,  port: {}",
                a.get("implied_port").getInt(), a.get("info_hash").getString(), address, port, a.get("port").getInt());
        MetaInfoRequest metaInfoRequest;
        if (a.get("implied_port") != null && a.get("implied_port").getInt() != 0) {

            metaInfoRequest = new MetaInfoRequest(address, port, a.get("info_hash").getBytes());
        } else {

            metaInfoRequest = new MetaInfoRequest(address, a.get("port").getInt(), a.get("info_hash").getBytes());
        }

        jedis.lpush("meta_info", metaInfoRequest.toString());

        channelHandlerContext.writeAndFlush(new DatagramPacket(
                Unpooled.copiedBuffer(
                        dhtProtocol.announcePeerResponse(transactionId, nodeId)),
                datagramPacket.sender()));
    }

    private void queryMethodUnknown(ChannelHandlerContext channelHandlerContext, DatagramPacket datagramPacket, String transactionId) throws IOException {

        channelHandlerContext.writeAndFlush(new DatagramPacket(
                Unpooled.copiedBuffer(
                        dhtProtocol.error(transactionId, 204, "Method Unknown")),
                datagramPacket.sender()));
    }

    private void responseHasId(Map<String, BEncodedValue> r, DatagramPacket datagramPacket) throws InvalidBEncodingException {

        byte[] id = r.get("id").getBytes();

        if (this.nodeTable.containsKey(id)) {

            String address = datagramPacket.sender().getAddress().getHostAddress();

            int port = datagramPacket.sender().getPort();

            this.nodeTable.put(id, new NodeTable(id, address, port, System.currentTimeMillis()));
        }
    }

    private void responseHasNodes(Map<String, BEncodedValue> r) throws InvalidBEncodingException {

        LOGGER.info("has nodes");

        byte[] nodes = r.get("nodes").getBytes();

        List<NodeTable> nodeTableList = Helper.nodesDecode(nodes);

        if (nodeTable.size() > maxNodes) {

            return;
        }
        nodeTableList.forEach(nodeTable -> {

            this.nodeTable.put(nodeTable.getNid(), new NodeTable(nodeTable.getNid(), nodeTable.getIp(),
                    nodeTable.getPort(), System.currentTimeMillis()));
        });

    }

    private void responseHasValues(Map<String, BEncodedValue> r) throws InvalidBEncodingException {

        LOGGER.info("has values");
        LOGGER.info("{}", r.get("values").getString());
    }

    private void responseError(BEncodedValue data) throws InvalidBEncodingException {

        List<BEncodedValue> e = data.getMap().get("e").getList();

        LOGGER.error(" r : error Code: {} , Description: {}", e.get(0).getInt(), e.get(1).getString());

    }
}
