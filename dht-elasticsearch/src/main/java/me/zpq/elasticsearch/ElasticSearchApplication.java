package me.zpq.elasticsearch;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.*;
import lombok.extern.slf4j.Slf4j;
import org.bson.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
public class ElasticSearchApplication {

    private static final String DATABASE = "dht";

    private static final String COLLECTION = "metadata";

    private static String MONGODB_URL = "mongodb://localhost";

    private static String ELASTIC = "http://localhost";

    private static Integer PORT = 9200;

    private static String ELASTIC_USERNAME = "elastic";

    private static String ELASTIC_PASSWORD = "elastic";

    public static void main(String[] args) throws IOException {

        readConfig();
        MongoClient mongoClient = mongo(MONGODB_URL);
        MongoCollection<Document> collection = mongoClient.getDatabase(DATABASE).getCollection(COLLECTION);
        ElasticSearchService elasticsearchService = new ElasticSearchService(ELASTIC, PORT, ELASTIC_USERNAME, ELASTIC_PASSWORD, collection);
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.submit(elasticsearchService);
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
            MONGODB_URL = properties.getProperty("mongodb.url", MONGODB_URL);
            ELASTIC = properties.getProperty("elasticsearch.host", ELASTIC);
            PORT = Integer.parseInt(properties.getProperty("elasticsearch.port", PORT.toString()));
            ELASTIC_USERNAME = properties.getProperty("elasticsearch.username", ELASTIC_USERNAME);
            ELASTIC_PASSWORD = properties.getProperty("elasticsearch.password", ELASTIC_PASSWORD);
            inputStream.close();
        }

        readEnv();

        log.info("==========>");
        log.info("=> mongodb.url: {}", MONGODB_URL);
        log.info("=> elasticsearch.host: {}", ELASTIC);
        log.info("=> elasticsearch.port: {}", PORT);
        log.info("=> elasticsearch.username: {}", ELASTIC_USERNAME);
        log.info("=> elasticsearch.password: {}", ELASTIC_PASSWORD);
    }

    private static void readEnv() {
        // docker
        String mongodbUrl = System.getenv("MONGODB_URL");
        if (mongodbUrl != null && !mongodbUrl.isEmpty()) {
            log.info("=> env MONGODB_URL: {}", mongodbUrl);
            MONGODB_URL = mongodbUrl;
        }

        String elastic = System.getenv("ELASTICSEARCH_HOST");
        if (elastic != null && !elastic.isEmpty()) {
            log.info("=> env ELASTICSEARCH_HOST: {}", elastic);
            ELASTIC = elastic;
        }

        String elasticPort = System.getenv("ELASTICSEARCH_PORT");
        if (elasticPort != null && !elasticPort.isEmpty()) {
            log.info("=> env ELASTICSEARCH_PORT: {}", elasticPort);
            PORT = Integer.parseInt(elasticPort);
        }

        String elasticUsername = System.getenv("ELASTICSEARCH_USERNAME");
        if (elasticUsername != null && !elasticUsername.isEmpty()) {
            log.info("=> env ELASTICSEARCH_USERNAME: {}", elasticUsername);
            ELASTIC_USERNAME = elasticUsername;
        }

        String elasticPassword = System.getenv("ELASTICSEARCH_PASSWORD");
        if (elasticPassword != null && !elasticPassword.isEmpty()) {
            log.info("=> env ELASTICSEARCH_PASSWORD: {}", elasticPassword);
            ELASTIC_PASSWORD = elasticPassword;
        }

    }

}
