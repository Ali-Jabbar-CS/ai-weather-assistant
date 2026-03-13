package com.ali.ai_weather_assistant.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class WeatherService{

    @Value("${weather.api.key}")
    private String apiKey;

    public String getWeather(String City){
        
        String url = "https://api.openweathermap.org/data/2.5/weather?q="
        + City + "&appid=" + apiKey + "&units=imperial";

        RestTemplate restTemplate = new RestTemplate();

        String result = restTemplate.getForObject(url, String.class);

        return result;
    }

}
