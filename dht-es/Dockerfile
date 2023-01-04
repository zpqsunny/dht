FROM openjdk:8-jre-alpine
LABEL auther=zpq
WORKDIR /app
ADD ./target/dht-es-*dependencies.jar ./dht-es.jar
ENTRYPOINT ["java", "-jar", "dht-es.jar"]

