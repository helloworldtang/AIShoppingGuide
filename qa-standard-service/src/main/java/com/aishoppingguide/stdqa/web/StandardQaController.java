package com.aishoppingguide.stdqa.web;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import com.aishoppingguide.stdqa.service.StandardQaService;
import org.springframework.beans.factory.annotation.Autowired;

@RestController
@RequestMapping("/internal/stdqa")
public class StandardQaController {
    @Autowired
    private StandardQaService standardQaService;

    @PostMapping(path = "/check", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> check(@RequestBody Map<String, Object> body) {
        String question = String.valueOf(body.getOrDefault("text", ""));
        return standardQaService.match(question);
    }
}
