package me.zpq.server;

import be.adaxisoft.bencode.BEncodedValue;
import be.adaxisoft.bencode.InvalidBEncodingException;
import io.lettuce.core.api.sync.RedisCommands;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import lombok.extern.slf4j.Slf4j;
import me.zpq.dht.common.Utils;
import me.zpq.krpc.KrpcConstant;
import me.zpq.krpc.KrpcProtocol;
import me.zpq.route.IRoutingTable;
import me.zpq.route.NodeTable;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

import java.io.IOException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author zpq
 * @date 2019-08-21
 */
@Slf4j
public class DHTServerHandler extends SimpleChannelInboundHandler<DHTData> {

    private final byte[] nodeId;

    private final IRoutingTable routingTable;

    private final int maxNodes;

    private final RedisCommands<String, String> redis;

    private final static String SET_KEY = "announce";

    private final static String LIST_KEY = "peer";

    private final static String FRESH_EKY = "fresh";

    public DHTServerHandler(IRoutingTable routingTable, byte[] nodeId, int maxNodes, RedisCommands<String, String> redis) {

        this.nodeId = nodeId;
        this.routingTable = routingTable;
        this.maxNodes = maxNodes;
        this.redis = redis;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, DHTData data) {

        try {

            BEncodedValue value = data.getData();
            byte[] transactionId = value.getMap().get(KrpcConstant.T).getBytes();

            switch (value.getMap().get(KrpcConstant.Y).getString()) {

                case KrpcConstant.Q:

                    Map<String, BEncodedValue> a = value.getMap().get(KrpcConstant.A).getMap();
                    switch (value.getMap().get(KrpcConstant.Q).getString()) {

                        case KrpcConstant.PING:
                            this.queryPing(ctx, data, transactionId, a);
                            break;
                        case KrpcConstant.FIND_NODE:
                            this.queryFindNode(ctx, data, transactionId);
                            break;
                        case KrpcConstant.GET_PEERS:
                            this.queryGetPeers(ctx, data, transactionId, a);
                            break;
                        case KrpcConstant.ANNOUNCE_PEER:
                            this.queryAnnouncePeer(ctx, data, transactionId, a);
                            break;
                        default:
                            this.queryMethodUnknown(ctx, data, transactionId);
                            break;
                    }

                    break;
                case KrpcConstant.R:

                    Map<String, BEncodedValue> r = value.getMap().get(KrpcConstant.R).getMap();
                    if (r.get(KrpcConstant.A) != null) {

                        this.responseHasId(r, data);
                    }

                    if (r.get(KrpcConstant.NODES) != null) {

                        this.responseHasNodes(r);
                    }

                    break;

                case KrpcConstant.E:

                    this.responseError(data);

                    break;
                default:
                    break;
            }

        } catch (Exception e) {

            // nothing to do
        }

    }

    private void queryPing(ChannelHandlerContext ctx, DHTData value, byte[] transactionId, Map<String, BEncodedValue> a) throws IOException {

        ctx.writeAndFlush(new DatagramPacket(Unpooled.copiedBuffer(
                KrpcProtocol.pingResponse(transactionId, nodeId)), value.getSender()));

        byte[] id = a.get(KrpcConstant.ID).getBytes();
        String ip = value.getSender().getAddress().getHostAddress();
        int port = value.getSender().getPort();
        this.updateRoutingTable(id, ip, port);
    }

    private void queryFindNode(ChannelHandlerContext ctx, DHTData value, byte[] transactionId) throws IOException {

        Collection<NodeTable> table = routingTable.values();
        List<NodeTable> t = table.stream().sorted(Comparator.comparingLong(NodeTable::getLastChanged).reversed())
                .limit(10)
                .collect(Collectors.toList());
        ctx.writeAndFlush(new DatagramPacket(
                Unpooled.copiedBuffer(KrpcProtocol.findNodeResponse(transactionId, nodeId, this.nodesEncode(t))),
                value.getSender()));
    }

    private void queryGetPeers(ChannelHandlerContext ctx, DHTData value, byte[] transactionId, Map<String, BEncodedValue> a) throws IOException {

        byte[] token = this.getToken(a.get(KrpcConstant.INFO_HASH).getBytes());
//        List<NodeTable> nodes = new ArrayList<>(nodeTable.values());
        ctx.writeAndFlush(new DatagramPacket(
                Unpooled.copiedBuffer(
                        KrpcProtocol.getPeersResponseNodes(transactionId, nodeId, token, new byte[0])),
                value.getSender()));

    }

