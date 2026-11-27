package com.kanbanflow.controller;

import com.kanbanflow.mapper.HealthMapper;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    private final HealthMapper healthMapper;

    public HealthController(HealthMapper healthMapper) {
        this.healthMapper = healthMapper;
    }

    /** DB 연결 확인용: SELECT 1 FROM DUAL */
    @GetMapping("/api/health")
    public Map<String, Object> health() {
        return Map.of("status", "UP", "db", healthMapper.selectOne());
    }
}
