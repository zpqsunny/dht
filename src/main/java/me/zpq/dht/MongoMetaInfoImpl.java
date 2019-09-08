package me.zpq.dht;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.*;
import org.bson.Document;

public class MongoMetaInfoImpl implements MetaInfo {

    private MongoClient mongoClient;

    public MongoMetaInfoImpl(String connectionString) {
        // mongodb+srv://<username>:<password>@<cluster-address>/test?w=majority
        ConnectionString connString = new ConnectionString(connectionString);
        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(connString)
                .retryWrites(true)
                .build();
        mongoClient = MongoClients.create(settings);
    }

    @Override
    public void todoSomething(byte[] infoHash, byte[] metaInfo) {

        MongoDatabase database = mongoClient.getDatabase("dht");
        MongoCollection<Document> document = database.getCollection("meta_info");

    }
}
