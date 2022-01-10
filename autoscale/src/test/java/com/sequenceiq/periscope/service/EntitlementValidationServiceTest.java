package com.sequenceiq.periscope.service;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.powermock.reflect.Whitebox;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;

@RunWith(MockitoJUnitRunner.class)
public class EntitlementValidationServiceTest {

    private static final String TEST_ACCOUNT_ID = "accid";

    private static final String TEST_USER_CRN = String.format("crn:cdp:iam:us-west-1:%s:user:mockuser@cloudera.com", TEST_ACCOUNT_ID);

    @InjectMocks
    private EntitlementValidationService underTest;

    @Mock
    private EntitlementService entitlementService;

    @Before
    public void setUp() {
        Whitebox.setInternalState(underTest, "entitlementCheckEnabled", true);
    }

    @Test
    public void testWhenEntitlementCheckDisabledThenEntitled() {
        Whitebox.setInternalState(underTest, "entitlementCheckEnabled", false);
        boolean entitled = underTest.autoscalingEntitlementEnabled(TEST_ACCOUNT_ID, "AWS");
        assertTrue("entitled should be true when entitlementCheckDisabled", entitled);
    }

    @Test
    public void testWhenAWSAndEntitled() {
        when(entitlementService.awsAutoScalingEnabled(anyString())).thenReturn(true);

        boolean entitled = underTest.autoscalingEntitlementEnabled(TEST_ACCOUNT_ID, "AWS");
        assertTrue("isEntitled should be true when entitlement found", entitled);
    }

    @Test
    public void testWhenAWSAndNotEntitled() {
        when(entitlementService.awsAutoScalingEnabled(anyString())).thenReturn(false);

        boolean entitled = underTest.autoscalingEntitlementEnabled(TEST_ACCOUNT_ID, "AWS");
        assertFalse("isEntitled should be false when entitlement is not found", entitled);
    }

    @Test
    public void testWhenAWSStopStartAndEntitled() {
        when(entitlementService.awsAutoScalingEnabled(anyString())).thenReturn(true);
        when(entitlementService.awsStopStartScalingEnabled(anyString())).thenReturn(true);

        boolean entitled = underTest.stopStartAutoscalingEntitlementEnabled(TEST_ACCOUNT_ID, "AWS");
        assertTrue("isEntitled should be true when entitlement found", entitled);
    }

    @Test
    public void testWhenAWSStopStartAndNotEntitled() {
        when(entitlementService.awsAutoScalingEnabled(anyString())).thenReturn(false);

        boolean entitled = underTest.stopStartAutoscalingEntitlementEnabled(TEST_ACCOUNT_ID, "AWS");
        assertFalse("isEntitled should be false when entitlement is not found", entitled);
    }

    @Test
    public void testWhenAWSEntitledAndNotStopStart() {
        when(entitlementService.awsAutoScalingEnabled(anyString())).thenReturn(true);
        when(entitlementService.awsStopStartScalingEnabled(anyString())).thenReturn(false);

        boolean entitled = underTest.stopStartAutoscalingEntitlementEnabled(TEST_ACCOUNT_ID, "AWS");
        assertFalse("isEntitled should be false when entitlement is not found", entitled);
    }

    @Test
    public void testWhenAzureAndEntitled() {
        when(entitlementService.azureAutoScalingEnabled(anyString())).thenReturn(true);

        boolean entitled = underTest.autoscalingEntitlementEnabled(TEST_ACCOUNT_ID, "AZURE");
        assertTrue("isEntitled should be true when entitlement found", entitled);
    }

    @Test
    public void testWhenAzureAndNotEntitled() {
        when(entitlementService.azureAutoScalingEnabled(anyString())).thenReturn(false);

        boolean entitled = underTest.autoscalingEntitlementEnabled(TEST_ACCOUNT_ID, "AZURE");
        assertFalse("isEntitled should be false when entitlement is not found", entitled);
    }

    @Test
    public void testWhenYarnAndAlwaysAllowed() {
        boolean entitled = underTest.autoscalingEntitlementEnabled(TEST_ACCOUNT_ID, "YARN");
        assertTrue("isEntitled should be true when entitlement always allowed", entitled);
    }

    @Test
    public void testWhenGCPAndNotEntitled() {
        boolean entitled = underTest.autoscalingEntitlementEnabled(TEST_ACCOUNT_ID, "GCP");
        assertFalse("isEntitled should be false when entitlement is not defined", entitled);
    }
}
