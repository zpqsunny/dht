package me.zpq.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.changestream.ChangeStreamDocument;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.bson.BsonDocument;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.Binary;
import org.elasticsearch.client.RestClient;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static com.mongodb.client.model.Aggregates.match;
import static com.mongodb.client.model.Filters.eq;

@Slf4j
public class ElasticSearchService implements Runnable {

    private final ElasticsearchClient elasticsearchClient;

    private final MongoCollection<Document> collection;

    private final List<Bson> pipeline = Collections.singletonList(match(eq("operationType", "insert")));

    private BsonDocument resumeToken = null;

    public ElasticSearchService(String elasticsearch, Integer port, String username, String password,
                                MongoCollection<Document> collection) {

        this.elasticsearchClient = buildElasticsearchClient(elasticsearch, port, username, password);
        this.collection = collection;
    }

    public Metadata transformation(Document document) {

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

    public void push(Metadata metadata) throws IOException {

        IndexResponse response = elasticsearchClient.index(i -> i.index("metadata")
                .id(metadata.getId())
                .document(metadata));
        log.info("Metadata id: {} hash: {} version {}", metadata.getId(), metadata.getHash(), response.version());
    }

    @Override
    public void run() {

        while (true) {
            try {
                MongoCursor<ChangeStreamDocument<Document>> cursor;
                if (resumeToken != null) {
                    cursor = collection.watch(pipeline).resumeAfter(resumeToken).cursor();
                } else {
                    cursor = collection.watch(pipeline).cursor();
                }
                while (cursor.hasNext()) {

                    ChangeStreamDocument<Document> next = cursor.next();
                    if (next.getFullDocument() == null) {
                        continue;
                    }
                    Metadata metadata = this.transformation(next.getFullDocument());
                    this.push(metadata);
                    resumeToken = next.getResumeToken();
                }
            } catch (Exception e) {

                log.error("error: ", e);
            }
        }
    }
}
