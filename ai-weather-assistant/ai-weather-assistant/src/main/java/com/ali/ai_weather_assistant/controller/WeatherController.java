package com.ali.ai_weather_assistant.controller;

import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
        return weatherService.getWeather(city);
    }

    @GetMapping("/weather/describe")
    public String describeWeather(@RequestParam String city) {
        String weatherData = weatherService.getWeather(city);
        String prompt = """
                You are a friendly weather assistant. Given the following weather data in JSON format,
                write a natural, conversational 2-3 sentence description of the current weather.
                Include temperature, conditions, and how it might feel outside.
                Weather data: %s
                """.formatted(weatherData);
        return aiService.askAI(prompt);
    }

    @GetMapping(value = "/weather/image", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> getWeatherImage(@RequestParam String city) throws InterruptedException {
        String weatherData = weatherService.getWeather(city);
        String visualPrompt = aiService.askAI("""
        Based on this weather data, write a short vivid image generation prompt (max 20 words).
        Describe a single unique cityscape or landscape scene with specific lighting and atmosphere.
        Emphasize unique architectural details. No people. No text. No repeated elements.
        Weather data: %s
        """.formatted(weatherData));

        System.out.println("Visual prompt for ComfyUI: " + visualPrompt);
        byte[] imageBytes = comfyUIService.generateWeatherImage(visualPrompt);
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(imageBytes);
    }

    @GetMapping("/weather/full")
    public Map<String, Object> getFullWeatherReport(@RequestParam String city) throws InterruptedException {
        String weatherData = weatherService.getWeather(city);
        String description = aiService.askAI("Describe this weather in 2-3 friendly sentences: " + weatherData);
        return Map.of(
            "city",        city,
            "rawWeather",  weatherData,
            "description", description,
            "imageUrl",    "/weather/image?city=" + city
        );
    }
}