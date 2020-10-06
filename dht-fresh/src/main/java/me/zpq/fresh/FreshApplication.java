package me.zpq.fresh;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.resource.DefaultClientResources;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.concurrent.*;

/**
 * @author zpq
 * @date 2020/8/28
 */
@Slf4j
public class FreshApplication {

    private static String REDIS_HOST = "127.0.0.1";

    private static int REDIS_PORT = 6379;

    private static String REDIS_PASSWORD = "";

    private static String MONGODB_URL = "mongodb://localhost";

    public static void main(String[] args) throws IOException {

        readConfig();

        RedisClient redisClient = redis();

        RedisCommands<String, String> redis = redisClient.connect().sync();

        MongoClient mongoClient = mongo(MONGODB_URL);

        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(5);

        scheduledExecutorService.scheduleWithFixedDelay(new Fresh(redis, mongoClient), 5L, 5L, TimeUnit.SECONDS);

        log.info("fresh started pid: {}", ManagementFactory.getRuntimeMXBean().getName());

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {

            log.info("closing...");
            scheduledExecutorService.shutdown();
            log.info("closed scheduled...");
            redisClient.shutdown();
            log.info("closed redis...");
            mongoClient.close();
            log.info("closed mongodb...");
        }));
    }

    private static void readConfig() throws IOException {

        String dir = System.getProperty("user.dir");
        Path configFile = Paths.get(dir + "/config.properties");
        if (Files.exists(configFile)) {

            log.info("=> read config...");
            InputStream inputStream = Files.newInputStream(configFile);
            Properties properties = new Properties();
            properties.load(inputStream);
            REDIS_HOST = properties.getProperty("redis.host", REDIS_HOST);
            REDIS_PORT = Integer.parseInt(properties.getProperty("redis.port", Integer.toString(REDIS_PORT)));
            REDIS_PASSWORD = properties.getProperty("redis.password", REDIS_PASSWORD);
            MONGODB_URL = properties.getProperty("mongodb.url", MONGODB_URL);
            inputStream.close();
        }

        log.info("=> redis.host: {}", REDIS_HOST);
        log.info("=> redis.port: {}", REDIS_PORT);
        log.info("=> redis.password: {}", REDIS_PASSWORD);
        log.info("=> mongodb.url: {}", MONGODB_URL);
    }

    private static RedisClient redis() {

        DefaultClientResources.Builder resourceBuild = DefaultClientResources.builder();
        RedisURI.Builder builder = RedisURI.builder();
        builder.withHost(REDIS_HOST);
        builder.withPort(REDIS_PORT);
        builder.withPassword(REDIS_PASSWORD);
        return RedisClient.create(resourceBuild.build(), builder.build());
    }

    private static MongoClient mongo(String mongoUri) {

        MongoClientSettings.Builder mongoClientSettings = MongoClientSettings.builder();
        ConnectionString connectionString = new ConnectionString(mongoUri);
        mongoClientSettings.applyConnectionString(connectionString);
        return MongoClients.create(mongoClientSettings.build());
    }
}
