package me.zpq.krpc;

import be.adaxisoft.bencode.BEncodedValue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static me.zpq.krpc.KrpcConstant.*;

/**
 * @author zpq
 * @date 2020/7/23
 */
public class KrpcProtocol {

    public static Map<String, BEncodedValue> query(byte[] transactionId, String q, Map<String, BEncodedValue> a) throws IOException {

        Map<String, BEncodedValue> document = new HashMap<>(6);
        document.put(T, new BEncodedValue(transactionId));
        document.put(Y, new BEncodedValue(Q));
        document.put(Q, new BEncodedValue(q));
        document.put(A, new BEncodedValue(a));
        return document;
    }

    public static Map<String, BEncodedValue> response(byte[] transactionId, Map<String, BEncodedValue> r) throws IOException {

        Map<String, BEncodedValue> document = new HashMap<>(6);
        document.put(T, new BEncodedValue(transactionId));
        document.put(Y, new BEncodedValue(R));
        document.put(R, new BEncodedValue(r));
        return document;
    }

    public static Map<String, BEncodedValue> error(byte[] transactionId, Integer code, String description) throws IOException {

        Map<String, BEncodedValue> document = new HashMap<>(6);
        document.put(T, new BEncodedValue(transactionId));
        document.put(Y, new BEncodedValue(E));
        List<BEncodedValue> list = new ArrayList<>();
        list.add(new BEncodedValue(code));
        list.add(new BEncodedValue(description));
        document.put(E, new BEncodedValue(list));
        return document;
    }

    public static Map<String, BEncodedValue> pingQuery(byte[] transactionId, byte[] nodeId) throws IOException {

        Map<String, BEncodedValue> data = new HashMap<>(6);
        data.put(ID, new BEncodedValue(nodeId));
        return query(transactionId, PING, data);
    }

    public static Map<String, BEncodedValue> pingResponse(byte[] transactionId, byte[] nodeId) throws IOException {

        Map<String, BEncodedValue> data = new HashMap<>(6);
        data.put(ID, new BEncodedValue(nodeId));
        return response(transactionId, data);
    }

    public static Map<String, BEncodedValue> findNodeQuery(byte[] transactionId, byte[] nodeId, byte[] target) throws IOException {

        Map<String, BEncodedValue> data = new HashMap<>(6);
        data.put(ID, new BEncodedValue(nodeId));
        data.put(TARGET, new BEncodedValue(target));
        return query(transactionId, FIND_NODE, data);
    }

    public static Map<String, BEncodedValue> findNodeResponse(byte[] transactionId, byte[] nodeId, byte[] nodes) throws IOException {

        Map<String, BEncodedValue> data = new HashMap<>(6);
        data.put(ID, new BEncodedValue(nodeId));
        data.put(NODES, new BEncodedValue(nodes));
        return response(transactionId, data);
    }

    public static Map<String, BEncodedValue> getPeersQuery(byte[] transactionId, byte[] nodeId, byte[] infoHash) throws IOException {

        Map<String, BEncodedValue> data = new HashMap<>(6);
        data.put(ID, new BEncodedValue(nodeId));
        data.put(INFO_HASH, new BEncodedValue(infoHash));
        return query(transactionId, GET_PEERS, data);
    }

    public static Map<String, BEncodedValue> getPeersResponseNodes(byte[] transactionId, byte[] nodeId, byte[] token, byte[] nodes) throws IOException {

        Map<String, BEncodedValue> data = new HashMap<>(6);
        data.put(ID, new BEncodedValue(nodeId));
        data.put(TOKEN, new BEncodedValue(token));
        data.put(NODES, new BEncodedValue(nodes));
        return response(transactionId, data);
    }

    public static Map<String, BEncodedValue> announcePeerResponse(byte[] transactionId, byte[] nodeId) throws IOException {

        Map<String, BEncodedValue> data = new HashMap<>(6);
        data.put(ID, new BEncodedValue(nodeId));
        return response(transactionId, data);
    }
}
