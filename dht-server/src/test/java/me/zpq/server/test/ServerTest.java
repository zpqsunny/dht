package me.zpq.server.test;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.resource.DefaultClientResources;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;

import java.util.StringTokenizer;

/**
 * @author zpq
 * @date 2020/7/23
 */
@Slf4j
public class ServerTest {

//    @Test
    public void test() {

        RedisCommands<String, String> redis = redis();
        redis.lpush("pp", "1");
        redis.lpush("pp", "2");
        redis.lpush("pp", "3");
        redis.lpush("pp", "4");

        log.info("{}", redis.rpop("pp"));
        log.info("{}", redis.rpop("pp"));
        log.info("{}", redis.rpop("pp"));
        log.info("{}", redis.rpop("pp"));
        log.info("{}",String.join("|","a","b","c"));
        StringTokenizer stringTokenizer = new StringTokenizer("1|2|3|4","|");
        log.info("{}",stringTokenizer.nextToken());

    }

    public RedisCommands<String, String> redis() {

        DefaultClientResources.Builder resourceBuild = DefaultClientResources.builder();
        RedisURI.Builder builder = RedisURI.builder();
        builder.withHost("127.0.0.1");
        builder.withPort(6379);
        RedisClient redisClient = RedisClient.create(resourceBuild.build(), builder.build());
        StatefulRedisConnection<String, String> connection = redisClient.connect();
        return connection.sync();
    }
}
