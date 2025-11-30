package com.aishoppingguide.intent.web;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/internal/intent")
public class IntentController {

    @PostMapping(path = "/classify", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> classify(@RequestBody Map<String, Object> body) {
        return Map.of(
            "isGuide", true,
            "isStandard", false,
            "category", "placeholder",
            "brandSplitNeeded", false,
            "keywords", new String[]{},
            "confidence", 0.8
        );
    }
}
