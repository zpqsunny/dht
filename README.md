# DHT 网络爬虫

## 架构设计

DHT Server -> Redis

Redis <- Peer -> (Mongodb && local)        

### 模块解释
- dht-common        公共变量和方法
- dht-fresh         hash的7天内统计日活跃数
- dht-krpc          krpc 协议实现
- dht-peer          Peer 客户端实现(TCP),实现端与端之间的数据交互,来实现获取对端的metadata数据和存储
- dht-routing-table 为dht-server 实现的内部路由表
- dht-server        负责基于UDP传输协议的DHT网络传输Bencode编码的服务器

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
redis.database=0 #redis Database
```
### Peer
```properties
peers.core.pool.size= #peer核心线程数
peers.maximum.pool.size= #peer最大线程数
redis.host=127.0.0.1 #redis地址
redis.port=6379 #redis端口
redis.password= #redis密码
redis.database=0 #redis Database
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

## Docker

运行在Docker

[dht-server](https://hub.docker.com/repository/docker/zpqsunny/dht-server)

[dht-peer](https://hub.docker.com/repository/docker/zpqsunny/dht-peer)

### ENV 环境变量配置

#### DHT Server

```properties
HOST = 127.0.0.1 #服务器IP
PORT = 6881 端口
MIN_NODES = 20#node节点最少数量
MAX_NODES = 5000 #node节点最大数量
REDIS_HOST = 127.0.0.1 #redis地址
REDIS_PORT = 6379 #redis端口
REDIS_PASSWORD = '' #redis密码
REDIS_DATABASE = 0 #redis Database
```

#### DHT Peer

```properties
REDIS_HOST = 127.0.0.1 #redis地址
REDIS_PORT = 6379 #redis端口
REDIS_PASSWORD = '' #redis密码
REDIS_DATABASE = 0 #redis Database
MONGODB_URL = 'mongodb://localhost' #mongodb url
```

## 快速运行

```shell
docker run -d --name redis --network host redis:5.0.10
docker run -d --name dht-server --network host -e HOST={HOST} zpqsunny/dht-server:latest
docker run -d --name mongo --network host -v /docker/mongo/db:/data/db -e MONGO_INITDB_ROOT_USERNAME=admin -e MONGO_INITDB_ROOT_PASSWORD=admin mongo:4.4.1
docker run -d --name dht-peer --network host -v /metadata:/metadata -e MONGODB_URL="mongodb://admin:admin@127.0.0.1:27017/?authSource=admin" -e REDIS_HOST=127.0.0.1 -e REDIS_PORT=6379 zpqsunny/dht-peer:latest
```

## 示例数据

![example-data](example-data.png)
