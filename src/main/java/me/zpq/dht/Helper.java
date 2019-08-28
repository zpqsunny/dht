package me.zpq.dht;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * @author zpq
 * @date 2019-08-26
 */
public class Helper {


    public static byte[] nodesEncode(List<NodeTable> nodes) {

        ByteBuffer byteBuffer = ByteBuffer.allocate(nodes.size() * 26);

        nodes.forEach(nodeTable -> {

            try {
                byteBuffer.put(nodeTable.getNid())
                        .put(ipToByte(nodeTable.getIp()))
                        .put(intToBytes2(nodeTable.getPort()));
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }

        });
        return byteBuffer.array();
    }

    public static List<NodeTable> nodesDecode(byte[] nodes) {

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
            nodeTableList.add(new NodeTable(nodeId, bytesToIp(ip), bytesToInt2(port), null));
        }
        return nodeTableList;
    }


    private static byte[] ipToByte(String ip) throws UnknownHostException {

        return InetAddress.getByName(ip).getAddress();
    }

    public static byte[] nodeId() {

        byte[] nodeId = new byte[20];
        (new Random()).nextBytes(nodeId);
        return nodeId;
    }

    private static String bytesToIp(byte[] src) {
        return (src[0] & 0xff) + "." + (src[1] & 0xff) + "." + (src[2] & 0xff)
                + "." + (src[3] & 0xff);
    }

    private static int bytesToInt2(byte[] src) {

        return src[0] << 8 & 0xFF00 | src[1] & 0xFF;
    }

    private static byte[] intToBytes2(int i) {

        byte[] data = new byte[2];
        data[0] = (byte) (i & 0xFF);
        data[1] = (byte) ((i >> 8) & 0xFF);
        return data;
    }

    public static String bytesToHexString(byte[] src){
        StringBuilder stringBuilder = new StringBuilder();
        if (src == null || src.length <= 0) {
            return null;
        }
        for (byte b : src) {
            int v = b & 0xFF;
            String hv = Integer.toHexString(v);
            if (hv.length() < 2) {
                stringBuilder.append(0);
            }
            stringBuilder.append(hv);
        }
        return stringBuilder.toString();
    }
}
