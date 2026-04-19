FROM openjdk:17
LABEL auther=zpq
WORKDIR /app
ENV TZ=Asia/Shanghai
ADD ./target/dht-database-*.jar ./dht-database.jar
ENTRYPOINT ["java", "-jar", "dht-database.jar"]

