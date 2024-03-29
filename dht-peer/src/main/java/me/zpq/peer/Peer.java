package me.zpq.peer;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import io.lettuce.core.api.sync.RedisCommands;
import io.netty.bootstrap.Bootstrap;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.bson.BsonBinary;
import org.bson.Document;

import java.nio.ByteBuffer;
import java.util.StringTokenizer;
import java.util.concurrent.ThreadPoolExecutor;

import static me.zpq.dht.common.RedisKeys.LIST_KEY;
import static me.zpq.dht.common.RedisKeys.SET_KEY;

/**
 * @author zpq
 * @date 2020/7/23
 */
@Slf4j
public class Peer implements Runnable {

    private static final String DHT = "dht";

    private static final String METADATA = "metadata";

    private static final String HASH = "hash";

    private final RedisCommands<String, String> redis;

    private final MongoCollection<Document> collection;

    private final ThreadPoolExecutor threadPoolExecutor;

    private final Bootstrap b;

    public Peer(RedisCommands<String, String> redis, MongoClient mongoClient, ThreadPoolExecutor threadPoolExecutor, Bootstrap b) {
        this.redis = redis;
        this.collection = mongoClient.getDatabase(DHT).getCollection(METADATA);
        this.threadPoolExecutor = threadPoolExecutor;
        this.b = b;
    }

    @Override
    public void run() {

        String infoHash = redis.rpop(LIST_KEY);
        if (infoHash != null) {
            log.info("redis peer len: {} threadPoolExecutor queue size: {}", redis.llen(LIST_KEY), threadPoolExecutor.getQueue().size());
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

                Document has = new Document();
                has.put(HASH, new BsonBinary(hash));
                if (collection.find(has).first() != null) {
                    log.info("hash is exist, ignore");
                    return;
                }

                b.handler(new Initializer(hash));
                try {
                    Object metadata = b.connect(ip, port).channel().closeFuture().sync().channel().attr(AttributeKey.valueOf("metadata")).get();
                    if (metadata instanceof ByteBuffer) {

                        if (collection.find(has).first() != null) {

                            log.info("hash is exist, ignore too");
                            return;
                        }
                        Document doc = MongoMetaInfo.saveLocalFile(((ByteBuffer) metadata).array());
                        collection.insertOne(doc);
                        log.info("metadata save success");
                    }
                } catch (Exception e) {
                    log.error("{} {}", e.getClass(), e);
                }
            });
        }
    }
}
