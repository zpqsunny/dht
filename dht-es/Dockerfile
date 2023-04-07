FROM openjdk:8-jre-alpine
LABEL auther=zpq
WORKDIR /app
ENV TZ=Asia/Shanghai
ADD ./target/dht-es-*dependencies.jar ./dht-es.jar
ENTRYPOINT ["java", "-jar", "dht-es.jar"]

