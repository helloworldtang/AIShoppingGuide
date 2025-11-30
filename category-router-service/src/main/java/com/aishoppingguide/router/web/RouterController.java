package com.aishoppingguide.router.web;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/internal/router")
public class RouterController {

    @PostMapping(path = "/route", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> route(@RequestBody Map<String, Object> body) {
        return Map.of("category", "placeholder", "brandSplitNeeded", false);
    }
}
