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

    private final MemoryQueue memoryQueue;

    private final BaseMetaInfo baseMetaInfo;

    private final ThreadPoolExecutor threadPoolExecutor;

    private final Bootstrap b;

    public Peer(MemoryQueue memoryQueue, BaseMetaInfo baseMetaInfo, ThreadPoolExecutor threadPoolExecutor, Bootstrap b) {
        this.memoryQueue = memoryQueue;
        this.baseMetaInfo = baseMetaInfo;
        this.threadPoolExecutor = threadPoolExecutor;
        this.b = b;
    }

    @Override
    public void run() {

        PeerNode peerNode = memoryQueue.rightPop();
        if (peerNode == null) {

            return;
        }
        log.info("queue peer len: {} threadPoolExecutor queue size: {}", memoryQueue.size(), threadPoolExecutor.getQueue().size());
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

            if (!baseMetaInfo.checkContinue(hash)) {
                return;
            }
            b.handler(new Initializer(hash));
            try {
                Object metadata = b.connect(ip, port).channel().closeFuture().sync().channel().attr(AttributeKey.valueOf("metadata")).get();
                if (metadata instanceof ByteBuffer) {
                    baseMetaInfo.run(((ByteBuffer) metadata).array());
                }
            } catch (Exception e) {
                log.error("get remote metadata fail ", e);
            }
        });
    }
}
