package com.sequenceiq.redbeams.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.test.util.ReflectionTestUtils;

class ChangeInstanceTypeConfigTest {

    private final ChangeInstanceTypeConfig underTest = new ChangeInstanceTypeConfig();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(underTest, "pollingIntervalInSeconds", 30);
        ReflectionTestUtils.setField(underTest, "awsTimeoutInMinutes", 30);
        ReflectionTestUtils.setField(underTest, "azureHaTimeoutInMinutes", 31);
        ReflectionTestUtils.setField(underTest, "azureTimeoutInMinutes", 15);
        ReflectionTestUtils.setField(underTest, "gcpTimeoutInMinutes", 16);
        ReflectionTestUtils.setField(underTest, "defaultTimeoutInMinutes", 45);
    }

    @Test
    void testPollingInterval() {
        assertEquals(Duration.ofSeconds(30), underTest.getPollingInterval());
    }

    @ParameterizedTest
    @CsvSource({"AWS, false, 30", "AWS, true, 30", "aws, false, 30", "AZURE, true, 31", "AZURE, false, 15", "GCP, true, 16", "GCP, false, 16",
            "MOCK, false, 45"})
    void testTimeoutDependsOnTheProviderAndTheHaState(String cloudPlatform, boolean highAvailability, int expectedMinutes) {
        assertEquals(Duration.ofMinutes(expectedMinutes), underTest.getTimeout(cloudPlatform, highAvailability));
    }
}