    private void queryAnnouncePeer(ChannelHandlerContext ctx, DHTData value, byte[] transactionId, Map<String, BEncodedValue> a) throws IOException {

        // sha1
        byte[] infoHash = a.get(KrpcConstant.INFO_HASH).getBytes();

        if (infoHash.length != 20) {

            return;
        }
        // token
//        byte[] needValidatorToken = a.get(KrpcConstant.TOKEN).getBytes();
//
//        if (!this.validatorToken(infoHash, needValidatorToken)) {
//
//            return;
//        }
        // ip
        String ip = value.getSender().getAddress().getHostAddress();

        // port
        int port = value.getSender().getPort();

        int peerPort;

        if (a.get(KrpcConstant.IMPLIED_PORT) != null && a.get(KrpcConstant.IMPLIED_PORT).getInt() != 0) {

            peerPort = port;

        } else {

            peerPort = a.get(KrpcConstant.PORT).getInt();
        }

        ctx.writeAndFlush(new DatagramPacket(Unpooled.copiedBuffer(
                KrpcProtocol.announcePeerResponse(transactionId, nodeId)), value.getSender()));

        String hash = Hex.encodeHexString(infoHash);

        log.info("ip {} port {} infoHash {}", ip, peerPort, hash);
        // format hash|ip|port|timestamp
//        redis.lpush(FRESH_EKY, String.join("|", hash, ip, Integer.toString(port), Long.toString(System.currentTimeMillis())));

        if (!redis.sismember(SET_KEY, hash)) {

            redis.sadd(SET_KEY, hash);
            // format hash|ip|port
            redis.lpush(LIST_KEY, String.join("|", hash, ip, Integer.toString(peerPort)));
        }

    }

    private void queryMethodUnknown(ChannelHandlerContext ctx, DHTData value, byte[] transactionId) throws IOException {

        ctx.writeAndFlush(new DatagramPacket(
                Unpooled.copiedBuffer(
                        KrpcProtocol.error(transactionId, 204, "Method Unknown")),
                value.getSender()));
    }

    private void responseHasId(Map<String, BEncodedValue> r, DHTData value) throws InvalidBEncodingException {

        byte[] id = r.get(KrpcConstant.ID).getBytes();
        String ip = value.getSender().getAddress().getHostAddress();
        int port = value.getSender().getPort();
        this.updateRoutingTable(id, ip, port);
    }

    private void responseHasNodes(Map<String, BEncodedValue> r) throws InvalidBEncodingException {

        byte[] nodes = r.get(KrpcConstant.NODES).getBytes();

        List<NodeTable> list = this.nodesDecode(nodes);

        if (routingTable.size() >= maxNodes) {

            return;
        }

        list.forEach(table -> routingTable.put(NodeTable
                        .builder()
                        .id(table.getId())
                        .ip(table.getIp())
                        .port(table.getPort())
                        .lastChanged(System.currentTimeMillis())
                        .build())
        );
    }

    private void responseError(DHTData value) throws InvalidBEncodingException {

        List<BEncodedValue> e = value.getData().getMap().get(KrpcConstant.E).getList();

        log.error("from: {} r : error Code: {} , Description: {}", value.getSender().getAddress().getHostAddress(), e.get(0).getInt(), e.get(1).getString());

    }

    private void updateRoutingTable(byte[] id, String ip, int port) {

        String nId = Hex.encodeHexString(id);

        NodeTable nodeTable = NodeTable.builder()
                .id(nId)
                .ip(ip)
                .port(port)
                .lastChanged(System.currentTimeMillis())
                .build();
        if (routingTable.has(nId)) {

            // exists
            routingTable.replace(nodeTable);
            return;
        }

        if (routingTable.size() < maxNodes) {

            routingTable.put(nodeTable);
        }

        // ignore
    }

    private byte[] nodesEncode(Collection<NodeTable> collection) {

        ByteBuffer byteBuffer = ByteBuffer.allocate(collection.size() * 26);

        collection.forEach(nodeTable -> {

            try {
                byteBuffer.put(Hex.decodeHex(nodeTable.getId()))
                        .put(Utils.ipToByte(nodeTable.getIp()))
                        .put(Utils.intToBytes2(nodeTable.getPort()));
            } catch (UnknownHostException | DecoderException e) {
                // ignore
            }

        });
        return byteBuffer.array();
    }

    private List<NodeTable> nodesDecode(byte[] nodes) {

        int i = nodes.length / 26;
        int index = 0;
        List<NodeTable> nodeTableList = new ArrayList<>();
        for (int j = 0; j < i; j++) {

            byte[] nodeId = Arrays.copyOfRange(nodes, index, index + 20);
            index += 20;
            byte[] ip = Arrays.copyOfRange(nodes, index, index + 4);
            index += 4;
            byte[] port = Arrays.copyOfRange(nodes, index, index + 2);
            index += 2;
            nodeTableList.add(new NodeTable(Hex.encodeHexString(nodeId), Utils.bytesToIp(ip), Utils.bytesToInt2(port), System.currentTimeMillis()));
        }
        return nodeTableList;
    }

    private byte[] getToken(byte[] infoHash) {

        return Arrays.copyOfRange(infoHash, 0, 5);
    }

    private boolean validatorToken(byte[] infoHash, byte[] needValidatorToken) {

        byte[] token = this.getToken(infoHash);

        if (needValidatorToken.length != token.length) {

            log.error("announcePeer validator false length not eq length: {} ", needValidatorToken.length);
            return false;
        }
        for (int i = 0; i < token.length; i++) {

            if (token[i] != needValidatorToken[i]) {

                log.error("announcePeer validator false token not eq ");
                return false;
            }
        }
        return true;
    }

}
