package com.ravi.notesapp.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class AiService {

    private final HttpClient httpClient;
    private final Gson gson;

    public AiService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
        this.gson = new Gson();
    }

    public CompletableFuture<Void> askAiStream(String apiUrl, String model, String apiKey, String context,
            String userQuery, Consumer<String> onNextToken) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            return CompletableFuture
                    .failedFuture(new IllegalArgumentException("API Key is missing. Please configure it in settings."));
        }
        if (apiUrl == null || apiUrl.trim().isEmpty()) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("API URL is missing."));
        }

        String prompt = buildPrompt(context, userQuery);
        String jsonPayload = buildJsonPayload(model, prompt);

        String endpoint = apiUrl;
        if (!endpoint.endsWith("/")) {
            endpoint += "/";
        }
        if (!endpoint.endsWith("chat/completions")) {
            endpoint += "chat/completions";
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofLines())
                .thenAccept(response -> {
                    if (response.statusCode() != 200) {
                        StringBuilder errorBody = new StringBuilder();
                        response.body().forEach(line -> errorBody.append(line).append("\n"));
                        throw new RuntimeException("API Error (Code " + response.statusCode() + "): " + errorBody);
                    }
                    response.body().forEach(line -> {
                        if (line.startsWith("data: ") && !line.equals("data: [DONE]")) {
                            try {
                                String json = line.substring(6);
                                JsonObject rootObj = gson.fromJson(json, JsonObject.class);
                                JsonArray choices = rootObj.getAsJsonArray("choices");
                                if (choices != null && !choices.isEmpty()) {
                                    JsonObject delta = choices.get(0).getAsJsonObject().getAsJsonObject("delta");
                                    if (delta != null) {
                                        if (delta.has("reasoning") && !delta.get("reasoning").isJsonNull()) {
                                            String r = delta.get("reasoning").getAsString();
                                            if (r != null && !r.isEmpty()) {
                                                onNextToken.accept("[REASONING]" + r);
                                            }
                                        }
                                        if (delta.has("content") && !delta.get("content").isJsonNull()) {
                                            String token = delta.get("content").getAsString();
                                            if (token != null && !token.isEmpty()) {
                                                onNextToken.accept(token);
                                            }
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                // ignore malformed line
                            }
                        }
                    });
                });
    }

    private String buildPrompt(String context, String userQuery) {
        StringBuilder sb = new StringBuilder();
        if (context != null && !context.isBlank()) {
            sb.append("Context from the active file:\n")
                    .append("----------------------------\n")
                    .append(context).append("\n")
                    .append("----------------------------\n\n");
        }
        sb.append("User Query: ").append(userQuery);
        return sb.toString();
    }

    private String buildJsonPayload(String model, String prompt) {
        JsonObject textMessage = new JsonObject();
        textMessage.addProperty("role", "user");
        textMessage.addProperty("content", prompt);

        JsonArray messages = new JsonArray();
        messages.add(textMessage);

        JsonObject rootObj = new JsonObject();
        rootObj.addProperty("model", model);
        rootObj.addProperty("stream", true);
        rootObj.add("messages", messages);

        return gson.toJson(rootObj);
    }

    private String parseResponse(String responseBody) {
        try {
            JsonObject rootObj = gson.fromJson(responseBody, JsonObject.class);
            JsonArray choices = rootObj.getAsJsonArray("choices");
            if (choices != null && !choices.isEmpty()) {
                JsonObject firstChoice = choices.get(0).getAsJsonObject();
                JsonObject message = firstChoice.getAsJsonObject("message");
                if (message != null) {
                    return message.get("content").getAsString();
                }
            }
            return "Could not parse response from AI.";
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse JSON response: " + e.getMessage(), e);
        }
    }
}
