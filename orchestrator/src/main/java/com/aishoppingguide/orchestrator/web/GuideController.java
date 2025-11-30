package com.aishoppingguide.orchestrator.web;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/v1/guide")
public class GuideController {

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    @PostMapping(path = "/stream", consumes = MediaType.APPLICATION_JSON_VALUE)
    public SseEmitter stream(@RequestBody Map<String, Object> body) {
        SseEmitter emitter = new SseEmitter(Duration.ofSeconds(180).toMillis());
        String traceId = UUID.randomUUID().toString();

        scheduler.scheduleAtFixedRate(() -> {
            try {
                emitter.send(SseEmitter.event()
                    .name("ping")
                    .data(Map.of("traceId", traceId, "ts", System.currentTimeMillis())));
            } catch (IOException ignored) {}
        }, 0, 10, TimeUnit.SECONDS);

        Executors.newSingleThreadExecutor().submit(() -> {
            try {
                emitter.send(SseEmitter.event().name("intent").data(Map.of("traceId", traceId, "status", "ok")));
                emitter.send(SseEmitter.event().name("route").data(Map.of("traceId", traceId, "category", "placeholder")));
                emitter.send(SseEmitter.event().name("formatted").data(Map.of(
                    "final", false,
                    "title", "占位：推荐结果",
                    "items", new Object[]{}
                )));
                emitter.send(SseEmitter.event().name("final").data(Map.of("final", true)));
                emitter.complete();
            } catch (IOException e) {
                emitter.completeWithError(e);
            }
        });

        emitter.onCompletion(() -> {});
        emitter.onTimeout(() -> emitter.complete());
        return emitter;
    }

    @GetMapping(path = "/stream")
    public SseEmitter streamGet(@RequestParam(name = "text", required = false) String text) {
        return stream(Map.of("text", text == null ? "" : text));
    }
}
