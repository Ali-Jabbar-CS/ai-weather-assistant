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
public class ComfyUIService {

    @Value("${replicate.api.key:}")
    private String replicateApiKey;

    private static final String PREDICTIONS_URL = "https://api.replicate.com/v1/predictions";

    // official model — runs by owner/name, no version hash needed
    // flux-dev = higher quality than schnell (more steps, slower, costs a bit more)
    private static final String MODEL = "black-forest-labs/flux-dev";

    private final ObjectMapper mapper = new ObjectMapper();
    private final RestTemplate restTemplate = createRestTemplate();

    private static RestTemplate createRestTemplate() {
        SimpleClientHttpRequestFactory rf = new SimpleClientHttpRequestFactory();
        rf.setConnectTimeout(10_000);  // 10s
        rf.setReadTimeout(30_000);     // 30s
        return new RestTemplate(rf);
    }

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(replicateApiKey);
        return headers;
    }

    // -------------------------------------------------------
    // Build the request body: FLUX settings + our weather prompt
    // one image per prediction so each slideshow image uses its own prompt
    // -------------------------------------------------------
    private String buildRequestBody(String prompt) throws Exception {
        Map<String, Object> input = Map.ofEntries(
                Map.entry("prompt",
                        "Cinematic photorealistic photograph. " + prompt
                        + " Dramatic atmospheric lighting, ultra-detailed, sharp focus, "
                        + "high dynamic range, professional color grading. No people. No text."),
                Map.entry("aspect_ratio", "1:1"),
                Map.entry("megapixels", "1"),
                Map.entry("num_outputs", 1),
                Map.entry("num_inference_steps", 30),
                Map.entry("guidance_scale", 3.0),
                Map.entry("output_format", "png"),
                Map.entry("seed", (int)(Math.random() * 999999999))
        );

        Map<String, Object> body = Map.of(
                "version", MODEL,
                "input", input
        );

        return mapper.writeValueAsString(body);
    }

    private String submitPrediction(String prompt) throws Exception {
        String body = buildRequestBody(prompt);
        HttpEntity<String> request = new HttpEntity<>(body, authHeaders());

        ResponseEntity<String> response =
                restTemplate.exchange(PREDICTIONS_URL, HttpMethod.POST, request, String.class);

        JsonNode root = mapper.readTree(response.getBody());
        String id = root.path("id").asText(null);

        if (id == null || id.isEmpty()) {
            throw new RuntimeException("Replicate did not return a prediction id. Body: " + response.getBody());
        }

        return id;
    }

    // poll one prediction until succeeded, return its image URL
    private String pollForImageUrl(String predictionId) throws InterruptedException {
        String url = PREDICTIONS_URL + "/" + predictionId;
        HttpEntity<Void> request = new HttpEntity<>(authHeaders());

        for (int attempt = 0; attempt < 60; attempt++) {   // up to 120s
            Thread.sleep(2000);

            ResponseEntity<String> response =
                    restTemplate.exchange(url, HttpMethod.GET, request, String.class);

            JsonNode root;
            try {
                root = mapper.readTree(response.getBody());
            } catch (Exception e) {
                System.out.println("Could not parse poll response: " + e.getMessage());
                continue;
            }

            String status = root.path("status").asText();

            if ("succeeded".equals(status)) {
                return root.path("output").path(0).asText();
            }
            if ("failed".equals(status) || "canceled".equals(status)) {
                throw new RuntimeException("Replicate prediction " + status + ": " + root.path("error").asText());
            }

            System.out.println("Waiting for Replicate... attempt " + (attempt + 1) + " (status: " + status + ")");
        }

        throw new RuntimeException("Replicate timed out after 120 seconds");
    }

    private byte[] fetchImageBytes(String imageUrl) {
        return restTemplate.getForObject(imageUrl, byte[].class);
    }

    // one prompt -> one finished image (submit, poll, download)
    private byte[] generateOne(String prompt) throws InterruptedException {
        String id;
        try {
            id = submitPrediction(prompt);
        } catch (HttpStatusCodeException e) {
            throw new RuntimeException("Replicate returned " + e.getStatusCode()
                    + " - " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to submit Replicate prediction: " + e.getMessage(), e);
        }
        System.out.println("Prediction ID: " + id);
        return fetchImageBytes(pollForImageUrl(id));
    }

    // -----------------------------------------
    // PUBLIC METHOD: the controller calls this — two prompts, two images
    // generated one after the other so we stay under Replicate's burst limit
    // -----------------------------------------
    public List<byte[]> generateWeatherImages(String prompt1, String prompt2) throws InterruptedException {
        System.out.println("=== ComfyUIService: generating 2 images (Replicate FLUX dev) ===");

        if (replicateApiKey == null || replicateApiKey.isBlank()) {
            throw new RuntimeException("replicate.api.key is not set. Add it to application.properties "
                    + "(or set the REPLICATE_API_TOKEN environment variable).");
        }

        byte[] img1 = generateOne(prompt1);
        byte[] img2 = generateOne(prompt2);
        return List.of(img1, img2);
    }
}