# DHT 网络爬虫

## config.properties 配置文件

1. transactionID=aa     //事务ID
2. peerId=-WW0001-123456789012 //peerID [更多描述](http://www.bittorrent.org/beps/bep_0020.html)
3. serverIp=0.0.0.0     //服务器ip
4. serverPort=6881      //服务器端口
5. minNodes=200         //node表最小数量
6. maxNodes=300         //node表最大数量
7. timeout=60000        //判定node最大超时时间(毫秒)
8. corePoolSize=10      //核心线程池数量 用于请求metadata的客户端
9. maximumPoolSize=10   //最大线程池数量
10. redis.host=localhost//redis 用于存放 announce_peer 请求数据队列
11. redis.port=6379     //

## metadata 接口

需要实现MetaInfo的接口,这里默认实现了两个接口,分别是
1. JsonMetaInfoImpl     // 只负责json方式显示输出
2. MongoMetaInfoImpl    // 保存到mongodb

## 实现协议

- [x] [DHT Protocol](http://www.bittorrent.org/beps/bep_0005.html)
- [x] [Extension for Peers to Send Metadata Files](http://www.bittorrent.org/beps/bep_0009.html)
- [x] [Extension Protocol](http://www.bittorrent.org/beps/bep_0010.html)

## 运行
```
java -Xbootclasspath/a:/path/to/configDir -jar dht-1.0-SNAPSHOT-jar-with-dependencies
```
