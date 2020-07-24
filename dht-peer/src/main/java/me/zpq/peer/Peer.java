package me.zpq.peer;

import io.lettuce.core.api.sync.RedisCommands;
import lombok.extern.slf4j.Slf4j;
import me.zpq.dht.common.Utils;

import java.util.Random;
import java.util.StringTokenizer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author zpq
 * @date 2020/7/23
 */
@Slf4j
public class Peer implements Runnable {

    private final static String SET_KEY = "announce";

    private final static String LIST_KEY = "peer";

    private final RedisCommands<String, String> redis;

    private final ThreadPoolExecutor threadPoolExecutor;

    public Peer(RedisCommands<String, String> redis, ThreadPoolExecutor threadPoolExecutor) {

        this.redis = redis;
        this.threadPoolExecutor = threadPoolExecutor;
    }

    @Override
    public void run() {

        String infoHash = redis.rpop(LIST_KEY);
        if (infoHash != null) {

            StringTokenizer stringTokenizer = new StringTokenizer(infoHash, "|");
            String hashHex = stringTokenizer.nextToken();
            redis.srem(SET_KEY, hashHex);
            byte[] hash = Utils.hexToByte(hashHex);
            String ip = stringTokenizer.nextToken();
            int port = Integer.parseInt(stringTokenizer.nextToken());

            threadPoolExecutor.execute(() -> {

                PeerClient peerClient = new PeerClient(ip, port, hash);
                byte[] info = peerClient.run();
                if (info != null) {

                    try {
                        String show = JsonMetaInfo.show(info);
                        System.out.println(show);
                    } catch (Exception e) {
                        //ignore
                    }
                }
            });
        }

    }
}
