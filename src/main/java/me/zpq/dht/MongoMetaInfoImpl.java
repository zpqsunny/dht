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
    public void todoSomething(byte[] infoHash, byte[] metaInfo) throws IOException {

        MongoDatabase database = mongoClient.getDatabase("dht");
        MongoCollection<Document> document = database.getCollection("meta_info");
        Document has = new Document();
        has.put("info_hash", new BsonBinary(infoHash));
        FindIterable<Document> documents = document.find(has);
        Document first = documents.first();
        if (first == null) {

            BEncodedValue decode = BDecoder.decode(new ByteArrayInputStream(metaInfo));
            Document info = new Document();
            info.put("info_hash", new BsonBinary(infoHash));
            info.put("name", decode.getMap().get("name").getString());
            info.put("piece length", decode.getMap().get("piece length").getInt());
            if (decode.getMap().get("length") != null) {

                //single-file mode
                info.put("length", decode.getMap().get("length").getInt());
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

                info.put("files", bsonArray);
            }
            info.put("pieces", new BsonBinary(decode.getMap().get("pieces").getBytes()));
            document.insertOne(info);
        }

    }
}
