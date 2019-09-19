package me.zpq.dht.scheduled;

import me.zpq.dht.MetaInfo;
import me.zpq.dht.client.PeerClient;
import me.zpq.dht.exception.TryAgainException;
import me.zpq.dht.model.MetaInfoRequest;
import me.zpq.dht.util.Helper;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

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

        if (threadPoolExecutor.getActiveCount() >= threadPoolExecutor.getCorePoolSize()) {

            return;
        }

        try (Jedis jedis = jedisPool.getResource()) {

            Long len = jedis.llen("meta_info");
            LOGGER.info("redis len {}", len);
            String metaInfo = jedis.rpop("meta_info");
            if (metaInfo != null) {

                JSONObject jsonObject = new JSONObject(metaInfo);
                String ip = jsonObject.getString("ip");
                int p = jsonObject.getInt("port");
                byte[] infoHash = Helper.hexToByte(jsonObject.getString("infoHash"));
                threadPoolExecutor.execute(() -> {

                    PeerClient peerClient = new PeerClient(ip, p, peerId, infoHash, mongoMetaInfo);
                    try {

                        LOGGER.info("todo request peerClient ......");
                        peerClient.request();

                    } catch (TryAgainException e) {

                        LOGGER.warn("try to again. error:" + e.getMessage());
                        MetaInfoRequest metaInfoRequest = new MetaInfoRequest(ip, p, infoHash);
                        jedis.lpush("meta_info", metaInfoRequest.toString());
                    }
                });
            }
        } catch (Exception e) {

            LOGGER.error("peer error: " + e.getMessage());
        }
    }
}
