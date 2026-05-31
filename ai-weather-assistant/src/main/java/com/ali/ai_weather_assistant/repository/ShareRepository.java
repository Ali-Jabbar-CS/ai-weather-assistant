package com.ali.ai_weather_assistant.repository;

import com.ali.ai_weather_assistant.model.SharedResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ShareRepository extends JpaRepository<SharedResult, String> {
}