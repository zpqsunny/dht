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

    private byte[] result(Map<String, BEncodedValue> document) throws IOException {

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        BEncoder.encode(document, byteArrayOutputStream);
        return byteArrayOutputStream.toByteArray();
    }

    private byte[] query(byte[] transactionId, String q, Map<String, BEncodedValue> a) throws IOException {

        Map<String, BEncodedValue> document = new HashMap<>(6);
        document.put("t", new BEncodedValue(transactionId));
        document.put("y", new BEncodedValue("q"));
        document.put("q", new BEncodedValue(q));
        document.put("a", new BEncodedValue(a));
        return this.result(document);
    }

    private byte[] response(byte[] transactionId, Map<String, BEncodedValue> r) throws IOException {

        Map<String, BEncodedValue> document = new HashMap<>(6);
        document.put("t", new BEncodedValue(transactionId));
        document.put("y", new BEncodedValue("r"));
        document.put("r", new BEncodedValue(r));
        return this.result(document);

    }

    public byte[] error(byte[] transactionId, Integer code, String description) throws IOException {

        Map<String, BEncodedValue> document = new HashMap<>(6);
        document.put("t", new BEncodedValue(transactionId));
        document.put("y", new BEncodedValue("e"));
        List<BEncodedValue> list = new ArrayList<>();
        list.add(new BEncodedValue(code));
        list.add(new BEncodedValue(description));
        document.put("e", new BEncodedValue(list));
        return this.result(document);
    }

    public byte[] pingQuery(byte[] transactionId, byte[] nodeId) throws IOException {

        Map<String, BEncodedValue> data = new HashMap<>(6);
        data.put("id", new BEncodedValue(nodeId));
        return this.query(transactionId, "ping", data);
    }

    public byte[] pingResponse(byte[] transactionId, byte[] nodeId) throws IOException {

        Map<String, BEncodedValue> data = new HashMap<>(6);
        data.put("id", new BEncodedValue(nodeId));
        return this.response(transactionId, data);
    }

    public byte[] findNodeQuery(byte[] transactionId, byte[] nodeId, byte[] target) throws IOException {

        Map<String, BEncodedValue> data = new HashMap<>(6);
        data.put("id", new BEncodedValue(nodeId));
        data.put("target", new BEncodedValue(target));
        return this.query(transactionId, "find_node", data);
    }

    public byte[] findNodeResponse(byte[] transactionId, byte[] nodeId, byte[] nodes) throws IOException {

        Map<String, BEncodedValue> data = new HashMap<>(6);
        data.put("id", new BEncodedValue(nodeId));
        data.put("nodes", new BEncodedValue(nodes));
        return this.response(transactionId, data);
    }

    public byte[] getPeersQuery(byte[] transactionId, byte[] nodeId, byte[] infoHash) throws IOException {

        Map<String, BEncodedValue> data = new HashMap<>(6);
        data.put("id", new BEncodedValue(nodeId));
        data.put("info_hash", new BEncodedValue(infoHash));
        return this.query(transactionId, "get_peers", data);
    }

    public byte[] getPeersResponseNodes(byte[] transactionId, byte[] nodeId, byte[] token, byte[] nodes) throws IOException {

        Map<String, BEncodedValue> data = new HashMap<>(6);
        data.put("id", new BEncodedValue(nodeId));
        data.put("token", new BEncodedValue(token));
        data.put("nodes", new BEncodedValue(nodes));
        return this.response(transactionId, data);
    }

    public byte[] announcePeerResponse(byte[] transactionId, byte[] nodeId) throws IOException {

        Map<String, BEncodedValue> data = new HashMap<>(6);
        data.put("id", new BEncodedValue(nodeId));
        return this.response(transactionId, data);
    }
}
