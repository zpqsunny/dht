package me.zpq.es;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.*;
import com.mongodb.client.model.changestream.ChangeStreamDocument;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.resource.DefaultClientResources;
import lombok.extern.slf4j.Slf4j;
import org.bson.*;
import org.bson.conversions.Bson;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.mongodb.client.model.Aggregates.match;
import static com.mongodb.client.model.Filters.eq;

@Slf4j
public class EsApplication {

    private static final String DATABASE = "dht";

    private static final String COLLECTION = "metadata";

    private static String MONGODB_URL = "mongodb://localhost";

    private static String ELASTIC = "http://localhost";

    private static Integer PORT = 9200;

    private static String ELASTIC_USERNAME = "elastic";

    private static String ELASTIC_PASSWORD = "elastic";

    private static String REDIS_HOST = "127.0.0.1";

    private static int REDIS_PORT = 6379;

    private static String REDIS_PASSWORD = "";

    private static int REDIS_DATABASE = 0;

    public static void main(String[] args) throws IOException {

        readConfig();
        RedisCommands<String, String> redis = redis();
        ElasticsearchService elasticsearchService = new ElasticsearchService(ELASTIC, PORT, ELASTIC_USERNAME, ELASTIC_PASSWORD, redis);
        MongoClient mongoClient = mongo(MONGODB_URL);
        MongoCollection<Document> collection = mongoClient.getDatabase(DATABASE).getCollection(COLLECTION);
        List<Bson> pipeline = Collections.singletonList(match(eq("operationType", "insert")));
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(elasticsearchService);
        MongoCursor<ChangeStreamDocument<Document>> cursor;
        BsonDocument resumeToken = null;
        ObjectMapper objectMapper = new ObjectMapper();
        while (true) {
            try {
                if (resumeToken != null) {
                    cursor = collection.watch(pipeline).batchSize(100).resumeAfter(resumeToken).cursor();
                } else {
                    cursor = collection.watch(pipeline).batchSize(100).cursor();
                }
                while (cursor.hasNext()) {

                    ChangeStreamDocument<Document> next = cursor.next();
                    if (next.getFullDocument() != null) {

                        Metadata metadata = ElasticsearchService.transformation(next.getFullDocument());
                        redis.lpush("es", metadata.getId() + "@@@@!!!!" + objectMapper.writeValueAsString(metadata));
                    }
                    resumeToken = next.getResumeToken();
                }
            } catch (Exception e) {

                log.error("error: ", e);
            }
        }
    }

    private static MongoClient mongo(String mongoUrl) {

        MongoClientSettings.Builder mongoClientSettings = MongoClientSettings.builder();
        ConnectionString connectionString = new ConnectionString(mongoUrl);
        mongoClientSettings.applyConnectionString(connectionString);
        return MongoClients.create(mongoClientSettings.build());
    }

    private static RedisCommands<String, String> redis() {

        DefaultClientResources.Builder resourceBuild = DefaultClientResources.builder();
        RedisURI.Builder builder = RedisURI.builder();
        builder.withHost(REDIS_HOST);
        builder.withPort(REDIS_PORT);
        builder.withPassword(REDIS_PASSWORD.toCharArray());
        builder.withDatabase(REDIS_DATABASE);
        RedisClient redisClient = RedisClient.create(resourceBuild.build(), builder.build());
        StatefulRedisConnection<String, String> connection = redisClient.connect();
        return connection.sync();
    }

    private static void readConfig() throws IOException {

        String dir = System.getProperty("user.dir");
        Path configFile = Paths.get(dir + "/config.properties");
        if (Files.exists(configFile)) {

            log.info("=> read config...");
            InputStream inputStream = Files.newInputStream(configFile);
            Properties properties = new Properties();
            properties.load(inputStream);
            MONGODB_URL = properties.getProperty("mongodb.url", MONGODB_URL);
            ELASTIC = properties.getProperty("es.host", ELASTIC);
            PORT = Integer.parseInt(properties.getProperty("es.port", PORT.toString()));
            ELASTIC_USERNAME = properties.getProperty("es.username", ELASTIC_USERNAME);
            ELASTIC_PASSWORD = properties.getProperty("es.password", ELASTIC_PASSWORD);
            REDIS_HOST = properties.getProperty("redis.host", REDIS_HOST);
            REDIS_PORT = Integer.parseInt(properties.getProperty("redis.port", String.valueOf(REDIS_PORT)));
            REDIS_PASSWORD = properties.getProperty("redis.password", REDIS_PASSWORD);
            REDIS_DATABASE = Integer.parseInt(properties.getProperty("redis.database", String.valueOf(REDIS_DATABASE)));
            inputStream.close();
        }

        readEnv();

        log.info("==========>");
        log.info("=> mongodb.url: {}", MONGODB_URL);
        log.info("=> es.host: {}", ELASTIC);
        log.info("=> es.port: {}", PORT);
        log.info("=> es.username: {}", ELASTIC_USERNAME);
        log.info("=> es.password: {}", ELASTIC_PASSWORD);
        log.info("=> redis.host: {}", REDIS_HOST);
        log.info("=> redis.port: {}", REDIS_PORT);
        log.info("=> redis.password: {}", REDIS_PASSWORD);
        log.info("=> redis.database: {}", REDIS_DATABASE);
    }

    private static void readEnv() {
        // docker
        String mongodbUrl = System.getenv("MONGODB_URL");
        if (mongodbUrl != null && !mongodbUrl.isEmpty()) {
            log.info("=> env MONGODB_URL: {}", mongodbUrl);
            MONGODB_URL = mongodbUrl;
        }

        String elastic = System.getenv("ES_HOST");
        if (elastic != null && !elastic.isEmpty()) {
            log.info("=> env ES_HOST: {}", elastic);
            ELASTIC = elastic;
        }

        String elasticPort = System.getenv("ES_PORT");
        if (elasticPort != null && !elasticPort.isEmpty()) {
            log.info("=> env ES_PORT: {}", elasticPort);
            PORT = Integer.parseInt(elasticPort);
        }

        String elasticUsername = System.getenv("ES_USERNAME");
        if (elasticUsername != null && !elasticUsername.isEmpty()) {
            log.info("=> env ES_USERNAME: {}", elasticUsername);
            ELASTIC_USERNAME = elasticUsername;
        }

        String elasticPassword = System.getenv("ES_PASSWORD");
        if (elasticPassword != null && !elasticPassword.isEmpty()) {
            log.info("=> env ES_PASSWORD: {}", elasticPassword);
            ELASTIC_PASSWORD = elasticPassword;
        }
        String redisHost = System.getenv("REDIS_HOST");
        if (redisHost != null && !redisHost.isEmpty()) {
            log.info("=> env REDIS_HOST: {}", redisHost);
            REDIS_HOST = redisHost;
        }

        String redisPort = System.getenv("REDIS_PORT");
        if (redisPort != null && !redisPort.isEmpty()) {
            log.info("=> env REDIS_PORT: {}", redisPort);
            REDIS_PORT = Integer.parseInt(redisPort);
        }

        String redisPassword = System.getenv("REDIS_PASSWORD");
        if (redisPassword != null && !redisPassword.isEmpty()) {
            log.info("=> env REDIS_PASSWORD: {}", redisPassword);
            REDIS_PASSWORD = redisPassword;
        }

        String redisDatabase = System.getenv("REDIS_DATABASE");
        if (redisDatabase != null && !redisDatabase.isEmpty()) {
            log.info("=> env REDIS_DATABASE: {}", redisDatabase);
            REDIS_DATABASE = Integer.parseInt(redisDatabase);
        }
    }

}
