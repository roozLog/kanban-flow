package com.kanbanflow;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.kanbanflow.mapper")
public class KanbanFlowApplication {
    public static void main(String[] args) {
        SpringApplication.run(KanbanFlowApplication.class, args);
    }
}
