package me.zpq.dht;


import java.util.Arrays;

public class MetaInfoRequest {

    private String ip;

    private int p;

    private byte[] infoHash;

    public MetaInfoRequest() {
    }

    public MetaInfoRequest(String ip, int p, byte[] infoHash) {
        this.ip = ip;
        this.p = p;
        this.infoHash = infoHash;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getP() {
        return p;
    }

    public void setP(int p) {
        this.p = p;
    }

    public byte[] getInfoHash() {
        return infoHash;
    }

    public void setInfoHash(byte[] infoHash) {
        this.infoHash = infoHash;
    }

    @Override
    public String toString() {
        return "MetaInfoRequest{" +
                "ip='" + ip + '\'' +
                ", p=" + p +
                ", infoHash=" + Arrays.toString(infoHash) +
                '}';
    }
}
