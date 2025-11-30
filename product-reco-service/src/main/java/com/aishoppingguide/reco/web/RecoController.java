package com.aishoppingguide.reco.web;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/internal/reco")
public class RecoController {

    @PostMapping(path = "/select", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> select(@RequestBody Map<String, Object> body) {
        return Map.of(
            "top3", List.of(),
            "exact_target", null
        );
    }
}
