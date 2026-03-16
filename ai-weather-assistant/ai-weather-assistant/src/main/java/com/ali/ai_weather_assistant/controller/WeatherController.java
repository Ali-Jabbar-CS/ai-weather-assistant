package com.ali.ai_weather_assistant.controller;

import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ali.ai_weather_assistant.model.WeatherData;
import com.ali.ai_weather_assistant.service.AIService;
import com.ali.ai_weather_assistant.service.ComfyUIService;
import com.ali.ai_weather_assistant.service.WeatherService;

@RestController
public class WeatherController {

    private final WeatherService weatherService;
    private final AIService aiService;
    private final ComfyUIService comfyUIService;

    public WeatherController(WeatherService weatherService, AIService aiService, ComfyUIService comfyUIService) {
        this.weatherService = weatherService;
        this.aiService = aiService;
        this.comfyUIService = comfyUIService;
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
        Describe the weather in %s in exactly 2 sentences.
        Current conditions: %.1f degrees Fahrenheit, %s.
        Only describe temperature and sky conditions. 
        Do NOT mention time of day, season, or make comparisons.
        """.formatted(
            data.getCity(),
            data.getTempF(),
            data.getDescription());
        return aiService.askAI(prompt);
    }

    @GetMapping(value = "/weather/image", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> getWeatherImage(@RequestParam String city) throws InterruptedException {
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
        byte[] imageBytes = comfyUIService.generateWeatherImage(visualPrompt);
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(imageBytes);
    }

    @GetMapping("/weather/full")
    public Map<String, Object> getFullWeatherReport(@RequestParam String city) throws InterruptedException {
        WeatherData data = weatherService.getWeather(city);
        String description = aiService.askAI("Describe this weather in 2-3 friendly sentences: " + data.getRawJson());
        return Map.of(
            "city", data.getCity(),
            "country", data.getCountry(),
            "tempF", data.getTempF(),
            "Condition", data.getCondition(),
            "timeOfDay", data.getTimeOfDay(),
            "season", data.getSeason(),
            "description", description,
            "imageUrl", "/weather/image?city=" + city);
        
    }
}