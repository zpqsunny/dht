package me.zpq.es;

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
public class EsApplication {

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
        ElasticsearchService elasticsearchService = new ElasticsearchService(ELASTIC, PORT, ELASTIC_USERNAME, ELASTIC_PASSWORD, collection);
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
            ELASTIC = properties.getProperty("es.host", ELASTIC);
            PORT = Integer.parseInt(properties.getProperty("es.port", PORT.toString()));
            ELASTIC_USERNAME = properties.getProperty("es.username", ELASTIC_USERNAME);
            ELASTIC_PASSWORD = properties.getProperty("es.password", ELASTIC_PASSWORD);
            inputStream.close();
        }

        readEnv();

        log.info("==========>");
        log.info("=> mongodb.url: {}", MONGODB_URL);
        log.info("=> es.host: {}", ELASTIC);
        log.info("=> es.port: {}", PORT);
        log.info("=> es.username: {}", ELASTIC_USERNAME);
        log.info("=> es.password: {}", ELASTIC_PASSWORD);
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

    }

}
