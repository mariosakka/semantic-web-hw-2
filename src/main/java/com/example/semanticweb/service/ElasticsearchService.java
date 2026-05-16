package com.example.semanticweb.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.example.semanticweb.model.BookChunk;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

@Service
public class ElasticsearchService {

    private final ElasticsearchClient elasticsearchClient;
    private final String indexName;

    public ElasticsearchService(
            ElasticsearchClient elasticsearchClient,
            @Value("${elasticsearch.index:book_chunks}") String indexName
    ) {
        this.elasticsearchClient = elasticsearchClient;
        this.indexName = indexName;
    }

    public void ensureIndex() {
        try {
            boolean exists = elasticsearchClient.indices().exists(e -> e.index(indexName)).value();
            if (exists) {
                return;
            }

            String mapping = """
                    {
                      "settings": {
                        "number_of_shards": 1,
                        "number_of_replicas": 0
                      },
                      "mappings": {
                        "properties": {
                          "book": {
                            "properties": {
                              "id": { "type": "keyword" },
                              "title": { "type": "text" },
                              "author": { "type": "text" },
                              "themes": { "type": "keyword" },
                              "readingLevel": { "type": "keyword" }
                            }
                          },
                          "text": { "type": "text" },
                          "embedding": {
                            "type": "dense_vector",
                            "dims": 1536,
                            "index": true,
                            "similarity": "cosine"
                          }
                        }
                      }
                    }
                    """;

            elasticsearchClient.indices().create(c -> c.index(indexName).withJson(new StringReader(mapping)));
        } catch (Exception ignored) {
        }
    }

    public void indexChunk(BookChunk chunk) {
        if (chunk == null || chunk.getBook() == null || chunk.getBook().getId() == null || chunk.getBook().getId().isBlank()) {
            return;
        }

        try {
            elasticsearchClient.index(i -> i
                    .index(indexName)
                    .id(chunk.getBook().getId())
                    .document(chunk));
        } catch (Exception ignored) {
        }
    }

    public List<BookChunk> searchByVector(List<Float> queryVector, int topK) {
        if (queryVector == null || queryVector.isEmpty() || topK <= 0) {
            return List.of();
        }

        try {
            SearchResponse<BookChunk> response = elasticsearchClient.search(s -> s
                            .index(indexName)
                            .size(topK)
                            .knn(k -> k
                                    .field("embedding")
                                    .queryVector(queryVector)
                                    .k(topK)
                                    .numCandidates(Math.max(topK * 4, 20))),
                    BookChunk.class);

            List<BookChunk> chunks = new ArrayList<>();
            for (Hit<BookChunk> hit : response.hits().hits()) {
                if (hit.source() != null) {
                    chunks.add(hit.source());
                }
            }
            return chunks;
        } catch (Exception e) {
            return List.of();
        }
    }
}
