package me.zpq.dht;


public class MetaInfoRequest {

    private String ip;

    private int port;

    private byte[] infoHash;

    public MetaInfoRequest(String ip, int port, byte[] infoHash) {
        this.ip = ip;
        this.port = port;
        this.infoHash = infoHash;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public byte[] getInfoHash() {
        return infoHash;
    }

    public void setInfoHash(byte[] infoHash) {
        this.infoHash = infoHash;
    }

    @Override
    public String toString() {

        return "{\"ip\":\"" + ip + "\",\"port\":" + port + ",\"info_hash\":\"" + Helper.bytesToHex(infoHash) + "\"}";
    }
}
