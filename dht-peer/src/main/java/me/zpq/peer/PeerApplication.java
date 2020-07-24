package me.zpq.peer;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.resource.DefaultClientResources;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.concurrent.*;

/**
 * @author zpq
 * @date 2020/7/23
 */
@Slf4j
public class PeerApplication {

    private static int CORE_POOL_SIZE = 5;

    private static int MAX_POOL_SIZE = 10;

    private static String REDIS_HOST = "127.0.0.1";

    private static int REDIS_PORT = 6379;

    private static String REDIS_PASSWORD = "";

    private static String MONGODB_URL = "mongodb://localhost";

    public static void main(String[] args) throws IOException {

        readConfig();

        RedisCommands<String, String> redis = redis();

        MongoClient mongoClient = mongo(MONGODB_URL);

        ThreadFactory threadFactory = Executors.defaultThreadFactory();
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(CORE_POOL_SIZE, MAX_POOL_SIZE,
                0L, TimeUnit.MINUTES, new LinkedBlockingQueue<>(), threadFactory);

        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(5);

        scheduledExecutorService.scheduleWithFixedDelay(new Peer(redis, mongoClient, threadPoolExecutor), 2L, 2L, TimeUnit.SECONDS);

        log.info("peer start");
    }

    private static void readConfig() throws IOException {

        String dir = System.getProperty("user.dir");
        Path configFile = Paths.get(dir + "/config.properties");
        if (Files.exists(configFile)) {

            log.info("=> read config...");
            InputStream inputStream = Files.newInputStream(configFile);
            Properties properties = new Properties();
            properties.load(inputStream);
            CORE_POOL_SIZE = Integer.parseInt(properties.getProperty("peers.core.pool.size", String.valueOf(CORE_POOL_SIZE)));
            MAX_POOL_SIZE = Integer.parseInt(properties.getProperty("peers.maximum.pool.size", String.valueOf(MAX_POOL_SIZE)));
            REDIS_HOST = properties.getProperty("redis.host", REDIS_HOST);
            REDIS_PORT = Integer.parseInt(properties.getProperty("redis.port", String.valueOf(REDIS_PORT)));
            REDIS_PASSWORD = properties.getProperty("redis.password", REDIS_PASSWORD);
            MONGODB_URL = properties.getProperty("mongodb.url", MONGODB_URL);
            inputStream.close();
        }

        log.info("=> server.peers.core.pool.size: {}", CORE_POOL_SIZE);
        log.info("=> server.peers.maximum.pool.size: {}", MAX_POOL_SIZE);
        log.info("=> redis.host: {}", REDIS_HOST);
        log.info("=> redis.port: {}", REDIS_PORT);
        log.info("=> redis.password: {}", REDIS_PASSWORD);
        log.info("=> mongodb.url: {}", MONGODB_URL);
    }

    private static RedisCommands<String, String> redis() {

        DefaultClientResources.Builder resourceBuild = DefaultClientResources.builder();
        RedisURI.Builder builder = RedisURI.builder();
        builder.withHost(REDIS_HOST);
        builder.withPort(REDIS_PORT);
        builder.withPassword(REDIS_PASSWORD);
        RedisClient redisClient = RedisClient.create(resourceBuild.build(), builder.build());
        StatefulRedisConnection<String, String> connection = redisClient.connect();
        return connection.sync();
    }

    private static MongoClient mongo(String mongoUri) {

        MongoClientSettings.Builder mongoClientSettings = MongoClientSettings.builder();
        ConnectionString connectionString = new ConnectionString(mongoUri);
        mongoClientSettings.applyConnectionString(connectionString);
        return MongoClients.create(mongoClientSettings.build());
    }
}
