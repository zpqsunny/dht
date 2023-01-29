#!/bin/bash

echo '##########################################'
echo '#### 0: 检查环境 Docker docker-compose  ###'
echo '#### 1: 初始化   Elasticsearch          ###'
echo '#### 2: 初始化   mongodb                ###'
initElasticsearch() {
  echo -e "\033[32m start Elasticsearch \033[0m"
  docker-compose up -d elasticsearch
  echo -e "\033[32m elasticsearch install plugin analysis \033[0m"
  /usr/bin/expect <<EOF
  spawn exec -it elasticsearch bash
  exec sleep 1
  send "elasticsearch-plugin install https://github.com/medcl/elasticsearch-analysis-ik/releases/download/v7.17.7/elasticsearch-analysis-ik-7.17.7.zip \n"
  expect "*n"
  send "y\n"
  expect "100%"
  send "exit\n"
EOF
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

  if [ $(which expect | grep -c "expect") -ne 1 ]; then
      yum install expect -y
  fi
  
  # check docker
  if [ $(docker --version | grep -c "Docker") -ne 1 ]; then
     yum install docker -y
     systemctl enable docker
     systemctl start docker
  fi
  echo -e "\033[32m Docker ok \033[0m";
  if [ $(docker-compose -v | grep -c "Docker Compose") -ne 1 ]; then
    curl -SL https://github.com/docker/compose/releases/download/v2.15.1/docker-compose-linux-x86_64 -o /usr/local/bin/docker-compose
    chmod +x /usr/local/bin/docker-compose
    ln -s /usr/local/bin/docker-compose /usr/bin/docker-compose
  fi
  echo -e "\033[32m Docker Compose ok \033[0m";
  if [ $(ls -l | grep -c "docker-compose.yml") -ne 1 ]; then
    wget https://raw.githubusercontent.com/zpqsunny/dht/main/docker-compose.yml
  fi
  if [ $(ls -l | grep -c "mapping.json") -ne 1 ]; then
    wget https://raw.githubusercontent.com/zpqsunny/dht/main/mapping.json
  fi
  echo -e "\033[32m mapping.json ok \033[0m";
  if [ $(ls -l | grep -c "setting.json") -ne 1 ]; then
    wget https://raw.githubusercontent.com/zpqsunny/dht/main/setting.json
  echo -e "\033[32m setting.json ok \033[0m";
  fi
}
initMongoDB() {
  if [ $(docker ps -a | grep -c "mongo") -eq 1];
   then
    docker rm -f mongo
    rm -rf /docker/mongo
  fi
  docker-compose up -d mongo
  sleep 5
  # init mongo
  /usr/bin/expect <<EOF
  spawn docker exec -it mongo bash
  exec sleep 1
  send "mongo --host 127.0.0.1:27018 -u \\\$MONGO_INITDB_ROOT_USERNAME -p \\\$MONGO_INITDB_ROOT_PASSWORD \n"
  exec sleep 1
  send "rs.initiate()\n"
  exec sleep 1
  send "exit\n"
  exec sleep 1
  send "exit\n"
EOF
}
read c
case $c in
  0) checkSystem
    ;;
  1) initElasticsearch
    ;;
  2) initMongoDB
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
firewall-cmd --add-port 721/tcp --permanent
firewall-cmd --add-port 9300/tcp --permanent
firewall-cmd --reload
