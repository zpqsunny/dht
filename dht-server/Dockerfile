FROM openjdk:8-jre-alpine
LABEL auther=zpq
WORKDIR /app
EXPOSE 6881/udp
ADD ./target/dht-server-1.0-SNAPSHOT-jar-with-dependencies.jar ./dht-server.jar
ENTRYPOINT ["java", "-jar", "dht-server.jar"]

