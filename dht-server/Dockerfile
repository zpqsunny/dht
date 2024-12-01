FROM openjdk:17
LABEL auther=zpq
WORKDIR /app
ENV TZ=Asia/Shanghai
EXPOSE 6881/udp
ADD ./target/dht-server-full-*.jar ./dht-server-full.jar
ENTRYPOINT ["java", "-jar", "dht-server-full.jar"]

