package com.ali.ai_weather_assistant.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.ali.ai_weather_assistant.model.WeatherData;

@Service
public class WeatherService {

    @Value("${weather.api.key}")
    private String apiKey;

    public String getWeatherRaw(String city) {
        String url = "https://api.openweathermap.org/data/2.5/weather?q="
                + city + "&appid=" + apiKey + "&units=imperial";
        return new RestTemplate().getForObject(url, String.class);
    }

    public WeatherData getWeather(String city) {
        String json = getWeatherRaw(city);

        return new WeatherData(
            city,
            extractString(json, "\"country\":\""),
            extractDouble(json, "\"temp\":"),
            extractDouble(json, "\"feels_like\":"),
            extractString(json, "\"main\":\""),
            extractString(json, "\"description\":\""),
            (int) extractDouble(json, "\"humidity\":"),
            extractDouble(json, "\"speed\":"),
            (long) extractDouble(json, "\"sunrise\":"),
            (long) extractDouble(json, "\"sunset\":"),
            (long) extractDouble(json, "\"dt\":"),
            (int) extractDouble(json, "\"timezone\":"),
            json
        );
    }

    private String extractString(String json, String key) {
        int start = json.indexOf(key);
        if (start == -1) return "unknown";
        start += key.length();
        int end = json.indexOf("\"", start);
        return end == -1 ? "unknown" : json.substring(start, end);
    }

    private double extractDouble(String json, String key) {
        int start = json.indexOf(key);
        if (start == -1) return 0;
        start += key.length();
        int end = start;
        while (end < json.length() && (Character.isDigit(json.charAt(end))
                || json.charAt(end) == '.' || json.charAt(end) == '-')) {
            end++;
        }
        try { return Double.parseDouble(json.substring(start, end)); }
        catch (NumberFormatException e) { return 0; }
    }
}