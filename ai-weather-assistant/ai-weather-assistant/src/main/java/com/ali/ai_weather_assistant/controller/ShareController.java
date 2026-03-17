package com.ali.ai_weather_assistant.controller;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.ali.ai_weather_assistant.model.SharedResult;
import com.ali.ai_weather_assistant.repository.ShareRepository;

@RestController
public class ShareController {

    private final ShareRepository shareRepository;

    public ShareController(ShareRepository shareRepository) {
        this.shareRepository = shareRepository;
    }

    @PostMapping("/api/share")
    public ResponseEntity<Map<String, String>> saveShare(
            @RequestBody Map<String, Object> body) {

        String id = UUID.randomUUID().toString().substring(0, 8);

        SharedResult result = new SharedResult(
            id,
            (String) body.get("city"),
            (String) body.get("country"),
            ((Number) body.get("tempF")).doubleValue(),
            ((Number) body.get("feelsLikeF")).doubleValue(),
            ((Number) body.get("humidity")).intValue(),
            ((Number) body.get("windSpeed")).doubleValue(),
            (String) body.get("condition"),
            (String) body.get("timeOfDay"),
            (String) body.get("season"),
            (String) body.get("exactTime12"),
            (String) body.get("exactTime24"),
            (String) body.get("description"),
            (String) body.get("image1Base64"),
            (String) body.get("image2Base64")
        );

        shareRepository.save(result);
        return ResponseEntity.ok(Map.of("id", id));
    }

    @GetMapping("/api/share/{id}")
public ResponseEntity<?> getShare(@PathVariable String id) { {
        Optional<SharedResult> result = shareRepository.findById(id);

        if (result.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        SharedResult r = result.get();
        return ResponseEntity.ok(java.util.Map.ofEntries(
            Map.entry("city",         r.getCity()),
            Map.entry("country",      r.getCountry()),
            Map.entry("tempF",        r.getTempF()),
            Map.entry("feelsLikeF",   r.getFeelsLikeF()),
            Map.entry("humidity",     r.getHumidity()),
            Map.entry("windSpeed",    r.getWindSpeed()),
            Map.entry("condition",    r.getCondition()),
            Map.entry("timeOfDay",    r.getTimeOfDay()),
            Map.entry("season",       r.getSeason()),
            Map.entry("exactTime12",  r.getExactTime12()),
            Map.entry("exactTime24",  r.getExactTime24()),
            Map.entry("description",  r.getDescription()),
            Map.entry("image1Base64", r.getImage1Base64()),
            Map.entry("image2Base64", r.getImage2Base64())
        ));
    }

    
}

// POST /api/history — save full quality images, return an ID
@PostMapping("/api/history")
public ResponseEntity<Map<String, String>> saveHistory(@RequestBody Map<String, Object> body) {
    String id = UUID.randomUUID().toString().substring(0, 8);
    SharedResult result = new SharedResult(
        "hist-" + id,
        (String) body.get("city"),
        (String) body.getOrDefault("country", ""),
        body.get("tempF") != null ? ((Number) body.get("tempF")).doubleValue() : 0,
        body.get("feelsLikeF") != null ? ((Number) body.get("feelsLikeF")).doubleValue() : 0,
        body.get("humidity") != null ? ((Number) body.get("humidity")).intValue() : 0,
        body.get("windSpeed") != null ? ((Number) body.get("windSpeed")).doubleValue() : 0,
        (String) body.getOrDefault("condition", ""),
        (String) body.getOrDefault("timeOfDay", ""),
        (String) body.getOrDefault("season", ""),
        (String) body.getOrDefault("exactTime12", ""),
        (String) body.getOrDefault("exactTime24", ""),
        (String) body.getOrDefault("description", ""),
        (String) body.getOrDefault("image1Base64", ""),
        (String) body.getOrDefault("image2Base64", "")
    );
    shareRepository.save(result);
    return ResponseEntity.ok(Map.of("id", "hist-" + id));
}

// GET /api/history/{id} — retrieve full quality images
@GetMapping("/api/history/{id}")
public ResponseEntity<?> getHistory(@PathVariable String id) {
    Optional<SharedResult> result = shareRepository.findById(id);
    if (result.isEmpty()) return ResponseEntity.notFound().build();
    SharedResult r = result.get();
    return ResponseEntity.ok(Map.of(
        "image1Base64", r.getImage1Base64(),
        "image2Base64", r.getImage2Base64()
    ));
}}