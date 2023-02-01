package me.zpq.es;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.lettuce.core.api.sync.RedisCommands;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.bson.Document;
import org.bson.types.Binary;
import org.elasticsearch.client.RestClient;

import java.io.IOException;
import java.io.StringReader;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.LinkedList;
import java.util.List;

@Slf4j
public class ElasticsearchService implements Runnable {

    private final ElasticsearchClient elasticsearchClient;

    private final RedisCommands<String, String> redis;

    public ElasticsearchService(String elasticsearch, Integer port, String username, String password, RedisCommands<String, String> redis) {

        this.elasticsearchClient = buildElasticsearchClient(elasticsearch, port, username, password);
        this.redis = redis;
    }

    public static Metadata transformation(Document document) {

        Metadata metadata = new Metadata();
        metadata.setId(document.getObjectId("_id").toHexString());
        metadata.setHash(Hex.encodeHexString(document.get("hash", Binary.class).getData()));
        metadata.setName(document.getString("name"));
        metadata.setPieceLength(document.getInteger("pieceLength"));
        metadata.setCreatedDateTime(LocalDateTime.ofInstant(document.getDate("createdDateTime").toInstant(), ZoneOffset.of("+8")));
        metadata.setLength(document.getLong("length"));
        List<Metadata.File> files = document.get("files", new LinkedList<>());
        metadata.setFiles(files.size() == 0 ? null : files);
        metadata.setSize(document.getLong("size"));
        metadata.setFileNumber(document.getInteger("fileNumber"));
        return metadata;
    }

    private ElasticsearchClient buildElasticsearchClient(String hostname, int port, String username, String password) {
        // Create the low-level client
        CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(username, password));
        RestClient restClient = RestClient.builder(HttpHost.create(String.format("%s:%s", hostname, port)))
                .setHttpClientConfigCallback(httpAsyncClientBuilder -> httpAsyncClientBuilder.setDefaultCredentialsProvider(credentialsProvider))
                .build();
        // Create the transport with a Jackson mapper
        ElasticsearchTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
        // And create the API client
        return new ElasticsearchClient(transport);
    }

    public void push(String id, String documentJson) throws IOException {

        IndexResponse response = elasticsearchClient.index(i -> i.index("metadata")
                .id(id)
                .withJson(new StringReader(documentJson)));
        log.info("Indexed with version " + response.version());
    }

    @Override
    public void run() {

        while (true) {
            try {
                String document = redis.lpop("es");
                if (document == null) {
                    Thread.sleep(2000L);
                    continue;
                }
                log.info("queue size: {} ", redis.llen("es"));
                String[] split = document.split("@@@@!!!!", 2);
                push(split[0], split[1]);
            } catch (InterruptedException | IOException e) {
                log.error(e.getMessage(), e);
            }
        }
    }
}
