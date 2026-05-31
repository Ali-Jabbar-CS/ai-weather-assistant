package com.ali.ai_weather_assistant.service;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class AIService {

    // Groq is OpenAI-compatible. This is the chat-completions endpoint.
    private static final String GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";

    // Current supported model (the old llama3-8192 names are deprecated).
    // If Groq ever retires this one, just change it here.
    private static final String MODEL = "llama-3.3-70b-versatile";

    // Reads groq.api.key from application.properties locally, OR the
    // GROQ_API_KEY environment variable on Railway (Spring maps both to this).
    // The ":" gives an empty default so the app still boots if the key is missing.
    @Value("${groq.api.key:}")
    private String groqApiKey;

    // Jackson is already on your classpath via spring-boot-starter-web.
    private final ObjectMapper mapper = new ObjectMapper();

    private RestTemplate createRestTemplate() {
        SimpleClientHttpRequestFactory rf = new SimpleClientHttpRequestFactory();
        rf.setConnectTimeout(10_000); // 10s
        rf.setReadTimeout(30_000);    // 30s
        return new RestTemplate(rf);
    }

    public String askAI(String prompt) {
        if (groqApiKey == null || groqApiKey.isBlank()) {
            return "AI ERROR: groq.api.key is not set. Add it to application.properties "
                    + "(or set the GROQ_API_KEY environment variable).";
        }
        if (prompt == null) prompt = "";

        RestTemplate restTemplate = createRestTemplate();

        // Build the request body with Jackson so any quotes/newlines in the
        // prompt are escaped correctly (no more manual escaping).
        String jsonBody;
        try {
            Map<String, Object> body = Map.of(
                    "model", MODEL,
                    "messages", List.of(
                            Map.of("role", "user", "content", prompt)
                    ),
                    "temperature", 0.7,   // optional: 0 = focused, 1 = creative. Tune or remove.
                    "max_tokens", 500      // optional: caps description length. Tune or remove.
            );
            jsonBody = mapper.writeValueAsString(body);
        } catch (Exception e) {
            return "AI ERROR: failed to build request JSON - " + e.getMessage();
        }

        // Debug prints (these go to the Spring Boot console)
        System.out.println("=== AIService.askAI starting (Groq) ===");
        System.out.println("Prompt length: " + prompt.length());
        System.out.println("Model: " + MODEL);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            headers.setBearerAuth(groqApiKey); // sends "Authorization: Bearer <key>"

            HttpEntity<String> request = new HttpEntity<>(jsonBody, headers);

            ResponseEntity<String> resp = restTemplate.exchange(
                    GROQ_URL, HttpMethod.POST, request, String.class);

            System.out.println("Groq response status: " + resp.getStatusCode());
            String respBody = resp.getBody();
            if (respBody == null) {
                return "AI ERROR: empty response from Groq";
            }

            // Pull out choices[0].message.content
            JsonNode root = mapper.readTree(respBody);
            JsonNode contentNode = root.path("choices").path(0).path("message").path("content");

            if (contentNode.isMissingNode() || contentNode.asText().isEmpty()) {
                System.out.println("Groq response had no content. Raw body (first 1000 chars):");
                System.out.println(respBody.length() > 1000 ? respBody.substring(0, 1000) : respBody);
                return "AI ERROR: no content in Groq response (see server console).";
            }

            String response = contentNode.asText().trim();

            // Strip common preamble phrases (same as the Ollama version)
            response = response.replaceAll("(?i)here's a friendly description of the weather:\\s*", "");
            response = response.replaceAll("(?i)here's a description of the weather:\\s*", "");
            response = response.replaceAll("(?i)here's the weather description:\\s*", "");
            return response.trim();

        } catch (HttpStatusCodeException e) {
            // Groq returned 4xx/5xx — the body usually says why
            // (401 = bad/missing key, 429 = rate limited, 400 = bad model name).
            System.out.println("Groq HTTP error " + e.getStatusCode() + ": " + e.getResponseBodyAsString());
            return "AI ERROR: Groq returned " + e.getStatusCode() + " - " + e.getResponseBodyAsString();
        } catch (Exception e) {
            System.out.println("Exception calling Groq: " + e.getClass().getName() + " - " + e.getMessage());
            e.printStackTrace();
            return "AI ERROR: Groq call failed - " + e.getMessage();
        }
    }
}