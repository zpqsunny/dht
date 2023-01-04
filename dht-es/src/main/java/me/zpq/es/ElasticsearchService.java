package me.zpq.es;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.HttpHost;
import org.bson.Document;
import org.bson.types.Binary;
import org.elasticsearch.client.RestClient;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

@Slf4j
public class ElasticsearchService {

    private final ElasticsearchClient elasticsearchClient;

    public ElasticsearchService(String elasticsearch, Integer port) {

        this.elasticsearchClient = buildElasticsearchClient(elasticsearch, port);
    }

    private Metadata transformation(Document document) {

        Metadata metadata = new Metadata();
        metadata.setId(document.getObjectId("_id").toHexString());
        metadata.setHash(DigestUtils.sha1Hex(document.get("hash", Binary.class).getData()));
        metadata.setName(document.getString("name"));
        metadata.setPieceLength(document.getInteger("pieceLength"));
        metadata.setCreatedDateTime(document.getDate("createdDateTime").getTime());
        metadata.setLength(document.getLong("length"));
        List<Metadata.File> files = document.get("files", new LinkedList<>());
        metadata.setFiles(files);
        metadata.setSize(document.getLong("size"));
        metadata.setFileNumber(document.getInteger("fileNumber"));
        return metadata;
    }

    private ElasticsearchClient buildElasticsearchClient(String hostname, int port) {
        // Create the low-level client
        RestClient restClient = RestClient.builder(new HttpHost(hostname, port)).build();
        // Create the transport with a Jackson mapper
        ElasticsearchTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
        // And create the API client
        return new ElasticsearchClient(transport);
    }

    public void push(Document fullDocument) throws IOException {

        ObjectMapper objectMapper = new ObjectMapper();
        Metadata metadata = this.transformation(fullDocument);
        log.info(objectMapper.writeValueAsString(metadata));
        IndexResponse response = elasticsearchClient.index(i -> i.index("metadata")
                .id(metadata.getId())
                .document(metadata));
        log.info("Indexed with version " + response.version());
    }
}
