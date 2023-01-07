package me.zpq.es;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.*;
import lombok.extern.slf4j.Slf4j;
import org.bson.*;
import org.bson.conversions.Bson;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

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


    public static void main(String[] args) throws IOException {

        readConfig();
        ElasticsearchService elasticsearchService = new ElasticsearchService(ELASTIC, PORT, ELASTIC_USERNAME, ELASTIC_PASSWORD);
        MongoClient mongoClient = mongo(MONGODB_URL);
        MongoCollection<Document> collection = mongoClient.getDatabase(DATABASE).getCollection(COLLECTION);
        List<Bson> pipeline = Collections.singletonList(match(eq("operationType", "insert")));
        collection.watch(pipeline).forEach(document -> {
            try {
                Document fullDocument = document.getFullDocument();
                if (fullDocument == null) {
                    return;
                }
                elasticsearchService.push(fullDocument);
            } catch (Exception e) {

                log.error(e.getMessage(), e);
            }
        });
    }

    private static MongoClient mongo(String mongoUrl) {

        MongoClientSettings.Builder mongoClientSettings = MongoClientSettings.builder();
        ConnectionString connectionString = new ConnectionString(mongoUrl);
        mongoClientSettings.applyConnectionString(connectionString);
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

        String elastic = System.getenv("ES.HOST");
        if (elastic != null && !elastic.isEmpty()) {
            log.info("=> env ES.HOST: {}", elastic);
            ELASTIC = elastic;
        }

        String elasticPort = System.getenv("ES.PORT");
        if (elasticPort != null && !elasticPort.isEmpty()) {
            log.info("=> env ES.PORT: {}", elasticPort);
            PORT = Integer.parseInt(elasticPort);
        }

        String elasticUsername = System.getenv("ES.USERNAME");
        if (elasticUsername != null && !elasticUsername.isEmpty()) {
            log.info("=> env ES.USERNAME: {}", elasticUsername);
            ELASTIC_USERNAME = elasticUsername;
        }

        String elasticPassword = System.getenv("ES.PASSWORD");
        if (elasticPassword != null && !elasticPassword.isEmpty()) {
            log.info("=> env ES.PASSWORD: {}", elasticPassword);
            ELASTIC_PASSWORD = elasticPassword;
        }
    }

}