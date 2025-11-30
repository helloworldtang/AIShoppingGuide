package com.aishoppingguide.stdqa.service;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class StandardQaService {
    @Cacheable(cacheNames = "stdqa", key = "#question")
    public Map<String, Object> match(String question) {
        return Map.of("hit", false);
    }
}
