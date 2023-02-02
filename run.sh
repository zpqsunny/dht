#!/bin/bash

initElasticsearch() {
  docker-compose stop elasticsearch
  docker-compose rm -f elasticsearch
  rm -rf /docker/elasticsearch/data
  rm -rf /docker/elasticsearch/plugins
  mkdir -p /docker/elasticsearch/data
  mkdir -p /docker/elasticsearch/plugins
  chmod 777 /docker/elasticsearch/data
  chmod 777 /docker/elasticsearch/plugins
  echo -e "\033[32m start Elasticsearch \033[0m"
  docker-compose up -d elasticsearch
  sleep 10
  echo -e "\033[32m elasticsearch install plugin analysis \033[0m"

#  docker-compose restart elasticsearch
#  sleep 15
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
  if [ $(which docker | grep -c "docker") -ne 1 ]; then
     yum install docker -y
     systemctl enable docker
     systemctl start docker
  fi
  echo -e "\033[32m Docker ok \033[0m";

  if [ $(which docker-compose | grep -c "docker-compose") -ne 1 ]; then
    curl -SL https://github.com/docker/compose/releases/download/v2.15.1/docker-compose-linux-x86_64 -o /usr/local/bin/docker-compose
    chmod +x /usr/local/bin/docker-compose
    ln -s /usr/local/bin/docker-compose /usr/bin/docker-compose
  fi
  echo -e "\033[32m Docker Compose ok \033[0m";

  if [ $(ls | grep -c "docker-compose.yml") -ne 1 ]; then
    wget https://raw.githubusercontent.com/zpqsunny/dht/main/docker-compose.yml
  fi
  echo -e "\033[32m docker-compose.yml ok \033[0m";

  if [ $(ls | grep -c "^es.env$") -ne 1 ]; then
    wget https://raw.githubusercontent.com/zpqsunny/dht/main/es.env
  fi
  echo -e "\033[32m es.env ok \033[0m";

  if [ $(ls | grep -c "^mongo.env$") -ne 1 ]; then
    wget https://raw.githubusercontent.com/zpqsunny/dht/main/mongo.env
  fi
  echo -e "\033[32m mongo.env ok \033[0m";

  if [ $(ls | grep -c "^dht-es.env$") -ne 1 ]; then
    wget https://raw.githubusercontent.com/zpqsunny/dht/main/dht-es.env
  fi
  echo -e "\033[32m dht-es.env ok \033[0m";

  if [ $(ls | grep -c "^dht-mongo.env$") -ne 1 ]; then
    wget https://raw.githubusercontent.com/zpqsunny/dht/main/dht-mongo.env
  fi
  echo -e "\033[32m dht-mongo.env ok \033[0m";

  if [ $(ls | grep -c "^dht-redis.env$") -ne 1 ]; then
    wget https://raw.githubusercontent.com/zpqsunny/dht/main/dht-redis.env
  fi
  echo -e "\033[32m dht-redis.env ok \033[0m";

  if [ $(ls | grep -c "^mapping.json$") -ne 1 ]; then
    wget https://raw.githubusercontent.com/zpqsunny/dht/main/mapping.json
  fi
  echo -e "\033[32m mapping.json ok \033[0m";

  if [ $(ls | grep -c "^setting.json$") -ne 1 ]; then
    wget https://raw.githubusercontent.com/zpqsunny/dht/main/setting.json
  fi
  echo -e "\033[32m setting.json ok \033[0m";

}
initMongoDB() {
  if [ $(docker ps -a | grep -c "mongo") -eq 1 ]; then
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
  echo -e "\033[32m mongodb ok \033[0m";
}

openFirewalld() {

  firewall-cmd --add-port 6881/udp --permanent
  echo -e "\033[32m 6881/udp [OK] \033[0m"
  firewall-cmd --add-port 6882/udp --permanent
  echo -e "\033[32m 6883/udp [OK] \033[0m"
  firewall-cmd --add-port 6883/udp --permanent
  echo -e "\033[32m 6883/udp [OK] \033[0m"
  firewall-cmd --add-port 9200/tcp --permanent
  echo -e "\033[32m 9200/tcp [OK] \033[0m"
  firewall-cmd --add-port 9300/tcp --permanent
  echo -e "\033[32m 9300/tcp [OK] \033[0m"
  firewall-cmd --reload
  echo -e "\033[32m reload [OK] \033[0m"
}

while [ 1 -eq 1 ]; do
  echo '##########################################'
  echo '#### 0: 检查环境 Docker docker-compose  ###'
  echo '#### 1: 初始化   Elasticsearch          ###'
  echo '#### 2: 初始化   mongodb                ###'
  echo '#### 3: 防火墙开放端口                   ###'
  echo '#### x: 退出                           ###'
  read -r c
  case $c in
    0) checkSystem
      ;;
    1) initElasticsearch
      ;;
    2) initMongoDB
      ;;
    3) openFirewalld
      ;;
    x) break
      ;;
    *) continue
      echo "请正确输入"
      ;;
  esac
done






exit
# if have mongo backup
mongorestore --authenticationDatabase=admin -u admin -p xsad1sadsaa1 --port 27018 -d dht -c metadata /backup/dht/metadata.bson

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
curl -H "Content-Type: application/json" -X POST   -d @mapping.json -u elastic:xmow123mdsps http://80.66.196.105:9200/metadata/_mapping
# update setting
curl -H "Content-Type: application/json" -X PUT    -d @setting.json -u elastic:xmow123mdsps http://80.66.196.105:9200/metadata/_settings




#start other server
docker-compose up -d
