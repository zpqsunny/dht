package me.zpq.dht.common;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @author zpq
 * @date 2020/7/23
 */
public class Utils {

    public static byte[] ipToByte(String ip) throws UnknownHostException {

        return InetAddress.getByName(ip).getAddress();
    }

    public static byte[] nodeId() {

        byte[] nodeId = new byte[20];
        ThreadLocalRandom.current().nextBytes(nodeId);
        return nodeId;
    }

    public static byte[] nearNodeId(byte[] nodeId) {

        byte[] nearNodeId = new byte[20];
        System.arraycopy(nodeId, 0, nearNodeId, 0, 10);
        byte[] after = new byte[10];
        ThreadLocalRandom.current().nextBytes(after);
        System.arraycopy(after, 0, nearNodeId, 10, 10);
        return nearNodeId;
    }

    public static byte[] transactionId() {

        byte[] nodeId = new byte[5];
        ThreadLocalRandom.current().nextBytes(nodeId);
        return nodeId;
    }

    public static String bytesToIp(byte[] src) {
        return (src[0] & 0xff) + "." + (src[1] & 0xff) + "." + (src[2] & 0xff)
                + "." + (src[3] & 0xff);
    }

    public static int bytesToInt2(byte[] src) {

        return src[0] << 8 & 0xFF00 | src[1] & 0xFF;
    }

    public static byte[] intToBytes2(int i) {

        byte[] data = new byte[2];
        data[0] = (byte) (i & 0xFF);
        data[1] = (byte) ((i >> 8) & 0xFF);
        return data;
    }

}
