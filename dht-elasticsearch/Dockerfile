FROM openjdk:8-jre-alpine
LABEL auther=zpq
WORKDIR /app
ENV TZ=Asia/Shanghai
ADD ./target/dht-elasticsearch-*dependencies.jar ./dht-elasticsearch.jar
ENTRYPOINT ["java", "-jar", "dht-elasticsearch.jar"]

