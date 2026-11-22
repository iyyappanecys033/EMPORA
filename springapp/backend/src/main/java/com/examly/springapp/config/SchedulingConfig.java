package com.examly.springapp.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Enables Spring scheduled background processing (auto-closure and SLA breach scanning)
 * in runtime profiles while allowing deterministic test execution with epams.scheduling.enabled=false.
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "epams.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class SchedulingConfig {
}
