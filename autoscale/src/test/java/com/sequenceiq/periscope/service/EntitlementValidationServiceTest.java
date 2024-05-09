package com.sequenceiq.periscope.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
