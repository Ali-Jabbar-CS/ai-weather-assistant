package com.ali.ai_weather_assistant.model;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class WeatherData {

    private String city;
    private String country;
    private double tempF;
    private double feelsLikeF;
    private String condition;
    private String description;
    private int humidity;
    private double windSpeed;
    private long sunriseEpoch;
    private long sunsetEpoch;
    private long currentTimeEpoch;
    private int timezone;
    private String rawJson;

    public WeatherData(String city, String country, double tempF, double feelsLikeF,
                       String condition, String description, int humidity, double windSpeed,
                       long sunriseEpoch, long sunsetEpoch, long currentTimeEpoch,
                       int timezone, String rawJson) {
        this.city = city;
        this.country = country;
        this.tempF = tempF;
        this.feelsLikeF = feelsLikeF;
        this.condition = condition;
        this.description = description;
        this.humidity = humidity;
        this.windSpeed = windSpeed;
        this.sunriseEpoch = sunriseEpoch;
        this.sunsetEpoch = sunsetEpoch;
        this.currentTimeEpoch = currentTimeEpoch;
        this.timezone = timezone;
        this.rawJson = rawJson;
    }

    // Returns exact local time in 24hr format e.g. "07:42"
    public String getExactTime24() {
        ZonedDateTime localTime = ZonedDateTime.ofInstant(
            Instant.ofEpochSecond(currentTimeEpoch),
            ZoneOffset.ofTotalSeconds(timezone)
        );
        return localTime.format(DateTimeFormatter.ofPattern("HH:mm"));
    }

    // Returns exact local time in 12hr format e.g. "7:42 AM"
    public String getExactTime12() {
        ZonedDateTime localTime = ZonedDateTime.ofInstant(
            Instant.ofEpochSecond(currentTimeEpoch),
            ZoneOffset.ofTotalSeconds(timezone)
        );
        return localTime.format(DateTimeFormatter.ofPattern("h:mm a"));
    }

    // Returns "morning", "afternoon", "evening", or "night" based on LOCAL time
    public String getTimeOfDay() {
        ZonedDateTime localTime = ZonedDateTime.ofInstant(
            Instant.ofEpochSecond(currentTimeEpoch),
            ZoneOffset.ofTotalSeconds(timezone)
        );
        int hour = localTime.getHour();
        if (hour >= 5 && hour < 12)  return "morning";
        if (hour >= 12 && hour < 17) return "afternoon";
        if (hour >= 17 && hour < 21) return "evening";
        return "night";
    }

    // Returns season based on LOCAL month
    public String getSeason() {
        ZonedDateTime localTime = ZonedDateTime.ofInstant(
            Instant.ofEpochSecond(currentTimeEpoch),
            ZoneOffset.ofTotalSeconds(timezone)
        );
        int month = localTime.getMonthValue();
        if (month >= 3 && month <= 5)  return "spring";
        if (month >= 6 && month <= 8)  return "summer";
        if (month >= 9 && month <= 11) return "autumn";
        return "winter";
    }

    public boolean isDaytime() {
        return currentTimeEpoch >= sunriseEpoch && currentTimeEpoch <= sunsetEpoch;
    }

    // Getters
    public String getCity()         { return city; }
    public String getCountry()      { return country; }
    public double getTempF()        { return tempF; }
    public double getFeelsLikeF()   { return feelsLikeF; }
    public String getCondition()    { return condition; }
    public String getDescription()  { return description; }
    public int getHumidity()        { return humidity; }
    public double getWindSpeed()    { return windSpeed; }
    public String getRawJson()      { return rawJson; }
}