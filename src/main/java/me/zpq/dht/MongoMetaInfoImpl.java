package me.zpq.dht;

import be.adaxisoft.bencode.BDecoder;
import be.adaxisoft.bencode.BEncodedValue;
import com.mongodb.client.*;
import org.bson.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

public class MongoMetaInfoImpl implements MetaInfo {

    private MongoClient mongoClient;

    public MongoMetaInfoImpl(String connectionString) {
        // mongodb+srv://<username>:<password>@<cluster-address>/test?w=majority
//        ConnectionString connString = new ConnectionString(connectionString);
//        MongoClientSettings settings = MongoClientSettings.builder()
//                .applyConnectionString(connString)
//                .retryWrites(true)
//                .build();
        mongoClient = MongoClients.create();
    }

    @Override
    public void todoSomething(byte[] sha1, byte[] info) throws IOException {

        MongoDatabase database = mongoClient.getDatabase("dht");
        MongoCollection<Document> document = database.getCollection("test");
        Document has = new Document();
        has.put("info_hash", new BsonBinary(sha1));
        FindIterable<Document> documents = document.find(has);
        Document first = documents.first();
        if (first == null) {

            BEncodedValue decode = BDecoder.decode(new ByteArrayInputStream(info));
            Document metaInfo = new Document();
            metaInfo.put("sha1", new BsonBinary(sha1));
            metaInfo.put("name", decode.getMap().get("name").getString());
            metaInfo.put("piece length", decode.getMap().get("piece length").getInt());
            if (decode.getMap().get("length") != null) {

                //single-file mode
                metaInfo.put("length", decode.getMap().get("length").getInt());
            } else {

                //multi-file mode
                BsonArray bsonArray = new BsonArray();
                List<BEncodedValue> files = decode.getMap().get("files").getList();
                for (BEncodedValue file : files) {

                    BsonDocument f = new BsonDocument();
                    f.put("length", new BsonInt64(file.getMap().get("length").getInt()));
                    BsonArray path = new BsonArray();
                    List<BEncodedValue> paths = file.getMap().get("path").getList();
                    for (BEncodedValue path1 : paths) {

                        path.add(new BsonString(path1.getString()));
                    }
                    f.put("path", path);
                    bsonArray.add(f);
                }

                metaInfo.put("files", bsonArray);
            }
            metaInfo.put("pieces", new BsonBinary(decode.getMap().get("pieces").getBytes()));
            document.insertOne(metaInfo);
        }
    }
}
