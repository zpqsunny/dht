FROM openjdk:8-jre-alpine
LABEL auther=zpq
WORKDIR /app
ADD ./target/dht-peer-1.0-SNAPSHOT-jar-with-dependencies.jar ./dht-peer.jar
ENTRYPOINT ["java", "-jar", "dht-peer.jar"]

