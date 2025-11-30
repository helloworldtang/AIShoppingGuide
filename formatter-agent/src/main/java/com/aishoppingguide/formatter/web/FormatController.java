package com.aishoppingguide.formatter.web;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/internal/format")
public class FormatController {

    @PostMapping(path = "/apply", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> apply(@RequestBody Map<String, Object> body) {
        return Map.of("final", true, "title", "统一格式化占位", "items", new Object[]{});
    }
}
