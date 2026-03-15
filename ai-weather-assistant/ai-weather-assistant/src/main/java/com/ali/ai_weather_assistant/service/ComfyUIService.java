package com.ali.ai_weather_assistant.service;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class ComfyUIService {

    // We'll pull these from application.properties so they're easy to change
    @Value("${comfyui.url}")
    private String comfyUiUrl;

    @Value("${comfyui.checkpoint}")
    private String checkpoint;

    private final RestTemplate restTemplate = new RestTemplate();

    // -------------------------------------------------------
    // Building the workflow JSON with our weather prompt
    // -------------------------------------------------------
    private String buildWorkflow(String prompt) {

        // This is the ComfyUI API-format workflow 
        // Each numbered key is a node. Nodes reference each other by ["nodeId", outputIndex]
        return """
            {
              "3": {
                "inputs": {
                  "seed": %d,
                  "steps": 30,
                  "cfg": 7.5,
                  "sampler_name": "dpmpp_2m",
                  "scheduler": "karras",
                  "denoise": 1.0,
                  "model": ["4", 0],
                  "positive": ["6", 0],
                  "negative": ["7", 0],
                  "latent_image": ["5", 0]
                },
                "class_type": "KSampler"
              },
              "4": {
                "inputs": {
                  "ckpt_name": "%s"
                },
                "class_type": "CheckpointLoaderSimple"
              },
              "5": {
                "inputs": {
                  "width": 1024,
                  "height": 1024,
                  "batch_size": 1
                },
                "class_type": "EmptyLatentImage"
              },
              "6": {
                "inputs": {
                  "text": "%s",
                  "clip": ["4", 1]
                },
                "class_type": "CLIPTextEncode"
              },
              "7": {
                "inputs": {
                  "text": "ugly, deformed, blurry, low quality, watermark, text, people, crowd",
                  "clip": ["4", 1]
                },
                "class_type": "CLIPTextEncode"
              },
              "8": {
                "inputs": {
                  "samples": ["3", 0],
                  "vae": ["4", 2]
                },
                "class_type": "VAEDecode"
              },
              "9": {
                "inputs": {
                  "filename_prefix": "weather",
                  "images": ["8", 0]
                },
                "class_type": "SaveImage"
              }
            }
            """.formatted(
                (long)(Math.random() * 999999999), // random seed each time
                checkpoint,
                escapeForJson(prompt)
            );
    }

    // --------------------------------------------------
    //  Submitting the workflow, and getting back a prompt_id
    // --------------------------------------------------
    private String submitPrompt(String workflowJson) {
        String url = comfyUiUrl + "/api/prompt";

        // ComfyUI expects: { "prompt": { ...workflow nodes... } }
        String body = "{\"prompt\": " + workflowJson + "}";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
        Map<String, Object> responseBody = response.getBody();

        if (responseBody == null || !responseBody.containsKey("prompt_id")) {
            throw new RuntimeException("ComfyUI did not return a prompt_id. Is it running on " + comfyUiUrl + "?");
        }

        return (String) responseBody.get("prompt_id");
    }

    // ----------------------------------------
    //  Poll /history until the image is ready
    // ----------------------------------------
    private String pollForImageFilename(String promptId) throws InterruptedException {
        String url = comfyUiUrl + "/api/history/" + promptId;

        // ComfyUI generates images asynchronously — we poll every 2 seconds
        for (int attempt = 0; attempt < 30; attempt++) {
            Thread.sleep(2000);

            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            Map<String, Object> history = response.getBody();

            if (history != null && history.containsKey(promptId)) {
                // Dig into the nested response: history -> promptId -> outputs -> "9" -> images -> [0] -> filename
                Map<String, Object> entry    = (Map<String, Object>) history.get(promptId);
                Map<String, Object> outputs  = (Map<String, Object>) entry.get("outputs");
                Map<String, Object> node9    = (Map<String, Object>) outputs.get("9");
                var images                   = (java.util.List<Map<String, Object>>) node9.get("images");
                return (String) images.get(0).get("filename");
            }

            System.out.println("Waiting for ComfyUI... attempt " + (attempt + 1));
        }

        throw new RuntimeException("ComfyUI timed out after 60 seconds");
    }

    // ------------------------------
    //  Fetch the actual image bytes
    // ------------------------------
    private byte[] fetchImageBytes(String filename) {
        String url = comfyUiUrl + "/api/view?filename=" + filename;
        return restTemplate.getForObject(url, byte[].class);
    }

    // -----------------------------------------
    // PUBLIC METHOD: the controller calls this
    // -----------------------------------------
    public byte[] generateWeatherImage(String weatherPrompt) throws InterruptedException {
        System.out.println("=== ComfyUIService: generating image ===");
        System.out.println("Prompt: " + weatherPrompt);

        String workflow  = buildWorkflow(weatherPrompt);
        String promptId  = submitPrompt(workflow);
        System.out.println("Prompt ID: " + promptId);

        String filename  = pollForImageFilename(promptId);
        System.out.println("Image ready: " + filename);

        return fetchImageBytes(filename);
    }

    // Minimal JSON escaping (same pattern as AIService)
    private String escapeForJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}