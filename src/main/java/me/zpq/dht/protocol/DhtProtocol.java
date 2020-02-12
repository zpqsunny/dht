package me.zpq.dht.protocol;

import be.adaxisoft.bencode.BEncodedValue;
import be.adaxisoft.bencode.BEncoder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author zpq
 * @date 2019-08-26
 */
public class DhtProtocol {

    public static final String A = "a";

    public static final String Y = "y";

    public static final String Q = "q";

    public static final String T = "t";

    public static final String R = "r";

    public static final String E = "e";

    public static final String PING = "ping";

    public static final String FIND_NODE = "find_node";

    public static final String GET_PEERS = "get_peers";

    public static final String ANNOUNCE_PEER = "announce_peer";

    public static final String ID = "id";

    public static final String NODES = "nodes";

    public static final String IMPLIED_PORT = "implied_port";

    public static final String VALUES = "values";

    public static final String TARGET = "target";

    public static final String INFO_HASH = "info_hash";

    public static final String PORT = "port";

    public static final String TOKEN = "token";

    private static byte[] result(Map<String, BEncodedValue> document) throws IOException {

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        BEncoder.encode(document, byteArrayOutputStream);
        return byteArrayOutputStream.toByteArray();
    }

    private static byte[] query(byte[] transactionId, String q, Map<String, BEncodedValue> a) throws IOException {

        Map<String, BEncodedValue> document = new HashMap<>(6);
        document.put(T, new BEncodedValue(transactionId));
        document.put(Y, new BEncodedValue(Q));
        document.put(Q, new BEncodedValue(q));
        document.put(A, new BEncodedValue(a));
        return result(document);
    }

    private static byte[] response(byte[] transactionId, Map<String, BEncodedValue> r) throws IOException {

        Map<String, BEncodedValue> document = new HashMap<>(6);
        document.put(T, new BEncodedValue(transactionId));
        document.put(Y, new BEncodedValue(R));
        document.put(R, new BEncodedValue(r));
        return result(document);

    }

    public static byte[] error(byte[] transactionId, Integer code, String description) throws IOException {

        Map<String, BEncodedValue> document = new HashMap<>(6);
        document.put(T, new BEncodedValue(transactionId));
        document.put(Y, new BEncodedValue(E));
        List<BEncodedValue> list = new ArrayList<>();
        list.add(new BEncodedValue(code));
        list.add(new BEncodedValue(description));
        document.put(E, new BEncodedValue(list));
        return result(document);
    }

    public static byte[] pingQuery(byte[] transactionId, byte[] nodeId) throws IOException {

        Map<String, BEncodedValue> data = new HashMap<>(6);
        data.put(ID, new BEncodedValue(nodeId));
        return query(transactionId, PING, data);
    }

    public static byte[] pingResponse(byte[] transactionId, byte[] nodeId) throws IOException {

        Map<String, BEncodedValue> data = new HashMap<>(6);
        data.put(ID, new BEncodedValue(nodeId));
        return response(transactionId, data);
    }

    public static byte[] findNodeQuery(byte[] transactionId, byte[] nodeId, byte[] target) throws IOException {

        Map<String, BEncodedValue> data = new HashMap<>(6);
        data.put(ID, new BEncodedValue(nodeId));
        data.put(TARGET, new BEncodedValue(target));
        return query(transactionId, FIND_NODE, data);
    }

    public static byte[] findNodeResponse(byte[] transactionId, byte[] nodeId, byte[] nodes) throws IOException {

        Map<String, BEncodedValue> data = new HashMap<>(6);
        data.put(ID, new BEncodedValue(nodeId));
        data.put(NODES, new BEncodedValue(nodes));
        return response(transactionId, data);
    }

    public static byte[] getPeersQuery(byte[] transactionId, byte[] nodeId, byte[] infoHash) throws IOException {

        Map<String, BEncodedValue> data = new HashMap<>(6);
        data.put(ID, new BEncodedValue(nodeId));
        data.put(INFO_HASH, new BEncodedValue(infoHash));
        return query(transactionId, GET_PEERS, data);
    }

    public static byte[] getPeersResponseNodes(byte[] transactionId, byte[] nodeId, byte[] token, byte[] nodes) throws IOException {

        Map<String, BEncodedValue> data = new HashMap<>(6);
        data.put(ID, new BEncodedValue(nodeId));
        data.put(TOKEN, new BEncodedValue(token));
        data.put(NODES, new BEncodedValue(nodes));
        return response(transactionId, data);
    }

    public static byte[] announcePeerResponse(byte[] transactionId, byte[] nodeId) throws IOException {

        Map<String, BEncodedValue> data = new HashMap<>(6);
        data.put(ID, new BEncodedValue(nodeId));
        return response(transactionId, data);
    }
}
