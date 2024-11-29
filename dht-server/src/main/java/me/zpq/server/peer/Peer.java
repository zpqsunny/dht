package me.zpq.server.peer;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import io.netty.bootstrap.Bootstrap;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;
import me.zpq.dht.common.MemoryQueue;
import me.zpq.dht.common.PeerNode;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.bson.BsonBinary;
import org.bson.Document;

import java.nio.ByteBuffer;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author zpq
 * @date 2020/7/23
 */
@Slf4j
public class Peer implements Runnable {

    private static final String DHT = "dht";

    private static final String METADATA = "metadata";

    private static final String HASH = "hash";

    private final MemoryQueue memoryQueue;

    private final MongoCollection<Document> collection;

    private final ThreadPoolExecutor threadPoolExecutor;

    private final Bootstrap b;

    public Peer(MemoryQueue memoryQueue, MongoClient mongoClient, ThreadPoolExecutor threadPoolExecutor, Bootstrap b) {
        this.memoryQueue = memoryQueue;
        this.collection = mongoClient.getDatabase(DHT).getCollection(METADATA);
        this.threadPoolExecutor = threadPoolExecutor;
        this.b = b;
    }

    @Override
    public void run() {

        PeerNode peerNode = memoryQueue.rightPop();
        if (peerNode == null) {

            return;
        }
        log.info("redis peer len: {} threadPoolExecutor queue size: {}", memoryQueue.size(), threadPoolExecutor.getQueue().size());
        byte[] hash;
        try {
            hash = Hex.decodeHex(peerNode.hash());
        } catch (DecoderException e) {
            // ignore
            log.error(e.getMessage());
            return;
        }
        String ip = peerNode.ip();
        int port = peerNode.port();
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
                log.error("get remote metadata fail ", e);
            }
        });
    }
}
