package com.ali.ai_weather_assistant.service;

import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.ali.ai_weather_assistant.model.WeatherData;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class WeatherService {

    // Open-Meteo is free and needs no API key. It works off lat/lon, so we
    // geocode the city name first, then pull current weather for those coords.
    private static final String GEOCODE_URL  = "https://geocoding-api.open-meteo.com/v1/search";
    private static final String FORECAST_URL = "https://api.open-meteo.com/v1/forecast";

    private final ObjectMapper mapper = new ObjectMapper();
    private final RestTemplate restTemplate = createRestTemplate();

    private static RestTemplate createRestTemplate() {
        SimpleClientHttpRequestFactory rf = new SimpleClientHttpRequestFactory();
        rf.setConnectTimeout(10_000);
        rf.setReadTimeout(10_000);
        return new RestTemplate(rf);
    }

    public String getWeatherRaw(String city) {
        Place place = geocode(city);
        return fetchForecastJson(place.latitude, place.longitude);
    }

    public WeatherData getWeather(String city) {
        Place place = geocode(city);
        String json = fetchForecastJson(place.latitude, place.longitude);

        try {
            JsonNode root    = mapper.readTree(json);
            JsonNode current = root.path("current");

            double tempF      = current.path("temperature_2m").asDouble();
            double feelsLikeF = current.path("apparent_temperature").asDouble();
            int humidity      = current.path("relative_humidity_2m").asInt();
            double windSpeed  = current.path("wind_speed_10m").asDouble();
            int code          = current.path("weather_code").asInt();
            long currentTime  = current.path("time").asLong();
            int utcOffset     = root.path("utc_offset_seconds").asInt();

            long sunrise = root.path("daily").path("sunrise").path(0).asLong();
            long sunset  = root.path("daily").path("sunset").path(0).asLong();

            return new WeatherData(
                place.name,
                place.country,
                tempF,
                feelsLikeF,
                conditionFromCode(code),
                descriptionFromCode(code),
                humidity,
                windSpeed,
                sunrise,
                sunset,
                currentTime,
                utcOffset,
                json
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Open-Meteo forecast: " + e.getMessage(), e);
        }
    }

    // ---- geocoding: city name -> coordinates ----
    private Place geocode(String input) {
        String cityName = input;
        String wantedCountry = null;
        if (input.contains(",")) {
            String[] parts = input.split(",", 2);
            cityName = parts[0].trim();
            wantedCountry = parts[1].trim();   // can be a code ("FR") or a name ("France")
        }

        String url = GEOCODE_URL + "?name=" + encode(cityName) + "&count=10&language=en&format=json";
        String json = restTemplate.getForObject(url, String.class);

        try {
            JsonNode results = mapper.readTree(json).path("results");
            if (!results.isArray() || results.isEmpty()) {
                throw new RuntimeException("City not found: " + input);
            }

            JsonNode chosen = results.get(0);
            if (wantedCountry != null && !wantedCountry.isEmpty()) {
                for (JsonNode r : results) {
                    String cc    = r.path("country_code").asText("");
                    String cName = r.path("country").asText("");
                    if (wantedCountry.equalsIgnoreCase(cc) || wantedCountry.equalsIgnoreCase(cName)) {
                        chosen = r;
                        break;
                    }
                }
            }

            Place p = new Place();
            p.name      = chosen.path("name").asText(cityName);
            p.country   = chosen.path("country").asText("");   // full name (e.g. Germany), not the DE/GB code
            p.latitude  = chosen.path("latitude").asDouble();
            p.longitude = chosen.path("longitude").asDouble();
            return p;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to geocode city: " + e.getMessage(), e);
        }
    }

    private String fetchForecastJson(double lat, double lon) {
        String url = FORECAST_URL
            + "?latitude=" + lat
            + "&longitude=" + lon
            + "&current=temperature_2m,apparent_temperature,relative_humidity_2m,wind_speed_10m,weather_code"
            + "&daily=sunrise,sunset"
            + "&temperature_unit=fahrenheit"
            + "&wind_speed_unit=mph"
            + "&timezone=auto"
            + "&timeformat=unixtime";
        return restTemplate.getForObject(url, String.class);
    }

    private String encode(String s) {
        return java.net.URLEncoder.encode(s, java.nio.charset.StandardCharsets.UTF_8);
    }

    // ---- WMO weather code -> text (Open-Meteo sends a number, not words) ----
    private String conditionFromCode(int code) {
        if (code == 0) return "Clear";
        if (code <= 3) return "Clouds";
        if (code == 45 || code == 48) return "Fog";
        if (code >= 51 && code <= 57) return "Drizzle";
        if (code >= 61 && code <= 67) return "Rain";
        if (code >= 71 && code <= 77) return "Snow";
        if (code >= 80 && code <= 82) return "Rain";
        if (code >= 85 && code <= 86) return "Snow";
        if (code >= 95) return "Thunderstorm";
        return "Clouds";
    }

    private String descriptionFromCode(int code) {
        return switch (code) {
            case 0  -> "clear sky";
            case 1  -> "mainly clear";
            case 2  -> "partly cloudy";
            case 3  -> "overcast";
            case 45 -> "fog";
            case 48 -> "depositing rime fog";
            case 51 -> "light drizzle";
            case 53 -> "moderate drizzle";
            case 55 -> "dense drizzle";
            case 56 -> "light freezing drizzle";
            case 57 -> "dense freezing drizzle";
            case 61 -> "light rain";
            case 63 -> "moderate rain";
            case 65 -> "heavy rain";
            case 66 -> "light freezing rain";
            case 67 -> "heavy freezing rain";
            case 71 -> "light snow";
            case 73 -> "moderate snow";
            case 75 -> "heavy snow";
            case 77 -> "snow grains";
            case 80 -> "light rain showers";
            case 81 -> "moderate rain showers";
            case 82 -> "violent rain showers";
            case 85 -> "light snow showers";
            case 86 -> "heavy snow showers";
            case 95 -> "thunderstorm";
            case 96 -> "thunderstorm with light hail";
            case 99 -> "thunderstorm with heavy hail";
            default -> "unknown conditions";
        };
    }

    // a geocoded place
    private static class Place {
        String name;
        String country;
        double latitude;
        double longitude;
    }
}