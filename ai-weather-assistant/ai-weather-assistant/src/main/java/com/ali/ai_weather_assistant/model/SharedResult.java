package com.ali.ai_weather_assistant.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name= "shared_results")
public class SharedResult {


    @Id
    private String id;


    private String city;
    private String country;
    private double tempF;
    private double feelsLikeF;
    private int humidity;
    private double windSpeed;
    private String condition;
    private String timeOfDay;
    private String season;
    private String exactTime12;
    private String exactTime24;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String image1Base64;

    @Column(columnDefinition = "TEXT")
    private String image2Base64;

    public SharedResult() {}

    public SharedResult(String id, String city, String country, double tempF, double feelsLikeF, int humidity, double windSpeed, 
                        String condition, String timeOfDay, String season, String exactTime12, String exactTime24, 
                        String description, String image1Base64, String image2Base64) {

                            this.id = id;
                            this.city = city;
                            this.country = country;
                            this.tempF = tempF;
                            this.feelsLikeF = feelsLikeF;
                            this.humidity = humidity;
                            this.windSpeed = windSpeed;
                            this.condition = condition;
                            this.timeOfDay = timeOfDay;
                            this.season = season;
                            this.exactTime12 = exactTime12;
                            this.exactTime24= exactTime24;
                            this.description = description;
                            this.image1Base64 = image1Base64;
                            this.image2Base64 = image2Base64;
        }

    public String getId()           { return id; }
    public String getCity()         { return city; }
    public String getCountry()      { return country; }
    public double getTempF()        { return tempF; }
    public double getFeelsLikeF()   { return feelsLikeF; }
    public int getHumidity()        { return humidity; }
    public double getWindSpeed()    { return windSpeed; }
    public String getCondition()    { return condition; }
    public String getTimeOfDay()    { return timeOfDay; }
    public String getSeason()       { return season; }
    public String getExactTime12()  { return exactTime12; }
    public String getExactTime24()  { return exactTime24; }
    public String getDescription()  { return description; }
    public String getImage1Base64() { return image1Base64; }
    public String getImage2Base64() { return image2Base64; }
    


}