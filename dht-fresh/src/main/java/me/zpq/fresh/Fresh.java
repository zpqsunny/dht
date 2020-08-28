package me.zpq.fresh;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import io.lettuce.core.api.sync.RedisCommands;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.bson.*;

import java.util.StringTokenizer;

/**
 * @author zpq
 * @date 2020/8/28
 */
@Slf4j
public class Fresh implements Runnable {

    private final static String FRESH_KEY = "fresh";

    private static final String DHT = "dht";

    private static final String FRESH = "fresh";

    private final RedisCommands<String, String> redis;

    private final MongoCollection<Document> document;

    public Fresh(RedisCommands<String, String> redis, MongoClient mongoClient) {
        this.redis = redis;
        this.document = mongoClient.getDatabase(DHT).getCollection(FRESH);
    }

    @Override
    public void run() {

        long freshLength = redis.llen(FRESH_KEY);
        log.info("redis fresh len: {}", freshLength);
        for (long i = 0; i < freshLength; i++) {

            String freshValue = redis.rpop(FRESH_KEY);
            // format hash|ip|port|timestamp
            StringTokenizer stringTokenizer = new StringTokenizer(freshValue, "|");
            String hashHex = stringTokenizer.nextToken();
            String ip = stringTokenizer.nextToken();
            int port = Integer.parseInt(stringTokenizer.nextToken());
            long time = Long.parseLong(stringTokenizer.nextToken());
            byte[] hash;
            try {
                hash = Hex.decodeHex(hashHex);
            } catch (DecoderException e) {
                // ignore
                log.error(e.getMessage());
                return;
            }
            Document d = new Document();
            d.put("hash", new BsonBinary(hash));
            d.put("ip", ip);
            d.put("port", new BsonInt32(port));
            d.put("time", new BsonDateTime(time));
            document.insertOne(d);
            log.info("index: {} OK", i);
        }

        log.info("finish");

    }
}
