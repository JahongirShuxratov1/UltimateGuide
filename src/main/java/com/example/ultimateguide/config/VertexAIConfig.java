package com.example.ultimateguide.config;

import com.google.cloud.vertexai.api.LocationName;
import com.google.cloud.vertexai.VertexAI;
import com.google.cloud.vertexai.generativeai.preview.GenerativeModel;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.AccessToken;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.Collections;

@Configuration
public class VertexAIConfig {

    @Value("${vertex.ai.api-key}")
    private String apiKey;

    @Value("${vertex.ai.location}")
    private String location;

    @Value("${google.cloud.project-id}")
    private String projectId;

    @Bean
    public GenerativeModel generativeModel() throws IOException {
        GoogleCredentials credentials = GoogleCredentials.newBuilder()
            .setAccessToken(new AccessToken(apiKey, null))
            .build();
            
        VertexAI vertexAI = new VertexAI(projectId, location, credentials);
        return new GenerativeModel("gemini-pro", vertexAI);
    }
} 