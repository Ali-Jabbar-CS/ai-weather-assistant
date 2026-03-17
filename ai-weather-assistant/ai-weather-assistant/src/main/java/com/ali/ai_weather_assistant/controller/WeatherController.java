package com.ali.ai_weather_assistant.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

    public WeatherController(WeatherService weatherService, AIService aiService,
                             ComfyUIService comfyUIService) {
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

    @GetMapping(value = "/weather/image", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> getWeatherImage(@RequestParam String city)
            throws InterruptedException {
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
    public java.util.Map<String, Object> getFullWeatherReport(@RequestParam String city)
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

        return java.util.Map.ofEntries(
            java.util.Map.entry("city",        data.getCity()),
            java.util.Map.entry("country",     data.getCountry()),
            java.util.Map.entry("tempF",       data.getTempF()),
            java.util.Map.entry("feelsLikeF",  data.getFeelsLikeF()),
            java.util.Map.entry("humidity",    data.getHumidity()),
            java.util.Map.entry("windSpeed",   data.getWindSpeed()),
            java.util.Map.entry("condition",   data.getCondition()),
            java.util.Map.entry("timeOfDay",   data.getTimeOfDay()),
            java.util.Map.entry("season",      data.getSeason()),
            java.util.Map.entry("exactTime12", data.getExactTime12()),
            java.util.Map.entry("exactTime24", data.getExactTime24()),
            java.util.Map.entry("description", description)
        );
    }
    @GetMapping({"/share/{id}"})
public org.springframework.web.servlet.ModelAndView sharePage(@PathVariable String id) {
    return new org.springframework.web.servlet.ModelAndView("forward:/index.html");
}
}