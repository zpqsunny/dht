package me.zpq.dht;

import java.util.Arrays;

/**
 * @author zpq
 * @date 2019-08-26
 */
public class NodeTable {

    private byte[] nid;

    private String ip;

    private Integer port;

    private Long time;

    public NodeTable() {
    }

    public NodeTable(byte[] nid, String ip, Integer port, Long time) {
        this.nid = nid;
        this.ip = ip;
        this.port = port;
        this.time = time;
    }

    public byte[] getNid() {
        return nid;
    }

    public void setNid(byte[] nid) {
        this.nid = nid;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public Long getTime() {
        return time;
    }

    public void setTime(Long time) {
        this.time = time;
    }

    @Override
    public String toString() {
        return "NodeTable{" +
                "nid=" + Arrays.toString(nid) +
                ", ip='" + ip + '\'' +
                ", port=" + port +
                ", time=" + time +
                '}';
    }
}
