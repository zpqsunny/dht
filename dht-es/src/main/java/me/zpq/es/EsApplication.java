package me.zpq.es;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.*;
import com.mongodb.client.model.changestream.ChangeStreamDocument;
import lombok.extern.slf4j.Slf4j;
import org.bson.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

@Slf4j
public class EsApplication {

    private static final String DATABASE = "dht";

    private static final String COLLECTION = "metadata";

    private static String MONGODB_URL = "mongodb://localhost";

    private static String ELASTIC = "localhost";

    private static Integer PORT = 9200;

    public static void main(String[] args) throws IOException {

        readConfig();
        ElasticsearchService elasticsearchService = new ElasticsearchService(ELASTIC, PORT);
        MongoClient mongoClient = mongo(MONGODB_URL);
        MongoCollection<Document> collection = mongoClient.getDatabase(DATABASE).getCollection(COLLECTION);
        ChangeStreamIterable<Document> watch = collection.watch();
        for (ChangeStreamDocument<Document> document : watch) {

            try {
                Document fullDocument = document.getFullDocument();
                if (fullDocument == null) {
                    continue;
                }
                elasticsearchService.push(fullDocument);
            } catch (Exception e) {

                log.error(e.getMessage(), e);
            }
        }
    }

    private static MongoClient mongo(String mongoUrl) {

        assert mongoUrl != null;
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
            inputStream.close();
        }

        readEnv();

        log.info("==========>");
        log.info("=> mongodb.url: {}", MONGODB_URL);
        log.info("=> es.host: {}", ELASTIC);
        log.info("=> es.port: {}", PORT);
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
    }

}
