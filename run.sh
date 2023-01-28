#!/bin/bash

echo '##########################################'
echo '#### 0: 检查环境 Docker docker-compose  ###'
echo '#### 1: 初始化   Elasticsearch       ###'
echo '#### 1: 初始化   mongodb       ###'
initElasticsearch() {
  echo "initElasticsearch"
  docker-compose up -d elasticsearch
  # elasticsearch install plugin analysis
  docker exec -it elasticsearch bash
  elasticsearch-plugin install https://github.com/medcl/elasticsearch-analysis-ik/releases/download/v7.17.7/elasticsearch-analysis-ik-7.17.7.zip
  echo -e "y\n"
  docker-compose restart elasticsearch
  echo "drop index if exits"
  curl -H "Content-Type: application/json" -X DELETE -d '' -u elastic:elastic http://127.0.0.1:9200/metadata
  echo "create index"
  curl -H "Content-Type: application/json" -X PUT    -d '{}' -u elastic:elastic http://127.0.0.1:9200/metadata
  echo "update mapping"
  curl -H "Content-Type: application/json" -X POST   -d @mapping.json -u elastic:elastic http://127.0.0.1:9200/metadata/_mapping
  echo "update setting"
  curl -H "Content-Type: application/json" -X PUT    -d @setting.json -u elastic:elastic http://127.0.0.1:9200/metadata/_settings

  exit
}
checkSystem() {
  # check docker
  if [ $(docker --version | grep -c "Docker") -eq 1 ];
  then
     echo "Docker ok";
  else
    yum install docker -y
  fi
  if [ $(docker-compose -v | grep -c "Docker Compose") -eq 1 ];
  then
     echo "Docker Compose ok";
  else
    curl -SL https://github.com/docker/compose/releases/download/v2.15.1/docker-compose-linux-x86_64 -o /usr/local/bin/docker-compose
    chmod +x /usr/local/bin/docker-compose
    ln -s /usr/local/bin/docker-compose /usr/bin/docker-compose
  fi
}
curl -SL https://raw.githubusercontent.com/zpqsunny/dht/main/docker-compose.yml -o ./docker-compose.yml
read c
case $c in
  0) checkSystem
    ;;
  1) initElasticsearch
    ;;
  *) echo "fail"
    ;;
esac




# docker-compose
curl -SL https://github.com/docker/compose/releases/download/v2.15.1/docker-compose-linux-x86_64 -o /usr/local/bin/docker-compose
chmod +x /usr/local/bin/docker-compose
ln -s /usr/local/bin/docker-compose /usr/bin/docker-compose

# up mongo and es
docker-compose up -d elasticsearch
docker-compose up -d mongo

# init mongo
docker exec -it mongo bash
mongo --host 127.0.0.1:27018 -u admin -p
rs.initiate()

# if have mongo backup
mongorestore --authenticationDatabase=admin -u admin -p admin --port 27018 -d dht -c metadata /backup/dht/metadata.bson

exit
exit
# elasticsearch install plugin analysis
docker exec -it elasticsearch bash
elasticsearch-plugin install https://github.com/medcl/elasticsearch-analysis-ik/releases/download/v7.17.7/elasticsearch-analysis-ik-7.17.7.zip

exit
docker-compose restart elasticsearch
# drop index if exits
curl -H "Content-Type: application/json" -X DELETE -d '' -u elastic:elastic http://127.0.0.1:9200/metadata
# create index
curl -H "Content-Type: application/json" -X PUT    -d '{}' -u elastic:elastic http://127.0.0.1:9200/metadata
# update mapping
curl -H "Content-Type: application/json" -X POST   -d @mapping.json -u elastic:elastic http://127.0.0.1:9200/metadata/_mapping
# update setting
curl -H "Content-Type: application/json" -X PUT    -d @setting.json -u elastic:elastic http://127.0.0.1:9200/metadata/_settings




#start other server
docker-compose up -d

docker-compose logs -f --tail 20 dht-peer dht-es

# firewalld
firewall-cmd --add-port 6881/udp --permanent
firewall-cmd --add-port 6882/udp --permanent
firewall-cmd --add-port 6883/udp --permanent
firewall-cmd --add-port 9200/tcp --permanent
firewall-cmd --add-port 9300/tcp --permanent
firewall-cmd --reload
