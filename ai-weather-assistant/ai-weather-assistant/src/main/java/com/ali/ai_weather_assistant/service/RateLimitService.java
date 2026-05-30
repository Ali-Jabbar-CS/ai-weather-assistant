package com.ali.ai_weather_assistant.service;

import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

@Service
public class RateLimitService {

    // 10 searches/person/day, and each search makes 2 images
    private static final int MAX_IMAGES_PER_VISITOR = 20;
    private static final int MAX_IMAGES_GLOBAL = 100;

    private LocalDate currentDay = LocalDate.now();
    private int globalImageCount = 0;
    private final Map<String, Integer> visitorImageCounts = new ConcurrentHashMap<>();

    // wipe the counters once we roll over into a new day
    private void rolloverIfNewDay() {
        LocalDate today = LocalDate.now();
        if (!today.equals(currentDay)) {
            currentDay = today;
            globalImageCount = 0;
            visitorImageCounts.clear();
        }
    }

    // true if this visitor is allowed one more image today (and counts it)
    public synchronized boolean allowImage(String visitorId) {
        rolloverIfNewDay();

        if (globalImageCount >= MAX_IMAGES_GLOBAL) {
            return false;
        }
        int used = visitorImageCounts.getOrDefault(visitorId, 0);
        if (used >= MAX_IMAGES_PER_VISITOR) {
            return false;
        }

        globalImageCount++;
        visitorImageCounts.put(visitorId, used + 1);
        return true;
    }

    public synchronized int getGlobalImageCount() {
        return globalImageCount;
    }
}