package com.sequenceiq.cloudbreak.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;

import jakarta.inject.Inject;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = RetryErrorPatterns.class)
@EnableConfigurationProperties
class RetryErrorPatternsTest {

    @Inject
    private RetryErrorPatterns retryErrorPatterns;

    @ParameterizedTest
    @MethodSource("provideTestCases")
    void shouldDetectNonRetryableErrors(String message, boolean expectedResult) {
        assertEquals(expectedResult, retryErrorPatterns.containsNonRetryableError(message));
    }

    private static Stream<Arguments> provideTestCases() {
        return Stream.of(
                Arguments.of("TEST...TEST....Failed to execute: {Comment=Command \"/opt/salt/scripts/trust_status_validation.sh\" run}", true),
                Arguments.of("Failed to execute: {Comment=Command \"/opt/salt/scripts/trust_status_validation.sh\" with extra data}", true),
                Arguments.of("Failed to execute: Some context Stderr=Configuration file could not be loaded. More details", true),
                Arguments.of("Failed to execute: prefix Stderr=Configuration file could not be loaded. suffix", true),
                Arguments.of("Failed to execute: Some context Parameter validation failed: Invalid parameter", true),
                Arguments.of("Failed to execute: prefix Parameter validation failed: details", true),
                Arguments.of("Failed to execute: Some context Stdout=Failed to determine the current java version. More output", true),
                Arguments.of("Failed to execute: prefix Stdout=Failed to determine the current java version. suffix", true),
                Arguments.of("Status: 401 Unauthorized Response: <!DOCTYPE html PUBLIC", true),
                Arguments.of("java.net.SocketTimeoutException: Connect timed out", true),
                Arguments.of("STOP,STOP Check if the FreeIPA security rules have not changed and the instance is in running state", true),
                Arguments.of("com.sequenceiq.freeipa.client.FreeIpaHostNotAvailableException", true),
                Arguments.of(null, false),
                Arguments.of("", false),
                Arguments.of("Connection timeout occurred", false),
                Arguments.of("Authentication failed", false),
                Arguments.of("Some other error", false),
                Arguments.of("Failed to execute: Some other command", false)
        );
    }
}

