package me.zpq.database;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.*;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.resource.DefaultClientResources;
import lombok.extern.slf4j.Slf4j;
import org.bson.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public class MongoApplication {

    //redis
    private static String REDIS_HOST = "127.0.0.1";

    private static int REDIS_PORT = 6379;

    private static String REDIS_PASSWORD = "";

    private static int REDIS_DATABASE = 0;

    private static final String DATABASE = "dht";

    private static final String COLLECTION = "metadata";

    private static String MONGODB_URL = "mongodb://localhost";

    public static void main(String[] args) throws IOException {

        readConfig();
        RedisClient redis = redis();
        RedisCommands<String, String> redisCommands = redis.connect().sync();
        MongoClient mongoClient = mongo(MONGODB_URL);
        MongoCollection<Document> collection = mongoClient.getDatabase(DATABASE).getCollection(COLLECTION);
        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(20);
        scheduledExecutorService.schedule(new MongoDBTask(redisCommands, collection), 10, TimeUnit.SECONDS);
    }

    private static MongoClient mongo(String mongoUrl) {

        MongoClientSettings.Builder mongoClientSettings = MongoClientSettings.builder();
        ConnectionString connectionString = new ConnectionString(mongoUrl);
        mongoClientSettings.applyConnectionString(connectionString);
        mongoClientSettings.applyToSocketSettings(builder ->
                builder.connectTimeout(30, TimeUnit.SECONDS)
                        .readTimeout(1, TimeUnit.MINUTES));
        mongoClientSettings.applyToClusterSettings(builder ->
                builder.serverSelectionTimeout(1, TimeUnit.MINUTES));
        mongoClientSettings.applyToConnectionPoolSettings(builder ->
                builder.minSize(5).maxSize(10));
        return MongoClients.create(mongoClientSettings.build());
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
            REDIS_PORT = Integer.parseInt(properties.getProperty("redis.port", String.valueOf(REDIS_PORT)));
            REDIS_PASSWORD = properties.getProperty("redis.password", REDIS_PASSWORD);
            REDIS_DATABASE = Integer.parseInt(properties.getProperty("redis.database", String.valueOf(REDIS_DATABASE)));
            MONGODB_URL = properties.getProperty("mongodb.url", MONGODB_URL);
            inputStream.close();
        }

        readEnv();

        log.info("==========>");
        log.info("=> redis.host: {}", REDIS_HOST);
        log.info("=> redis.port: {}", REDIS_PORT);
        log.info("=> redis.password: {}", REDIS_PASSWORD);
        log.info("=> redis.database: {}", REDIS_DATABASE);
        log.info("=> mongodb.url: {}", MONGODB_URL);
    }

    private static void readEnv() {
        // docker
        String mongodbUrl = System.getenv("MONGODB_URL");
        String redisHost = System.getenv("REDIS_HOST");
        String redisPort = System.getenv("REDIS_PORT");
        String redisPassword = System.getenv("REDIS_PASSWORD");
        String redisDatabase = System.getenv("REDIS_DATABASE");
        if (mongodbUrl != null && !mongodbUrl.isEmpty()) {
            log.info("=> env MONGODB_URL: {}", mongodbUrl);
            MONGODB_URL = mongodbUrl;
        }
        if (redisHost != null && !redisHost.isEmpty()) {
            log.info("=> env REDIS_HOST: {}", redisHost);
            REDIS_HOST = redisHost;
        }
        if (redisPort != null && !redisPort.isEmpty()) {
            log.info("=> env REDIS_PORT: {}", redisPort);
            REDIS_PORT = Integer.parseInt(redisPort);
        }
        if (redisPassword != null && !redisPassword.isEmpty()) {
            log.info("=> env REDIS_PASSWORD: {}", redisPassword);
            REDIS_PASSWORD = redisPassword;
        }
        if (redisDatabase != null && !redisDatabase.isEmpty()) {
            log.info("=> env REDIS_DATABASE: {}", redisDatabase);
            REDIS_DATABASE = Integer.parseInt(redisDatabase);
        }
    }

    private static RedisClient redis() {

        DefaultClientResources.Builder resourceBuild = DefaultClientResources.builder();
        RedisURI.Builder builder = RedisURI.builder();
        builder.withHost(REDIS_HOST);
        builder.withPort(REDIS_PORT);
        builder.withPassword(REDIS_PASSWORD.toCharArray());
        builder.withDatabase(REDIS_DATABASE);
        return RedisClient.create(resourceBuild.build(), builder.build());
    }

}
