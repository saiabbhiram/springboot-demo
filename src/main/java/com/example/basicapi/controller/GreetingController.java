package com.example.basicapi.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class GreetingController {

    @GetMapping("/hello")
    public Map<String, Object> sayHello(@RequestParam(defaultValue = "World") String name) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message", "Hello, " + name + "!");
        response.put("timestamp", LocalDateTime.now());
        return response;
    }

    @GetMapping("/health")
    public Map<String, String> healthCheck() {
        Map<String, String> health = new LinkedHashMap<>();
        health.put("status", "UP");
        health.put("app", "spring-boot-basic-api");
        return health;
    }
}
