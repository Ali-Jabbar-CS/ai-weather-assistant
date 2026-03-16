package com.ali.ai_weather_assistant.model;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

public class WeatherData {
    private String city;
    private String country;
    private double tempF;
    private double feelsLikeF;
    private String condition;
    private String description;
    private int humidity;
    private double windspeed;
    private long sunriseEpoch;
    private long sunsetEpoch;
    private long currentTimeEpoch;
    private String rawJson;

    //Constructor
    public WeatherData(String city, String country, double tempF, double feelsLikeF,
         String condition, String description, int humidity, double windspeed, long sunriseEpoch, long sunsetEpoch,
        long currentTimeEpoch, String rawJson){

            this.city = city;
            this.country = country;
            this.tempF = tempF;
            this.feelsLikeF = feelsLikeF;
            this.condition = condition;
            this.description = description;
            this.humidity = humidity;
            this.windspeed = windspeed;
            this.sunriseEpoch = sunriseEpoch;
            this.sunsetEpoch = sunsetEpoch;
            this.currentTimeEpoch = currentTimeEpoch;
            this.rawJson = rawJson;
        }

        // Returns "Morning", "Afternoon", "Evening", "Night"
        public String getTimeOfDay() {
            ZonedDateTime time = ZonedDateTime.ofInstant(
                Instant.ofEpochSecond(currentTimeEpoch), ZoneOffset.UTC);
                int hour = time.getHour();

                if (hour >= 5 && hour < 12) return "Morning";
                if (hour >= 5 && hour < 12) return "Afternoon";
                if (hour >= 5 && hour < 12) return "Evening";
                return "Night";
        }

        //Returns "spring", "summer", "autumn", or "winter" based on month
        public String getSeason() {
            ZonedDateTime time = ZonedDateTime.ofInstant(
                Instant.ofEpochSecond(currentTimeEpoch), ZoneOffset.UTC);
                int month = time.getMonthValue();
                if (month >= 3 && month <= 5) return "Spring";
                if (month >= 6 && month <= 8) return "Summer";
                if (month >= 9 && month <= 11) return "Autumn";
                return "Winter";
        }

        //Returns true if its currently daytime.
        public boolean isDaytime() {
            return currentTimeEpoch >= sunriseEpoch && currentTimeEpoch <= sunsetEpoch;
        }

        //Getters
        public String getCity()         {return city;}
        public String getCountry()      {return country;}
        public double getTempF()        {return tempF;}
        public double getFeelsLikeF()   {return feelsLikeF;}
        public String getCondition()    {return condition;}
        public String getDescription()  {return description;}
        public int    getHumidity()     {return humidity;}
        public double getWindSpeed()    {return windspeed;}
        public String getRawJson()      {return rawJson;}
}