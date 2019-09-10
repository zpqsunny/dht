package me.zpq.dht;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;
import redis.clients.jedis.Jedis;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;

/**
 * @author zpq
 * @date 2019-08-28
 */
public class Main {

    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws InterruptedException, IOException {

        ClassLoader classLoader = Main.class.getClassLoader();
        URL url = classLoader.getResource("config.yaml");
        if (url == null) {

            LOGGER.error("error config");
            return;
        }
        InputStream config = Files.newInputStream(Paths.get(url.getFile()));
        Yaml yaml = new Yaml();
        Map configMap = yaml.load(config);
        String host = (String) configMap.get("serverIp");
        Integer port = (Integer) configMap.get("serverPort");
        String peerId = (String) configMap.get("peerId");
        String transactionId = (String) configMap.get("transactionID");
        Integer minNodes = (Integer) configMap.get("minNodes");
        Integer maxNodes = (Integer) configMap.get("maxNodes");
        Integer timeout = (Integer) configMap.get("timeout");
        Jedis jedis = new Jedis("localhost", 6379);

        Bootstrap bootstrap = new Bootstrap();
        byte[] nodeId = Helper.nodeId();
        Map<String, NodeTable> table = new Hashtable<>();
        table.put(new String(nodeId), new NodeTable(Helper.bytesToHex(nodeId), host, port, System.currentTimeMillis()));
        bootstrap.group(new NioEventLoopGroup())
                .channel(NioDatagramChannel.class)
                .option(ChannelOption.SO_BROADCAST, true)
                .handler(new DiscardServerHandler(table, nodeId, maxNodes, jedis));
        final Channel channel = bootstrap.bind(host, port).sync().channel();

        LOGGER.info("start autoFindNode");
        Timer autoFindNode = new Timer();
        autoFindNode.schedule(new FindNode(channel, transactionId, nodeId, table, minNodes), 2000, 2000);
        LOGGER.info("start ok autoFindNode");
        LOGGER.info("start Ping");
        Timer autoPing = new Timer();
        autoPing.schedule(new Ping(channel, transactionId, nodeId, table), 5000, 20000);
        LOGGER.info("start ok Ping");
        Timer autoRemoveNode = new Timer();
        autoRemoveNode.schedule(new RemoveNode(table, timeout), 30000, 60000);
        LOGGER.info("start ok RemoveNode");
        LOGGER.info("server ok");

        Callable<Void> callable = () -> {
            String metaInfo = jedis.rpop("meta_info");
            if (metaInfo != null) {

                JSONObject jsonObject = new JSONObject(metaInfo);
                String ip = jsonObject.getString("ip");
                int p = jsonObject.getInt("port");
                byte[] infoHash = Helper.hexToByte(jsonObject.getString("infoHash"));
                PeerClient peerClient = new PeerClient(ip, p, peerId, infoHash);
                try {

                    peerClient.request();

                } catch (TryAgainException e) {

                    MetaInfoRequest metaInfoRequest = new MetaInfoRequest(ip, p, infoHash);
                    jedis.lpush("meta_info", metaInfoRequest.toString());
                }

            }
            return null;
        };
        while (true) {

            ExecutorService executorService = Executors.newSingleThreadExecutor();
            FutureTask<Void> voidFutureTask = new FutureTask<>(callable);
            executorService.execute(voidFutureTask);

            try {

                voidFutureTask.get(5, TimeUnit.MINUTES);

            } catch (InterruptedException e) {

                LOGGER.error("InterruptedException " + e.getMessage());
                voidFutureTask.cancel(true);

            } catch (ExecutionException e) {

                LOGGER.error("ExecutionException " + e.getMessage());
                voidFutureTask.cancel(true);

            } catch (TimeoutException e) {

                LOGGER.error("TimeoutException " + e.getMessage());
                voidFutureTask.cancel(true);
            } finally {
                executorService.shutdown();
                Thread.sleep(2000);
            }
        }
    }
}
