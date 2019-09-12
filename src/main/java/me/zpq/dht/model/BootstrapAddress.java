package me.zpq.dht.model;

/**
 * @author zpq
 * @date 2019-08-27
 */
public class BootstrapAddress {

    private String host;

    private Integer port;

    public BootstrapAddress() {
    }

    public BootstrapAddress(String host, Integer port) {
        this.host = host;
        this.port = port;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }
}
