package me.zpq.peer;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import io.lettuce.core.api.sync.RedisCommands;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.bson.BsonBinary;
import org.bson.Document;

import java.io.IOException;
import java.util.StringTokenizer;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author zpq
 * @date 2020/7/23
 */
@Slf4j
public class Peer implements Runnable {

    private final static String SET_KEY = "announce";

    private final static String LIST_KEY = "peer";

    private static final String DHT = "dht";

    private static final String METADATA = "metadata";

    private static final String HASH = "hash";

    private final RedisCommands<String, String> redis;

    private final MongoCollection<Document> document;

    private final ThreadPoolExecutor threadPoolExecutor;

    public Peer(RedisCommands<String, String> redis, MongoClient mongoClient, ThreadPoolExecutor threadPoolExecutor) {
        this.redis = redis;
        this.document = mongoClient.getDatabase(DHT).getCollection(METADATA);
        this.threadPoolExecutor = threadPoolExecutor;
    }

    @Override
    public void run() {

        String infoHash = redis.rpop(LIST_KEY);
        if (infoHash != null) {
            log.info("redis peer len: {}", redis.llen(LIST_KEY));
            StringTokenizer stringTokenizer = new StringTokenizer(infoHash, "|");
            String hashHex = stringTokenizer.nextToken();
            redis.srem(SET_KEY, hashHex);
            byte[] hash;
            try {
                hash = Hex.decodeHex(hashHex);
            } catch (DecoderException e) {
                // ignore
                log.error(e.getMessage());
                return;
            }
            String ip = stringTokenizer.nextToken();
            int port = Integer.parseInt(stringTokenizer.nextToken());

            threadPoolExecutor.execute(() -> {

                PeerClient peerClient = new PeerClient(ip, port, hash);
                byte[] info = peerClient.run();
                if (info != null) {

                    Document has = new Document();
                    has.put(HASH, new BsonBinary(hash));
                    FindIterable<Document> documents = document.find(has);
                    if (documents.first() != null) {
                        return;
                    }

                    try {

                        Document doc = MongoMetaInfo.saveLocalFile(info);
                        document.insertOne(doc);

                    } catch (IOException e) {
                        //
                        log.error(e.getMessage(), e);
                    }
                }
            });
        }
    }
}
