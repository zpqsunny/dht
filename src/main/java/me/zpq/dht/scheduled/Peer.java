package me.zpq.dht.scheduled;

import me.zpq.dht.MetaInfo;
import me.zpq.dht.client.PeerClient;
import me.zpq.dht.exception.TryAgainException;
import me.zpq.dht.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.Arrays;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author zpq
 * @date 2019-09-19
 */
public class Peer implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(Peer.class);

    private ThreadPoolExecutor threadPoolExecutor;

    private MetaInfo mongoMetaInfo;

    private JedisPool jedisPool;

    private String peerId;

    public Peer(ThreadPoolExecutor threadPoolExecutor, MetaInfo metaInfo, JedisPool jedisPool, String peerId) {
        this.threadPoolExecutor = threadPoolExecutor;
        this.mongoMetaInfo = metaInfo;
        this.jedisPool = jedisPool;
        this.peerId = peerId;
    }

    @Override
    public void run() {

        if (threadPoolExecutor.getActiveCount() >= threadPoolExecutor.getMaximumPoolSize()) {

            return;
        }

        try (Jedis jedis = jedisPool.getResource()) {

            Long len = jedis.scard("meta_info");
            LOGGER.info("redis len {}", len);
            String metaInfo = jedis.spop("meta_info");
            if (metaInfo == null) {

                return;
            }
            String[] info = metaInfo.split(":", 3);
            String ip = info[0];
            byte[] infoHash = Utils.hexToByte(info[1]);
            int port = Integer.parseInt(info[2]);
            LOGGER.info("ip {} port {} infoHash {}", ip, port, info[1]);
            threadPoolExecutor.execute(() -> {

                PeerClient peerClient = new PeerClient(ip, port, peerId, infoHash, mongoMetaInfo);
                try {

                    LOGGER.info("todo request peerClient ......");
                    peerClient.request();

                } catch (TryAgainException e) {

                    LOGGER.warn("try to again. error:" + e.getMessage());

                    jedis.sadd("meta_info", String.join(":", Arrays.asList(ip, Utils.bytesToHex(infoHash), String.valueOf(port))));
                }
            });
        } catch (Exception e) {

            LOGGER.error("peer error: " + e.getMessage());
        }
    }
}
