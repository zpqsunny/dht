docker-compose up -d mongo elasticsearch

# init mongo
docker exec -it mongo bash
mongo --host 127.0.0.1:27018 -u admin -p
rs.initiate()
exit
exit
# elasticsearch install plugin analysis
docker exec -it elasticsearch bash
elasticsearch-plugin install https://github.com/medcl/elasticsearch-analysis-ik/releases/download/v7.17.7/elasticsearch-analysis-ik-7.17.7.zip

exit
docker-compose restart elasticsearch
# drop index if exits
curl -H "Content-Type: application/json" -X DELETE -d '{}' -u elastic:elastic http://127.0.0.1:9200/metadata
# create index
curl -H "Content-Type: application/json" -X PUT -d '{}' -u elastic:elastic http://127.0.0.1:9200/metadata
# update mapping
curl -H "Content-Type: application/json" -X POST -d @mapping.json -u elastic:elastic http://127.0.0.1:9200/metadata/_mapping

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
