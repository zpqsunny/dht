package me.zpq.database;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoCollection;
import io.lettuce.core.api.sync.RedisCommands;
import lombok.extern.slf4j.Slf4j;
import org.bson.BsonString;
import org.bson.Document;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

@Slf4j
public class MongoDBTask implements Runnable {

    private final RedisCommands<String, String> redisCommands;

    private final MongoCollection<Document> collection;

    public MongoDBTask(RedisCommands<String, String> redisCommands, MongoCollection<Document> collection) {
        this.redisCommands = redisCommands;
        this.collection = collection;
    }

    @Override
    public void run() {
        Set<String> hashSet = redisCommands.smembers("metadata");
        for (String hash : hashSet) {

            Document has = new Document();
            has.put("hash", new BsonString(hash));
            if (collection.find(has).first() != null) {
                log.info("hash is exist, ignore");
                redisCommands.del("hash:" + hash);
                redisCommands.srem("metadata", hash);
                continue;
            }
            Map<String, String> hashInfo = redisCommands.hgetall("hash:" + hash);
            if (hashInfo == null) {
                redisCommands.del("hash:" + hash);
                continue;
            }
            String date = hashInfo.get("date");
            String path = hashInfo.get("path");
            String source = hashInfo.get("source");
            String document = hashInfo.get("document");
            Path dir = Path.of("/metadata/" + date);
            Path paths = Path.of(path);
            if (!Files.exists(dir)) {
                try {
                    Files.createDirectories(dir);
                } catch (IOException e) {
                    log.error("create dir fail ", e);
                    return;
                }
            }
            try {
                BufferedWriter bufferedWriter = Files.newBufferedWriter(paths);
                bufferedWriter.write(source);
                bufferedWriter.close();
            } catch (IOException e) {
                log.error("write metadata local fail ", e);
                return;
            }
            ObjectMapper objectMapper = new ObjectMapper();
            try {
                Metadata metadata = objectMapper.readValue(document, Metadata.class);
                log.info(objectMapper.writeValueAsString(metadata));
                collection.insertOne(Document.parse(document));
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
