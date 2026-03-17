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
        String query = parseCity(city);
        String url = "https://api.openweathermap.org/data/2.5/weather?q="
                + query + "&appid=" + apiKey + "&units=imperial";
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

    private String parseCity(String input) {
        if (!input.contains(",")) return input.trim();
        String[] parts = input.split(",", 2);
        String cityPart = parts[0].trim();
        String countryPart = parts[1].trim();
        if (countryPart.length() == 2) {
            return cityPart + "," + countryPart.toUpperCase();
        }
        String code = COUNTRY_CODES.getOrDefault(countryPart.toLowerCase(), countryPart);
        return cityPart + "," + code.toUpperCase();
    }

    private static final java.util.Map<String, String> COUNTRY_CODES =
        java.util.Map.ofEntries(
            java.util.Map.entry("afghanistan", "AF"),
            java.util.Map.entry("albania", "AL"),
            java.util.Map.entry("algeria", "DZ"),
            java.util.Map.entry("argentina", "AR"),
            java.util.Map.entry("armenia", "AM"),
            java.util.Map.entry("australia", "AU"),
            java.util.Map.entry("austria", "AT"),
            java.util.Map.entry("azerbaijan", "AZ"),
            java.util.Map.entry("bahrain", "BH"),
            java.util.Map.entry("bangladesh", "BD"),
            java.util.Map.entry("belarus", "BY"),
            java.util.Map.entry("belgium", "BE"),
            java.util.Map.entry("bolivia", "BO"),
            java.util.Map.entry("bosnia", "BA"),
            java.util.Map.entry("brazil", "BR"),
            java.util.Map.entry("bulgaria", "BG"),
            java.util.Map.entry("cambodia", "KH"),
            java.util.Map.entry("cameroon", "CM"),
            java.util.Map.entry("canada", "CA"),
            java.util.Map.entry("chile", "CL"),
            java.util.Map.entry("china", "CN"),
            java.util.Map.entry("colombia", "CO"),
            java.util.Map.entry("croatia", "HR"),
            java.util.Map.entry("cuba", "CU"),
            java.util.Map.entry("czech republic", "CZ"),
            java.util.Map.entry("czechia", "CZ"),
            java.util.Map.entry("denmark", "DK"),
            java.util.Map.entry("ecuador", "EC"),
            java.util.Map.entry("egypt", "EG"),
            java.util.Map.entry("ethiopia", "ET"),
            java.util.Map.entry("finland", "FI"),
            java.util.Map.entry("france", "FR"),
            java.util.Map.entry("georgia", "GE"),
            java.util.Map.entry("germany", "DE"),
            java.util.Map.entry("ghana", "GH"),
            java.util.Map.entry("greece", "GR"),
            java.util.Map.entry("hungary", "HU"),
            java.util.Map.entry("india", "IN"),
            java.util.Map.entry("indonesia", "ID"),
            java.util.Map.entry("iran", "IR"),
            java.util.Map.entry("iraq", "IQ"),
            java.util.Map.entry("ireland", "IE"),
            java.util.Map.entry("israel", "IL"),
            java.util.Map.entry("italy", "IT"),
            java.util.Map.entry("jamaica", "JM"),
            java.util.Map.entry("japan", "JP"),
            java.util.Map.entry("jordan", "JO"),
            java.util.Map.entry("kazakhstan", "KZ"),
            java.util.Map.entry("kenya", "KE"),
            java.util.Map.entry("kuwait", "KW"),
            java.util.Map.entry("kyrgyzstan", "KG"),
            java.util.Map.entry("laos", "LA"),
            java.util.Map.entry("latvia", "LV"),
            java.util.Map.entry("lebanon", "LB"),
            java.util.Map.entry("libya", "LY"),
            java.util.Map.entry("lithuania", "LT"),
            java.util.Map.entry("luxembourg", "LU"),
            java.util.Map.entry("malaysia", "MY"),
            java.util.Map.entry("mexico", "MX"),
            java.util.Map.entry("moldova", "MD"),
            java.util.Map.entry("mongolia", "MN"),
            java.util.Map.entry("morocco", "MA"),
            java.util.Map.entry("mozambique", "MZ"),
            java.util.Map.entry("myanmar", "MM"),
            java.util.Map.entry("nepal", "NP"),
            java.util.Map.entry("netherlands", "NL"),
            java.util.Map.entry("new zealand", "NZ"),
            java.util.Map.entry("nigeria", "NG"),
            java.util.Map.entry("north korea", "KP"),
            java.util.Map.entry("norway", "NO"),
            java.util.Map.entry("oman", "OM"),
            java.util.Map.entry("pakistan", "PK"),
            java.util.Map.entry("panama", "PA"),
            java.util.Map.entry("paraguay", "PY"),
            java.util.Map.entry("peru", "PE"),
            java.util.Map.entry("philippines", "PH"),
            java.util.Map.entry("poland", "PL"),
            java.util.Map.entry("portugal", "PT"),
            java.util.Map.entry("qatar", "QA"),
            java.util.Map.entry("romania", "RO"),
            java.util.Map.entry("russia", "RU"),
            java.util.Map.entry("saudi arabia", "SA"),
            java.util.Map.entry("senegal", "SN"),
            java.util.Map.entry("serbia", "RS"),
            java.util.Map.entry("singapore", "SG"),
            java.util.Map.entry("slovakia", "SK"),
            java.util.Map.entry("slovenia", "SI"),
            java.util.Map.entry("somalia", "SO"),
            java.util.Map.entry("south africa", "ZA"),
            java.util.Map.entry("south korea", "KR"),
            java.util.Map.entry("spain", "ES"),
            java.util.Map.entry("sri lanka", "LK"),
            java.util.Map.entry("sudan", "SD"),
            java.util.Map.entry("sweden", "SE"),
            java.util.Map.entry("switzerland", "CH"),
            java.util.Map.entry("syria", "SY"),
            java.util.Map.entry("taiwan", "TW"),
            java.util.Map.entry("tajikistan", "TJ"),
            java.util.Map.entry("tanzania", "TZ"),
            java.util.Map.entry("thailand", "TH"),
            java.util.Map.entry("tunisia", "TN"),
            java.util.Map.entry("turkey", "TR"),
            java.util.Map.entry("turkmenistan", "TM"),
            java.util.Map.entry("uganda", "UG"),
            java.util.Map.entry("ukraine", "UA"),
            java.util.Map.entry("united arab emirates", "AE"),
            java.util.Map.entry("uae", "AE"),
            java.util.Map.entry("united kingdom", "GB"),
            java.util.Map.entry("uk", "GB"),
            java.util.Map.entry("united states", "US"),
            java.util.Map.entry("usa", "US"),
            java.util.Map.entry("uruguay", "UY"),
            java.util.Map.entry("uzbekistan", "UZ"),
            java.util.Map.entry("venezuela", "VE"),
            java.util.Map.entry("vietnam", "VN"),
            java.util.Map.entry("yemen", "YE"),
            java.util.Map.entry("zambia", "ZM"),
            java.util.Map.entry("zimbabwe", "ZW")
        );

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