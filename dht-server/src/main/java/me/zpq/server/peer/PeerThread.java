package me.zpq.server.peer;

import be.adaxisoft.bencode.BDecoder;
import be.adaxisoft.bencode.BEncodedValue;
import io.lettuce.core.api.sync.RedisCommands;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;
import me.zpq.dht.common.PeerNode;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author zpq
 * @date 2020/7/23
 */
@Slf4j
public class PeerThread implements Runnable {

    private final PeerNode peerNode;

    private final RedisCommands<String, String> redisCommands;

    private static final String HASH = "hash";

    private static final String NAME = "name";

    private static final String NAME_UTF8 = "name.utf-8";

    private static final String PIECE_LENGTH = "piece length";

    private static final String PIECE_LEN = "pieceLength";

    private static final String CREATED_DATETIME = "createdDateTime";

    private static final String FILES = "files";

    private static final String LENGTH = "length";

    private static final String PATH = "path";

    private static final String PATH_UTF8 = "path.utf-8";

    private static final String METADATA = "metadata";

    private static final String SIZE = "size";

    private static final String FILE_NUMBER = "fileNumber";

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    private static final String FILE_EXT = ".info";

    public PeerThread(PeerNode peerNode, RedisCommands<String, String> redisCommands) {
        this.peerNode = peerNode;
        this.redisCommands = redisCommands;
    }

    @Override
    public void run() {
        if (redisCommands.exists("hash:" + peerNode.hash()) > 0) {
            log.info("hash: {} exists ignore peer", peerNode.hash());
            return;
        }
        EventLoopGroup group = new NioEventLoopGroup(1);
        Bootstrap b = new Bootstrap();
        b.group(group)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.AUTO_READ, true)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
        ;
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
        b.handler(new Initializer(hash));
        try {
            log.info("try to connect peer {}:{} hash: {}", ip, port, peerNode.hash());
            Object metadata = b.connect(ip, port).channel().closeFuture().sync().channel().attr(AttributeKey.valueOf("metadata")).get();
            if (metadata instanceof ByteBuffer) {
                saveRedis(((ByteBuffer) metadata).array());
            }
        } catch (Exception e) {
            log.error("get remote metadata fail ", e);
        }
    }

    private void saveRedis(byte[] info) {

        try {
            byte[] sha1 = DigestUtils.sha1(info);
            String hex = Hex.encodeHexString(sha1);
            String date = LocalDate.now(ZoneId.of("GMT+8")).format(FORMATTER);
            String fileName = hex + FILE_EXT;

            Map<String, String> redisHashMap = new HashMap<>();
            redisHashMap.put(HASH, hex);
            redisHashMap.put("date", date);
            redisHashMap.put("timestamp", Long.toString(System.currentTimeMillis() / 1000));
            redisHashMap.put(PATH, "/" + METADATA + "/" + date + "/" + fileName);
            redisHashMap.put("source", new String(info));

            BEncodedValue decode = BDecoder.decode(new ByteArrayInputStream(info));
            Map<String, Object> metaInfo = new HashMap<>(6);
            long size = 0;
            int fileNumber = 1;
            metaInfo.put(HASH, hex);
            String name = decode.getMap().get(NAME).getString();
            if (decode.getMap().get(NAME_UTF8) != null) {

                // 存在uft-8扩展
                name = decode.getMap().get(NAME_UTF8).getString();
            }
            metaInfo.put(NAME, name);
            metaInfo.put(PIECE_LEN, decode.getMap().get(PIECE_LENGTH).getInt());
            metaInfo.put(CREATED_DATETIME, System.currentTimeMillis());
            if (decode.getMap().get(LENGTH) != null) {

                // single-file mode
                size = decode.getMap().get(LENGTH).getLong();
                metaInfo.put(LENGTH, size);
            } else {

                // multi-file mode
                JSONArray bsonArray = new JSONArray();
                List<BEncodedValue> files = decode.getMap().get(FILES).getList();
                fileNumber = files.size();
                for (BEncodedValue file : files) {

                    JSONObject f = new JSONObject();
                    size += file.getMap().get(LENGTH).getLong();
                    f.put(LENGTH, file.getMap().get(LENGTH).getLong());
                    JSONArray path = new JSONArray();
                    List<BEncodedValue> paths = file.getMap().get(PATH).getList();
                    if (file.getMap().get(PATH_UTF8) != null) {

                        // 存在uft-8扩展
                        paths = file.getMap().get(PATH_UTF8).getList();
                    }
                    for (BEncodedValue p : paths) {

                        path.put(p.getString());
                    }
                    f.put(PATH, path);
                    bsonArray.put(f);
                }
                metaInfo.put(FILES, bsonArray);
            }
            metaInfo.put(PATH, "/" + date + "/" + fileName);
            metaInfo.put(SIZE, size);
            metaInfo.put(FILE_NUMBER, fileNumber);
            JSONObject jsonObject = new JSONObject(metaInfo);
            redisHashMap.put("document", jsonObject.toString());
            redisCommands.hmset("hash:" + hex, redisHashMap);
            redisCommands.sadd("metadata", hex);
            log.info("save redis success {}", peerNode.hash());
        } catch (Exception e) {
            log.error("save meta (redis) error", e);
        }

    }
}
