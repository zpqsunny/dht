# DHT 网络爬虫

## 架构设计

DHT Server -> Redis

Redis <- Peer -> (Mongodb && local)           
## config.properties 配置文件

### DHT Server
```properties
server.ip=0.0.0.0 #服务器IP
server.port=6881 #监听端口
server.nodes.min=20 #node节点最少数量
server.nodes.max=3000 #node节点最大数量
server.findNode.interval=60 #执行find_node方法时间间隔（单位秒）
server.ping.interval=300 #执行ping方法时间间隔（单位秒）
server.removeNode.interval=300 #执行删除失效节点时间间隔（单位秒）
redis.host=127.0.0.1 #redis地址
redis.port=6379 #redis端口
redis.password= #redis密码
```
### Peer
```properties
peers.core.pool.size= #peer核心线程数
peers.maximum.pool.size= #peer最大线程数
redis.host=127.0.0.1 #redis地址
redis.port=6379 #redis端口
redis.password= #redis密码
mongodb.url= #mongodb url
```


## 实现协议

- [x] [DHT Protocol](http://www.bittorrent.org/beps/bep_0005.html)
- [x] [Extension for Peers to Send Metadata Files](http://www.bittorrent.org/beps/bep_0009.html)
- [x] [Extension Protocol](http://www.bittorrent.org/beps/bep_0010.html)

## 运行

jar包和config.properties配置文件要在同一目录

```shell script
java  -jar dht-server-1.0-SNAPSHOT-jar-with-dependencies.jar &
java  -jar dht-peer-1.0-SNAPSHOT-jar-with-dependencies.jar &
```
