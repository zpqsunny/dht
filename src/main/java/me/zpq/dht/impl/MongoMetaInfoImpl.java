package me.zpq.dht.impl;

import be.adaxisoft.bencode.BDecoder;
import be.adaxisoft.bencode.BEncodedValue;
import com.mongodb.client.*;
import me.zpq.dht.MetaInfo;
import me.zpq.dht.util.Utils;
import org.bson.*;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

public class MongoMetaInfoImpl implements MetaInfo {

    private JedisPool jedisPool;

    private MongoCollection<Document> document;

    public MongoMetaInfoImpl(JedisPool jedisPool, String connectionString) {

        this.jedisPool = jedisPool;
        MongoClient mongoClient = MongoClients.create(connectionString);
        MongoDatabase database = mongoClient.getDatabase("dht");
        document = database.getCollection("meta_info");
    }

    @Override
    public void todoSomething(byte[] sha1, byte[] info) throws IOException {

        if (this.isExist(sha1)) {

            return;
        }
        BEncodedValue decode = BDecoder.decode(new ByteArrayInputStream(info));
        Document metaInfo = new Document();
        metaInfo.put("sha1", new BsonBinary(sha1));
        String name = decode.getMap().get("name").getString();
        if (decode.getMap().get("name.utf-8") != null) {

            // 存在uft-8扩展
            name = decode.getMap().get("name.utf-8").getString();
        }
        metaInfo.put("name", name);
        metaInfo.put("piece length", decode.getMap().get("piece length").getInt());
        metaInfo.put("created datetime", new BsonDateTime(System.currentTimeMillis()));
        if (decode.getMap().get("length") != null) {

            // single-file mode
            metaInfo.put("length", new BsonInt64(decode.getMap().get("length").getLong()));
        } else {

            // multi-file mode
            BsonArray bsonArray = new BsonArray();
            List<BEncodedValue> files = decode.getMap().get("files").getList();
            for (BEncodedValue file : files) {

                BsonDocument f = new BsonDocument();
                f.put("length", new BsonInt64(file.getMap().get("length").getLong()));
                BsonArray path = new BsonArray();
                List<BEncodedValue> paths = file.getMap().get("path").getList();
                if (file.getMap().get("path.utf-8") != null) {

                    // 存在uft-8扩展
                    paths = file.getMap().get("path.utf-8").getList();
                }
                for (BEncodedValue p : paths) {

                    path.add(new BsonString(p.getString()));
                }
                f.put("path", path);
                bsonArray.add(f);
            }

            metaInfo.put("files", bsonArray);
        }
        metaInfo.put("pieces", new BsonBinary(decode.getMap().get("pieces").getBytes()));
        document.insertOne(metaInfo);
    }

    @Override
    public void onAnnouncePeer(String host, Integer port, byte[] sha1) {

        if (this.isExist(sha1)) {

            return;
        }
        try (Jedis jedis = jedisPool.getResource()) {

            String infoHash = Utils.bytesToHex(sha1);

            jedis.sadd("meta_info", String.join(":", host, infoHash, String.valueOf(port)));
        }
    }

    private Boolean isExist(byte[] sha1) {

        Document has = new Document();
        has.put("sha1", new BsonBinary(sha1));
        FindIterable<Document> documents = document.find(has);
        Document first = documents.first();
        return first != null;
    }
}
