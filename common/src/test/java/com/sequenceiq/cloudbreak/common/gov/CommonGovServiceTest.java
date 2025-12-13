package com.sequenceiq.cloudbreak.common.gov;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
public class CommonGovServiceTest {

    @InjectMocks
    private CommonGovService commonGovService;

    @BeforeEach
    void setUp() {
        // Set the value of minimalGovRuntimeVersion using ReflectionTestUtils
        ReflectionTestUtils.setField(commonGovService, "minimalGovRuntimeVersion", "7.2.18");
    }

    @Test
    void testGovCloudCompatibleVersionWhenCurrentVersionIsCompatible() {
        String currentVersion = "7.2.20";
        assertTrue(commonGovService.govCloudCompatibleVersion(currentVersion));
    }

    @Test
    void testGovCloudCompatibleVersionWhenCurrentVersionIsNotCompatible() {
        String currentVersion = "7.1.0";
        assertFalse(commonGovService.govCloudCompatibleVersion(currentVersion));
    }

    @Test
    void testGovCloudDeploymentWhenAwsGovEnabledAndAwsNotEnabled() {
        Set<String> enabledGovPlatforms = new HashSet<>();
        enabledGovPlatforms.add("AWS");

        Set<String> enabledPlatforms = new HashSet<>();

        assertTrue(commonGovService.govCloudDeployment(enabledGovPlatforms, enabledPlatforms));
    }

    @Test
    void testGovCloudDeploymentWhenAwsGovEnabledAndAwsEnabled() {
        Set<String> enabledGovPlatforms = new HashSet<>();
        enabledGovPlatforms.add("AWS");

        Set<String> enabledPlatforms = new HashSet<>();
        enabledPlatforms.add("AWS");

        assertFalse(commonGovService.govCloudDeployment(enabledGovPlatforms, enabledPlatforms));
    }

    @Test
    void testGovCloudDeploymentWhenAwsGovNotEnabled() {
        Set<String> enabledGovPlatforms = new HashSet<>();

        Set<String> enabledPlatforms = new HashSet<>();
        enabledPlatforms.add("AWS");

        assertFalse(commonGovService.govCloudDeployment(enabledGovPlatforms, enabledPlatforms));
    }
}
