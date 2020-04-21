# DHT 网络爬虫

## config.properties 配置文件

1. server.ip=0.0.0.0                    //服务器ip
2. server.peers.core.pool.size=20       //核心线程池数量 用于请求metadata的客户端
3. server.peers.maximum.pool.size=40    //最大线程池数量

## 实现协议

- [x] [DHT Protocol](http://www.bittorrent.org/beps/bep_0005.html)
- [x] [Extension for Peers to Send Metadata Files](http://www.bittorrent.org/beps/bep_0009.html)
- [x] [Extension Protocol](http://www.bittorrent.org/beps/bep_0010.html)

## 运行
jar包和config.properties配置文件要在同一目录
```shell script
java  -jar dht-1.0-SNAPSHOT-jar-with-dependencies.jar
```
