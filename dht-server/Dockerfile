FROM openjdk:8-jre-alpine
LABEL auther=zpq
WORKDIR /app
ENV TZ=Asia/Shanghai
EXPOSE 6881/udp
ADD ./target/dht-server-*dependencies.jar ./dht-server.jar
ENTRYPOINT ["java", "-jar", "dht-server.jar"]

