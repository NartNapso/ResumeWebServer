package com.nart.aicv.servers;

import com.nart.aicv.entities.Message;
import com.nart.aicv.exception.ValidationException;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Service
public class AIService {
    @Value("${openai.api.url}")
    private String apiUrl;

    @Value("${openai.api.key}")
    private String apiKey;

    private final List<Message> conversationHistory = new ArrayList<>();

    public AIService() {
        // Load system message from a file
        try {
            String systemMessage = Files.readString(Path.of("src/main/resources/system-context.txt")).trim();
            conversationHistory.add(new Message("system", systemMessage));
        } catch (Exception e) {
            throw new RuntimeException("Failed to load system context message from file", e);
        }
    }

    public String callAI(String userMessage) throws Exception {
        conversationHistory.add(new Message("user", userMessage)); // Add user message to history

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost request = new HttpPost(apiUrl);
            request.setHeader("Authorization", "Bearer " + apiKey);
            request.setHeader("Content-Type", "application/json");

            // Build the messages JSON array dynamically from the conversation history
            StringBuilder messagesJson = new StringBuilder("[");
            for (Message message : conversationHistory) {
                messagesJson.append(String.format("{\"role\": \"%s\", \"content\": \"%s\"},", message.getRole(), message.getContent()));
            }
            if (messagesJson.length() > 1) {
                messagesJson.setLength(messagesJson.length() - 1); // Remove trailing comma
            }
            messagesJson.append("]");

            String body = String.format("{\"model\": \"gpt-4\", \"messages\": %s}", messagesJson);
            request.setEntity(new StringEntity(body, StandardCharsets.UTF_8));

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                if (response.getCode() != 200) {
                    throw new RuntimeException("Failed to call AI API: " + response.getReasonPhrase());
                }
                BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
                StringBuilder result = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }

                // Parse and store the assistant's response
                String assistantReply = parseAssistantReply(result.toString());
                conversationHistory.add(new Message("assistant", assistantReply)); // Add assistant response to history

                return assistantReply;
            }
        }
    }

    private String parseAssistantReply(String jsonResponse) {
        // Simplified parsing of the assistant's reply from the JSON response
        int contentIndex = jsonResponse.indexOf("\"content\":");
        if (contentIndex != -1) {
            int startIndex = jsonResponse.indexOf("\"", contentIndex + 10) + 1;
            int endIndex = jsonResponse.indexOf("\"", startIndex);
            return jsonResponse.substring(startIndex, endIndex);
        }
        return "Error parsing response.";
    }

}
