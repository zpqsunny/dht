FROM openjdk:8-jre-alpine
LABEL auther=zpq
WORKDIR /app
ENV TZ=Asia/Shanghai
ADD ./target/dht-peer*-dependencies.jar ./dht-peer.jar
ENTRYPOINT ["java", "-jar", "dht-peer.jar"]

