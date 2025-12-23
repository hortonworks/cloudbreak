package com.sequenceiq.cloudbreak.orchestrator.host;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.function.Predicate;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {RetryErrorPatterns.class, RetryPredicates.class})
@EnableConfigurationProperties
class RetryPredicatesTest {

    @Inject
    private RetryPredicates retryPredicates;

    @Test
    void retryAllShouldAcceptAllExceptions() {
        Predicate<Exception> predicate = retryPredicates.retryAll();

        assertTrue(predicate.test(new Exception("timeout")));
        assertTrue(predicate.test(new Exception("authentication failed")));
        assertTrue(predicate.test(new Exception("any error")));
    }

    @Test
    void retryNoneShouldRejectAllExceptions() {
        Predicate<Exception> predicate = retryPredicates.retryNone();

        assertFalse(predicate.test(new Exception("timeout")));
        assertFalse(predicate.test(new Exception("connection error")));
        assertFalse(predicate.test(new Exception("any error")));
    }

    @Test
    void retryTransientErrorsShouldRejectSpecificSaltErrors() {
        Predicate<Exception> predicate = retryPredicates.retryTransientErrors();

        assertFalse(predicate.test(new Exception("Failed to execute: {Comment=Command \"/opt/salt/scripts/trust_status_validation.sh\" run}")));
        assertFalse(predicate.test(new Exception("Failed to execute: Some context Stderr=Configuration file could not be loaded.")));
        assertFalse(predicate.test(new Exception("Failed to execute: prefix Parameter validation failed: details")));
        assertFalse(predicate.test(new Exception("Failed to execute: prefix Stdout=Failed to determine the current java version.")));
    }

    @Test
    void retryTransientErrorsShouldAcceptOtherErrors() {
        Predicate<Exception> predicate = retryPredicates.retryTransientErrors();

        assertTrue(predicate.test(new Exception("Connection timeout occurred")));
        assertTrue(predicate.test(new Exception("Authentication failed")));
        assertTrue(predicate.test(new Exception("Unauthorized access")));
        assertTrue(predicate.test(new Exception("Permission denied")));
        assertTrue(predicate.test(new Exception("Invalid configuration")));
        assertTrue(predicate.test(new Exception("Network unreachable")));
        assertTrue(predicate.test(new Exception("Temporarily unavailable")));
        assertTrue(predicate.test(new Exception("Failed to execute: some other command")));
        assertTrue(predicate.test(new Exception("Any other error message")));
    }

    @Test
    void retryTransientErrorsShouldHandleNullMessage() {
        Predicate<Exception> predicate = retryPredicates.retryTransientErrors();

        assertTrue(predicate.test(new Exception((String) null)));
    }
}