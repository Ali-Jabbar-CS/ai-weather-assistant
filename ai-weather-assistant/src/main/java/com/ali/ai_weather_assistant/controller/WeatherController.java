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

    // returns BOTH slideshow images (two prompts, two predictions) as base64 JSON
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

        // image 1: the headline landmark, street level
        String prompt1 = aiService.askAI(
            "Write a photorealistic image generation prompt (max 15 words) for a " +
            data.getSeason() + " " + data.getTimeOfDay() + " cityscape in " +
            data.getCity() + ", " + data.getCountry() + ". " +
            "Weather: " + data.getDescription() + ". " +
            (data.isDaytime() ? "Natural daylight. " : "Dramatic night lighting. ") +
            "Must include a famous skyline, landmark, or iconic architecture. " +
            "Street level perspective. No residential buildings. No gardens. " +
            "No people. No text. Photorealistic only."
        );

        // image 2: a DIFFERENT landmark / vantage so the two aren't near-identical
        String prompt2 = aiService.askAI(
            "Write a photorealistic image generation prompt (max 15 words) for a " +
            data.getSeason() + " " + data.getTimeOfDay() + " view of " +
            data.getCity() + ", " + data.getCountry() + ". " +
            "Weather: " + data.getDescription() + ". " +
            (data.isDaytime() ? "Natural daylight. " : "Dramatic night lighting. ") +
            "Feature a DIFFERENT famous landmark, or a wide skyline from a higher vantage point — " +
            "not the same street-level view. No residential buildings. No gardens. " +
            "No people. No text. Photorealistic only."
        );

        System.out.println("Visual prompt 1: " + prompt1);
        System.out.println("Visual prompt 2: " + prompt2);
        List<byte[]> images = comfyUIService.generateWeatherImages(prompt1, prompt2);

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
                You're a friendly local giving a quick weather update for %s. Write exactly 2 short sentences.
                Conditions: %s. Temperature %d degrees Fahrenheit, feels like %d, humidity %d%%, wind %d mph.

                How to write it:
                - Open with the conditions and how it actually feels outside.
                - Give the temperature once. Only mention humidity or wind if it genuinely stands out (very humid, very windy, or dead still); otherwise skip it.
                - Don't list every number, don't explain why the real and feels-like temperatures differ, and don't lean on filler words like "pretty" or "really".
                - Keep it crisp and warm. No metaphors, no food references, no exclamations.

                Tone to match: "It's grey and overcast in Oslo right now, sitting around 48 degrees. The air is damp and still, so it feels a touch colder than that."
                """.formatted(
                    data.getCity(),
                    data.getDescription(),
                    Math.round(data.getTempF()),
                    Math.round(data.getFeelsLikeF()),
                    data.getHumidity(),
                    Math.round(data.getWindSpeed()));

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