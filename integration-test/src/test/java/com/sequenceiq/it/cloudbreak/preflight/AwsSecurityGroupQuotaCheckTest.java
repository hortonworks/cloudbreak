package com.sequenceiq.it.cloudbreak.preflight;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.springframework.test.util.ReflectionTestUtils;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;

public class AwsSecurityGroupQuotaCheckTest {

    private CloudProviderEndpointResolver endpointResolver;

    private AwsSecurityGroupQuotaCheck underTest;

    @BeforeMethod
    public void setUp() {
        endpointResolver = mock(CloudProviderEndpointResolver.class);

        underTest = new AwsSecurityGroupQuotaCheck();
        ReflectionTestUtils.setField(underTest, "endpointResolver", endpointResolver);
    }

    @Test
    public void testCheckSkippedWhenNotAws() {
        when(endpointResolver.activeCloudPlatform()).thenReturn(CloudPlatform.GCP);

        PreflightCheckResult result = underTest.check();

        assertThat(result.isSkipped()).isTrue();
    }

    @Test
    public void testIsHeadroomBelowThresholdFlagsLowHeadroom() {
        assertThat(AwsSecurityGroupQuotaCheck.isHeadroomBelowThreshold(2500, 2480, 25)).isTrue();
    }

    @Test
    public void testIsHeadroomBelowThresholdPassesWithEnoughHeadroom() {
        assertThat(AwsSecurityGroupQuotaCheck.isHeadroomBelowThreshold(2500, 1000, 25)).isFalse();
    }
}
