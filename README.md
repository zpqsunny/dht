# DHT 网络爬虫

![GitHub](https://img.shields.io/github/license/zpqsunny/dht)
![GitHub release (latest by date)](https://img.shields.io/github/v/release/zpqsunny/dht)

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
server.port=6881                #监听端口
server.nodes.min=20             #node节点最少数量
server.nodes.max=3000           #node节点最大数量
server.findNode.interval=60     #执行find_node方法时间间隔（单位秒）
server.ping.interval=300        #执行ping方法时间间隔（单位秒）
server.removeNode.interval=300  #执行删除失效节点时间间隔（单位秒）
server.fresh=false              #是否开启hash统计 需要开启fresh 不然redis list数据会被占满
redis.host=127.0.0.1            #redis地址
redis.port=6379                 #redis端口
redis.password=                 #redis密码
redis.database=0                #redis Database
```
### Peer
```properties
peers.core.pool.size=5          #peer核心线程数
peers.maximum.pool.size=10      #peer最大线程数
redis.host=127.0.0.1            #redis地址
redis.port=6379                 #redis端口
redis.password=                 #redis密码
redis.database=0                #redis Database
mongodb.url=                    #mongodb url
```


## 实现协议

:heavy_check_mark: [DHT Protocol](http://www.bittorrent.org/beps/bep_0005.html)

:heavy_check_mark: [Extension for Peers to Send Metadata Files](http://www.bittorrent.org/beps/bep_0009.html)

:heavy_check_mark: [Extension Protocol](http://www.bittorrent.org/beps/bep_0010.html)

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
PORT = 6881                 #端口
MIN_NODES = 20              #node节点最少数量
MAX_NODES = 5000            #node节点最大数量
FRESH = false               #是否开启hash统计 需要开启fresh 不然redis list数据会被占满
REDIS_HOST = 127.0.0.1      #redis地址
REDIS_PORT = 6379           #redis端口
REDIS_PASSWORD = ''         #redis密码
REDIS_DATABASE = 0          #redis Database
```

#### DHT Peer

```properties
REDIS_HOST = 127.0.0.1              #redis地址
REDIS_PORT = 6379                   #redis端口
REDIS_PASSWORD = ''                 #redis密码
REDIS_DATABASE = 0                  #redis Database
MONGODB_URL = 'mongodb://localhost' #mongodb url
```

## 快速运行

**docker**
```shell
docker run -d --name redis --network host redis:5.0.10
docker run -d --name dht-server --network host zpqsunny/dht-server:latest
docker run -d --name mongo --network host -v /docker/mongo/db:/data/db -e MONGO_INITDB_ROOT_USERNAME=admin -e MONGO_INITDB_ROOT_PASSWORD=admin mongo:4.4.1
docker run -d --name dht-peer --network host -v /metadata:/metadata -e MONGODB_URL="mongodb://admin:admin@127.0.0.1:27017/?authSource=admin" -e REDIS_HOST=127.0.0.1 -e REDIS_PORT=6379 zpqsunny/dht-peer:latest
```

**docker-compose**
```yaml
services:
  redis:
    container_name: redis
    image: redis:5.0.10
    network_mode: host
    restart: unless-stopped
  dht-server-1: &dht-server
    depends_on:
      - redis
    image: zpqsunny/dht-server:latest
    build:
      context: dht-server
      dockerfile: Dockerfile
    network_mode: host
    restart: unless-stopped
    environment:
      PORT: 6881
      REDIS_HOST: 127.0.0.1
      REDIS_PORT: 6379
      REDIS_PASSWORD:
      REDIS_DATABASE: 0
  dht-server-2:
    <<: *dht-server
    environment:
      PORT: 6882
  dht-server-3:
    <<: *dht-server
    environment:
      PORT: 6883
  mongo:
    container_name: mongo
    image: mongo:4.4.1
    volumes:
      - /docker/mongo/db:/data/db
      - /docker/mongo/backup:/backup
    environment:
      MONGO_INITDB_ROOT_USERNAME: admin
      MONGO_INITDB_ROOT_PASSWORD: admin
    network_mode: host
    restart: unless-stopped
  dht-peer:
    depends_on:
      - redis
      - mongo
    deploy:
      mode: replicated
      replicas: 3
    image: zpqsunny/dht-peer:latest
    build:
      context: dht-server
      dockerfile: Dockerfile
    network_mode: host
    restart: unless-stopped
    volumes:
      - /metadata:/metadata
    environment:
      MONGODB_URL: mongodb://admin:admin@127.0.0.1:27017/?authSource=admin
      REDIS_HOST: 127.0.0.1
      REDIS_PORT: 6379
      REDIS_PASSWORD:
      REDIS_DATABASE: 0
```
```shell
docker-compose up
```
## 示例数据

![example-data](example-data.png)
