package com.sequenceiq.cloudbreak.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import jakarta.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Repository of error message patterns used for retry decision logic.
 * Patterns are configured via Spring ConfigurationProperties (retry.errors.patterns-no-retry).
 */
@Component
@ConfigurationProperties(prefix = "retry.errors")
public class RetryErrorPatterns {

    private static final Logger LOGGER = LoggerFactory.getLogger(RetryErrorPatterns.class);

    private List<String> patternsNoRetry = new ArrayList<>();

    private List<Pattern> compiledPatterns = Collections.emptyList();

    @PostConstruct
    public void init() {
        if (patternsNoRetry.isEmpty()) {
            LOGGER.warn("No error patterns configured via retry.errors.patterns-no-retry property");
        }

        this.compiledPatterns = patternsNoRetry.stream()
                .map(Pattern::compile)
                .toList();

        LOGGER.info("Loaded {} non-retryable Salt error patterns", compiledPatterns.size());
    }

    public boolean containsNonRetryableError(String message) {
        if (StringUtils.isEmpty(message)) {
            return false;
        }
        return compiledPatterns.stream().anyMatch(pattern -> pattern.matcher(message).find());
    }

    public List<String> getPatternsNoRetry() {
        return patternsNoRetry;
    }

    public void setPatternsNoRetry(List<String> patternsNoRetry) {
        this.patternsNoRetry = patternsNoRetry;
    }
}