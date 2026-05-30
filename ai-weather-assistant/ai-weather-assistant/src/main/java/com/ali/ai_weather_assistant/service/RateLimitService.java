package com.ali.ai_weather_assistant.service;

import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

@Service
public class RateLimitService {

    // each search makes both images in one prediction
    private static final int MAX_SEARCHES_PER_VISITOR = 10;
    private static final int MAX_SEARCHES_GLOBAL = 50;   // 50 searches x 2 images = 100 images/day

    private LocalDate currentDay = LocalDate.now();
    private int globalSearchCount = 0;
    private final Map<String, Integer> visitorSearchCounts = new ConcurrentHashMap<>();

    private void rolloverIfNewDay() {
        LocalDate today = LocalDate.now();
        if (!today.equals(currentDay)) {
            currentDay = today;
            globalSearchCount = 0;
            visitorSearchCounts.clear();
        }
    }

    // true if this visitor is allowed one more search today (and counts it)
    public synchronized boolean allowSearch(String visitorId) {
        rolloverIfNewDay();

        if (globalSearchCount >= MAX_SEARCHES_GLOBAL) {
            return false;
        }
        int used = visitorSearchCounts.getOrDefault(visitorId, 0);
        if (used >= MAX_SEARCHES_PER_VISITOR) {
            return false;
        }

        globalSearchCount++;
        visitorSearchCounts.put(visitorId, used + 1);
        return true;
    }

    public synchronized int getGlobalSearchCount() {
        return globalSearchCount;
    }
}