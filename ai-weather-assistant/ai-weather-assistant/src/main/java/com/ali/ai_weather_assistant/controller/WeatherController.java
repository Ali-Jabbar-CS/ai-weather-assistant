package com.ali.ai_weather_assistant.controller;

import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import com.ali.ai_weather_assistant.model.WeatherData;
import com.ali.ai_weather_assistant.service.AIService;
import com.ali.ai_weather_assistant.service.ComfyUIService;
import com.ali.ai_weather_assistant.service.RateLimitService;
import com.ali.ai_weather_assistant.service.WeatherService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
public class WeatherController {

    private final WeatherService weatherService;
    private final AIService aiService;
    private final ComfyUIService comfyUIService;
    private final RateLimitService rateLimitService;

    public WeatherController(WeatherService weatherService, AIService aiService,
                             ComfyUIService comfyUIService, RateLimitService rateLimitService) {
        this.weatherService = weatherService;
        this.aiService = aiService;
        this.comfyUIService = comfyUIService;
        this.rateLimitService = rateLimitService;
    }

    @GetMapping("/test")
    public String test(@RequestParam String city) {
        return "Controller works: " + city;
    }

    @GetMapping("/weather")
    public String getWeather(@RequestParam String city) {
        return weatherService.getWeatherRaw(city);
    }

    @GetMapping("/weather/describe")
    public String describeWeather(@RequestParam String city) {
        WeatherData data = weatherService.getWeather(city);
        String prompt = """
                Describe the current weather in %s in exactly 2 sentences.
                Facts: %s, %.1f degrees Fahrenheit (feels like %.1f), humidity %d%%, wind %.1f mph, %s, %s.
                Be specific and factual. Mention temperature and conditions only. No metaphors or food references.
                """.formatted(
                    data.getCity(),
                    data.getDescription(),
                    data.getTempF(),
                    data.getFeelsLikeF(),
                    data.getHumidity(),
                    data.getWindSpeed(),
                    data.getTimeOfDay(),
                    data.getSeason());
        return aiService.askAI(prompt);
    }

    // returns BOTH slideshow images (one prediction) as base64 JSON
    @GetMapping("/weather/images")
    public ResponseEntity<Map<String, String>> getWeatherImages(@RequestParam String city,
                                                                HttpServletRequest request)
            throws InterruptedException {

        String visitorId = clientIp(request);
        if (!rateLimitService.allowSearch(visitorId)) {
            System.out.println("Rate limit hit for " + visitorId
                    + " (searches today, global: " + rateLimitService.getGlobalSearchCount() + ")");
            return ResponseEntity.status(429).body(Map.of("error", "Daily limit reached. Try again tomorrow."));
        }

        WeatherData data = weatherService.getWeather(city);

        String visualPrompt = aiService.askAI(
            "Write a photorealistic image generation prompt (max 15 words) for a " +
            data.getSeason() + " " + data.getTimeOfDay() + " cityscape in " +
            data.getCity() + ", " + data.getCountry() + ". " +
            "Weather: " + data.getDescription() + ". " +
            (data.isDaytime() ? "Natural daylight. " : "Dramatic night lighting. ") +
            "Must include a famous skyline, landmark, or iconic architecture. " +
            "Street level perspective. No residential buildings. No gardens. " +
            "No people. No text. Photorealistic only."
        );

        System.out.println("Visual prompt: " + visualPrompt);
        List<byte[]> images = comfyUIService.generateWeatherImages(visualPrompt);

        Base64.Encoder enc = Base64.getEncoder();
        Map<String, String> result = new HashMap<>();
        result.put("image1", images.size() > 0 ? enc.encodeToString(images.get(0)) : "");
        result.put("image2", images.size() > 1 ? enc.encodeToString(images.get(1)) : "");
        return ResponseEntity.ok(result);
    }

    @GetMapping("/weather/full")
    public Map<String, Object> getFullWeatherReport(@RequestParam String city)
            throws InterruptedException {
        WeatherData data = weatherService.getWeather(city);

        String prompt = """
                Describe the current weather in %s in exactly 2 sentences.
                Facts: %s, %.1f degrees Fahrenheit (feels like %.1f), humidity %d%%, wind %.1f mph.
                Be specific and factual. No metaphors, no food references, no exclamations.
                """.formatted(
                    data.getCity(),
                    data.getDescription(),
                    data.getTempF(),
                    data.getFeelsLikeF(),
                    data.getHumidity(),
                    data.getWindSpeed());

        String description = aiService.askAI(prompt);

        return Map.ofEntries(
            Map.entry("city",        data.getCity()),
            Map.entry("country",     data.getCountry()),
            Map.entry("tempF",       data.getTempF()),
            Map.entry("feelsLikeF",  data.getFeelsLikeF()),
            Map.entry("humidity",    data.getHumidity()),
            Map.entry("windSpeed",   data.getWindSpeed()),
            Map.entry("condition",   data.getCondition()),
            Map.entry("timeOfDay",   data.getTimeOfDay()),
            Map.entry("season",      data.getSeason()),
            Map.entry("exactTime12", data.getExactTime12()),
            Map.entry("exactTime24", data.getExactTime24()),
            Map.entry("description", description)
        );
    }

    @GetMapping("/share/{id}")
    public ModelAndView sharePage(@PathVariable String id) {
        return new ModelAndView("forward:/index.html");
    }

    // real client IP — on Railway we sit behind a proxy, so check X-Forwarded-For first
    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}