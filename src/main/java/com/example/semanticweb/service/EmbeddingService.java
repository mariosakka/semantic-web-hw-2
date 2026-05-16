package com.example.semanticweb.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class EmbeddingService {

    private final RestClient openAiRestClient;
    private final ObjectMapper objectMapper;
    private final String embeddingModel;
    private final String openAiApiKey;

    public EmbeddingService(
            RestClient openAiRestClient,
            ObjectMapper objectMapper,
            @Value("${openai.embedding.model:text-embedding-3-small}") String embeddingModel,
            @Value("${openai.api.key:}") String openAiApiKey
    ) {
        this.openAiRestClient = openAiRestClient;
        this.objectMapper = objectMapper;
        this.embeddingModel = embeddingModel;
        this.openAiApiKey = openAiApiKey;
    }

    public boolean isEnabled() {
        return openAiApiKey != null && !openAiApiKey.isBlank() && !"replace_me".equalsIgnoreCase(openAiApiKey.trim());
    }

    public List<Float> createEmbedding(String text) {
        if (!isEnabled() || text == null || text.isBlank()) {
            return List.of();
        }

        Map<String, Object> request = new HashMap<>();
        request.put("model", embeddingModel);
        request.put("input", text);

        try {
            String response = openAiRestClient.post()
                    .uri("/embeddings")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(String.class);

            if (response == null || response.isBlank()) {
                return List.of();
            }

            JsonNode root = objectMapper.readTree(response);
            JsonNode dataArray = root.path("data");
            if (!dataArray.isArray() || dataArray.isEmpty()) {
                return List.of();
            }

            JsonNode embedding = dataArray.get(0).path("embedding");
            if (!embedding.isArray()) {
                return List.of();
            }

            List<Float> vector = new ArrayList<>();
            for (JsonNode value : embedding) {
                vector.add(value.floatValue());
            }
            return vector;
        } catch (Exception e) {
            return List.of();
        }
    }
}
