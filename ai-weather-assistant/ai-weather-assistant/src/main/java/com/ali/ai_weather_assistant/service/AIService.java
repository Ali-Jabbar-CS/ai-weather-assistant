package com.ali.ai_weather_assistant.service;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class AIService {

    // Try both to avoid localhost/127.0.0.1 networking issues
    private final List<String> OLLAMA_URLS = List.of(
            "http://127.0.0.1:11434/api/generate",
            "http://localhost:11434/api/generate"
    );

    private RestTemplate createRestTemplate() {
        SimpleClientHttpRequestFactory rf = new SimpleClientHttpRequestFactory();
        rf.setConnectTimeout(10_000); // 10s
        rf.setReadTimeout(30_000);    // 30s
        return new RestTemplate(rf);
    }

    public String askAI(String prompt) {
        RestTemplate restTemplate = createRestTemplate();

        String jsonBody = """
                {
                  "model": "llama3",
                  "prompt": "%s",
                  "stream": false
                }
                """.formatted(escapeForJson(prompt));

        // Debug prints (these go to the Spring Boot console)
        System.out.println("=== AIService.askAI starting ===");
        System.out.println("Prompt length: " + prompt.length());
        System.out.println("JSON body length: " + jsonBody.length());

        for (String url : OLLAMA_URLS) {
            try {
                System.out.println("Attempting Ollama URL: " + url);
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.setAccept(List.of(MediaType.APPLICATION_JSON));

                HttpEntity<String> request = new HttpEntity<>(jsonBody, headers);

                ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.POST, request, String.class);

                System.out.println("Ollama response status: " + resp.getStatusCode());
                String body = resp.getBody();
                if (body != null) {
                    System.out.println("Ollama response length: " + body.length());
                    System.out.println("Ollama response (first 1000 chars):");
                    System.out.println(body.length() > 1000 ? body.substring(0, 1000) : body);
                } else {
                    System.out.println("Ollama response body is NULL");
                }

               if (body == null) return "AI ERROR: empty response from Ollama";

int start = body.indexOf("\"response\":\"");
if (start != -1) {
    start += 12;
    // Walk forward until we find an unescaped closing quote
    StringBuilder result = new StringBuilder();
    int i = start;
    while (i < body.length()) {
        char c = body.charAt(i);
        if (c == '\\' && i + 1 < body.length()) {
            char next = body.charAt(i + 1);
            if (next == '"') { result.append('"'); i += 2; continue; }
            if (next == 'n') { result.append('\n'); i += 2; continue; }
            if (next == '\\') { result.append('\\'); i += 2; continue; }
        }
        if (c == '"') break; // unescaped quote = end of response
        result.append(c);
        i++;
    }
    return result.toString().trim();
}
return body;

                
            } catch (Exception e) {
                System.out.println("Exception calling Ollama at " + url + ": " + e.getClass().getName() + " - " + e.getMessage());
                StringWriter sw = new StringWriter();
                e.printStackTrace(new PrintWriter(sw));
                System.out.println(sw.toString());
                // try next URL
            }
        }

        return "AI ERROR: all Ollama attempts failed (see server console for stack traces).";
    }

    // minimal JSON-escape for prompt string
    private String escapeForJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }
}