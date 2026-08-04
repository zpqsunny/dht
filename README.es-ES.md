# Rastreador DHT en red

![GitHub](https://img.shields.io/github/license/zpqsunny/dht)
![GitHub release (latest by date)](https://img.shields.io/github/v/release/zpqsunny/dht)

[![](https://img.shields.io/chrome-web-store/v/jekflgekjidcpibhnnpiimekgckgnkop)](https://chrome.google.com/webstore/detail/transmission-web-ui/jekflgekjidcpibhnnpiimekgckgnkop)
[![](https://img.shields.io/chrome-web-store/rating/jekflgekjidcpibhnnpiimekgckgnkop)](https://chrome.google.com/webstore/detail/transmission-web-ui/jekflgekjidcpibhnnpiimekgckgnkop)
[![](https://img.shields.io/chrome-web-store/users/jekflgekjidcpibhnnpiimekgckgnkop)](https://chrome.google.com/webstore/detail/transmission-web-ui/jekflgekjidcpibhnnpiimekgckgnkop)

[![](https://img.shields.io/badge/dynamic/json?label=edge%20web%20store&prefix=v&query=%24.version&url=https%3A%2F%2Fmicrosoftedge.microsoft.com%2Faddons%2Fgetproductdetailsbycrxid%2Fgplhiomfemapanllhkkigblmhkbmjgfc)](https://microsoftedge.microsoft.com/addons/detail/transmission-web-ui/gplhiomfemapanllhkkigblmhkbmjgfc)
[![](https://img.shields.io/badge/dynamic/json?label=rating&suffix=/5&query=%24.averageRating&url=https%3A%2F%2Fmicrosoftedge.microsoft.com%2Faddons%2Fgetproductdetailsbycrxid%2Fgplhiomfemapanllhkkigblmhkbmjgfc)](https://microsoftedge.microsoft.com/addons/detail/transmission-web-ui/gplhiomfemapanllhkkigblmhkbmjgfc)
[![](https://img.shields.io/badge/dynamic/json?label=users&query=%24.activeInstallCount&url=https%3A%2F%2Fmicrosoftedge.microsoft.com%2Faddons%2Fgetproductdetailsbycrxid%2Fgplhiomfemapanllhkkigblmhkbmjgfc)](https://microsoftedge.microsoft.com/addons/detail/transmission-web-ui/gplhiomfemapanllhkkigblmhkbmjgfc)

## Diseño de arquitectura

Servidor DHT -> Redis

Redis <- Par -> (MongoDB y local)

### Explicación de los módulos
- dht-common: Variables y métodos comunes.
- dht-fresh: Número de usuarios activos diarios de hash en los últimos 7 días.
- dht-krpc: Implementación del protocolo KRPC.
- dht-peer: Implementación del cliente Peer (TCP) para la interacción de datos entre extremos, que permite obtener y almacenar los metadatos del par remoto.
- dht-routing-table: Tabla de enrutamiento interna implementada para dht-server.
- dht-server: Servidor responsable de la transmisión de red DHT basada en UDP con codificación Bencode.

## Archivo de configuración: config.properties

### Servidor DHT
```properties
server.port=6881                # Puerto de escucha
server.nodes.min=20             # Número mínimo de nodos
server.nodes.max=3000           # Número máximo de nodos
server.findNode.interval=60     # Intervalo de tiempo (en segundos) para ejecutar el método find_node
server.ping.interval=300        # Intervalo de tiempo (en segundos) para ejecutar el método ping
server.removeNode.interval=300  # Intervalo de tiempo (en segundos) para ejecutar la eliminación de nodos inválidos
server.fresh=false              # ¿Habilitar la estadística de hash? Si no se habilita fresh, la lista de datos de Redis se llenará.
redis.host=127.0.0.1            # Dirección de Redis
redis.port=6379                 # Puerto de Redis
redis.password=                 # Contraseña de Redis
redis.database=0                # Base de datos de Redis
```
### Par
```properties
peers.core.pool.size=5          # Número de hilos centrales de Peer
peers.maximum.pool.size=10      # Número máximo de hilos de Peer
redis.host=127.0.0.1            # Dirección de Redis
redis.port=6379                 # Puerto de Redis
redis.password=                 # Contraseña de Redis
redis.database=0                # Base de datos de Redis
mongodb.url=                    # URL de MongoDB
```

## Protocolo implementado

:heavy_check_mark: [Protocolo DHT](http://www.bittorrent.org/beps/bep_0005.html)

:heavy_check_mark: [Extensión para que los pares envíen archivos de metadatos](http://www.bittorrent.org/beps/bep_0009.html)

:heavy_check_mark: [Protocolo de extensión](http://www.bittorrent.org/beps/bep_0010.html)

## Ejecución

El archivo jar y el archivo de configuración config.properties deben estar en el mismo directorio.

```shell
java -jar dht-server-1.0-SNAPSHOT-jar-with-dependencies.jar &
java -jar dht-peer-1.0-SNAPSHOT-jar-with-dependencies.jar &
```

## Docker

Ejecución en Docker

[dht-server](https://hub.docker.com/repository/docker/zpqsunny/dht-server)

[dht-peer](https://hub.docker.com/repository/docker/zpqsunny/dht-peer)

### Variables de entorno de configuración

#### DHT Server

```properties
PORT = 6881                 # Puerto
MIN_NODES = 20              # Número mínimo de nodos
MAX_NODES = 5000            # Número máximo de nodos
FRESH = false               # ¿Habilitar la estadística de hash? Si no se habilita fresh, la lista de datos de Redis se llenará.
REDIS_HOST = 127.0.0.1      # Dirección de Redis
REDIS_PORT = 6379           # Puerto de Redis
REDIS_PASSWORD = ''         # Contraseña de Redis
REDIS_DATABASE = 0          # Base de datos de Redis
```

#### DHT Peer

```properties
REDIS_HOST = 127.0.0.1              # Dirección de Redis
REDIS_PORT = 6379                   # Puerto de Redis
REDIS_PASSWORD = ''                 # Contraseña de Redis
REDIS_DATABASE = 0                  # Base de datos de Redis
MONGODB_URL = 'mongodb://localhost' # URL de MongoDB
```

## Ejecución rápida

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
## Datos de ejemplo

![datos de ejemplo](example-data.png)


## Stargazers a lo largo del tiempo

[![Stargazers a lo largo del tiempo](https://starchart.cc/zpqsunny/dht.svg)](https://starchart.cc/zpqsunny/dht)

## Agradecimientos

> [IntelliJ IDEA](https://zh.wikipedia.org/zh-hans/IntelliJ_IDEA) es un IDE que mejora al máximo la productividad de los desarrolladores en todos los aspectos, y está diseñado para lenguajes de la plataforma JVM.

Un agradecimiento especial a JetBrains por proporcionar licencias gratuitas de [IntelliJ IDEA](https://www.jetbrains.com/) y otros IDE para proyectos de código abierto.

![Logotipo de IntelliJ IDEA](https://resources.jetbrains.com/storage/products/company/brand/logos/IntelliJ_IDEA.svg)
