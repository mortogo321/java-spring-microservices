package com.example.order.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfig {
    // Virtual threads are enabled via spring.threads.virtual.enabled=true
    // Spring Boot 3.2+ automatically uses virtual threads for:
    // - Tomcat request handling
    // - @Async method execution
    // - @Scheduled task execution
}
