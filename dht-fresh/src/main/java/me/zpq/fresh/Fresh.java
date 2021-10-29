package me.zpq.fresh;

import io.lettuce.core.api.sync.RedisCommands;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.StringTokenizer;

import static me.zpq.dht.common.RedisKeys.FRESH_KEY;

/**
 * @author zpq
 * @date 2020/8/28
 */
@Slf4j
public class Fresh implements Runnable {

    private final RedisCommands<String, String> redis;

    public Fresh(RedisCommands<String, String> redis) {
        this.redis = redis;
    }

    @Override
    public void run() {

        long freshLength = redis.llen(FRESH_KEY);
        log.info("redis fresh len: {}", freshLength);
        for (long i = 0; i < freshLength; i++) {

            String freshValue = redis.rpop(FRESH_KEY);
            if (freshValue == null) {

                break;
            }
            // format hash|timestamp
            StringTokenizer stringTokenizer = new StringTokenizer(freshValue, "|");
            String hashHex = stringTokenizer.nextToken();
            String t = stringTokenizer.nextToken();
            Date d = new Date(Long.parseLong(t));
            LocalDateTime localDateTime = LocalDateTime.ofInstant(d.toInstant(), ZoneId.systemDefault())
                    .withHour(0).withMinute(0).withSecond(0).withNano(0);
            redis.zaddincr(hashHex, 1D, String.valueOf(localDateTime.toEpochSecond(ZoneOffset.of("+8"))));
            redis.expire(hashHex, 7L * 24 * 60 * 60);
        }

        log.info("finish");

    }
}
