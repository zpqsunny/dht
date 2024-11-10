package me.zpq.fresh;

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

    private static int REDIS_DATABASE = 0;

    public static void main(String[] args) throws IOException {

        readConfig();

        RedisClient redisClient = redis();

        RedisCommands<String, String> redis = redisClient.connect().sync();

        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(5);

        scheduledExecutorService.scheduleWithFixedDelay(new Fresh(redis), 5L, 5L, TimeUnit.SECONDS);

        log.info("fresh started pid: {}", ManagementFactory.getRuntimeMXBean().getName());

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {

            log.info("closing...");
            scheduledExecutorService.shutdown();
            log.info("closed scheduled...");
            redisClient.shutdown();
            log.info("closed redis...");
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
            REDIS_DATABASE = Integer.parseInt(properties.getProperty("redis.database", Integer.toString(REDIS_DATABASE)));
            inputStream.close();
        }

        readEnv();

        log.info("=> redis.host: {}", REDIS_HOST);
        log.info("=> redis.port: {}", REDIS_PORT);
        log.info("=> redis.password: {}", REDIS_PASSWORD);
        log.info("=> redis.database: {}", REDIS_DATABASE);
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

    private static void readEnv() {
        // docker
        String redisHost = System.getenv("REDIS_HOST");
        String redisPort = System.getenv("REDIS_PORT");
        String redisPassword = System.getenv("REDIS_PASSWORD");
        String redisDatabase = System.getenv("REDIS_DATABASE");
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

}
