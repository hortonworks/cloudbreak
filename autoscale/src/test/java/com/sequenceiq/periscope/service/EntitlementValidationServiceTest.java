package com.sequenceiq.periscope.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;

@ExtendWith(MockitoExtension.class)
class EntitlementValidationServiceTest {

    private static final String TEST_ACCOUNT_ID = "accid";

    @InjectMocks
    private EntitlementValidationService underTest;

    @Mock
    private EntitlementService entitlementService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(underTest, "entitlementCheckEnabled", true);
        ReflectionTestUtils.setField(underTest, "skipEntitlementCheckPlatforms", Set.of("YARN", "MOCK"));
    }

    @Test
    void testWhenEntitlementCheckDisabledThenEntitled() {
        ReflectionTestUtils.setField(underTest, "entitlementCheckEnabled", false);
        boolean entitled = underTest.autoscalingEntitlementEnabled(TEST_ACCOUNT_ID, "AWS");
        assertTrue(entitled, "entitled should be true when entitlementCheckDisabled");
    }

    @Test
    void testWhenAWSAndEntitled() {
        when(entitlementService.awsAutoScalingEnabled(anyString())).thenReturn(true);

        boolean entitled = underTest.autoscalingEntitlementEnabled(TEST_ACCOUNT_ID, "AWS");
        assertTrue(entitled, "isEntitled should be true when entitlement found");
    }

    @Test
    void testWhenAWSAndNotEntitled() {
        when(entitlementService.awsAutoScalingEnabled(anyString())).thenReturn(false);

        boolean entitled = underTest.autoscalingEntitlementEnabled(TEST_ACCOUNT_ID, "AWS");
        assertFalse(entitled, "isEntitled should be false when entitlement is not found");
    }

    @Test
    void testWhenAWSStopStartAndEntitled() {
        when(entitlementService.awsAutoScalingEnabled(anyString())).thenReturn(true);
        when(entitlementService.awsStopStartScalingEnabled(anyString())).thenReturn(true);

        boolean entitled = underTest.stopStartAutoscalingEntitlementEnabled(TEST_ACCOUNT_ID, "AWS");
        assertTrue(entitled, "isEntitled should be true when entitlement found");
    }

    @Test
    void testWhenAWSStopStartAndNotEntitled() {
        when(entitlementService.awsAutoScalingEnabled(anyString())).thenReturn(false);

        boolean entitled = underTest.stopStartAutoscalingEntitlementEnabled(TEST_ACCOUNT_ID, "AWS");
        assertFalse(entitled, "isEntitled should be false when entitlement is not found");
    }

    @Test
    void testWhenAWSEntitledAndNotStopStart() {
        when(entitlementService.awsAutoScalingEnabled(anyString())).thenReturn(true);
        when(entitlementService.awsStopStartScalingEnabled(anyString())).thenReturn(false);

        boolean entitled = underTest.stopStartAutoscalingEntitlementEnabled(TEST_ACCOUNT_ID, "AWS");
        assertFalse(entitled, "isEntitled should be false when entitlement is not found");
    }

    @Test
    void testWhenAzureAndEntitled() {
        when(entitlementService.azureAutoScalingEnabled(anyString())).thenReturn(true);

        boolean entitled = underTest.autoscalingEntitlementEnabled(TEST_ACCOUNT_ID, "AZURE");
        assertTrue(entitled, "isEntitled should be true when entitlement found");
    }

    @Test
    void testWhenAzureAndNotEntitled() {
        when(entitlementService.azureAutoScalingEnabled(anyString())).thenReturn(false);

        boolean entitled = underTest.autoscalingEntitlementEnabled(TEST_ACCOUNT_ID, "AZURE");
        assertFalse(entitled, "isEntitled should be false when entitlement is not found");
    }

    @Test
    void testWhenYarnAndAlwaysAllowed() {
        boolean entitled = underTest.autoscalingEntitlementEnabled(TEST_ACCOUNT_ID, "YARN");
        assertTrue(entitled, "isEntitled should be true when entitlement always allowed");
    }

    @Test
    void testWhenMockAndAlwaysAllowed() {
        boolean entitled = underTest.autoscalingEntitlementEnabled(TEST_ACCOUNT_ID, "MOCK");
        assertTrue(entitled, "isEntitled should be true when entitlement always allowed");
    }

    @Test
    void testWhenGCPAndNotEntitled() {
        boolean entitled = underTest.autoscalingEntitlementEnabled(TEST_ACCOUNT_ID, "GCP");
        assertFalse(entitled, "isEntitled should be false when entitlement is not defined");
    }
}
