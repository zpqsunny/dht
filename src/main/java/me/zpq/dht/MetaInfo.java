package me.zpq.dht;

public interface MetaInfo {

    void todoSomething(byte[] sha1, byte[] info) throws Exception;

    /**
     * 用于 dht AnnouncePeer请求后置操作
     * @param host ip
     * @param port 端口
     * @param sha1 文件sha1 长度20
     */
    void onAnnouncePeer(String host, Integer port, byte[] sha1);
}
