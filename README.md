# DHT 网络爬虫

## config.properties 配置文件

3. serverIp=0.0.0.0     //服务器ip
8. corePoolSize=5       //核心线程池数量 用于请求metadata的客户端
9. maximumPoolSize=10   //最大线程池数量

## 实现协议

- [x] [DHT Protocol](http://www.bittorrent.org/beps/bep_0005.html)
- [x] [Extension for Peers to Send Metadata Files](http://www.bittorrent.org/beps/bep_0009.html)
- [x] [Extension Protocol](http://www.bittorrent.org/beps/bep_0010.html)

## 运行
```shell script
java -Xbootclasspath/a:/path/to/configDir -jar dht-1.0-SNAPSHOT-jar-with-dependencies.jar
```
