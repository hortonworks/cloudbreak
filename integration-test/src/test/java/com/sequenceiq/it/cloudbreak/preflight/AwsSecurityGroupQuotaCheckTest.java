package com.sequenceiq.it.cloudbreak.preflight;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;

@ExtendWith(MockitoExtension.class)
class AwsSecurityGroupQuotaCheckTest {

    @Mock
    private CloudProviderEndpointResolver endpointResolver;

    @InjectMocks
    private AwsSecurityGroupQuotaCheck underTest;

    @Test
    void testCheckSkippedWhenNotAws() {
        when(endpointResolver.activeCloudPlatform()).thenReturn(CloudPlatform.GCP);

        PreflightCheckResult result = underTest.check();

        assertThat(result.isSkipped()).isTrue();
    }

    @Test
    void testIsHeadroomBelowThresholdFlagsLowHeadroom() {
        assertThat(AwsSecurityGroupQuotaCheck.isHeadroomBelowThreshold(2500, 2480, 25)).isTrue();
    }

    @Test
    void testIsHeadroomBelowThresholdPassesWithEnoughHeadroom() {
        assertThat(AwsSecurityGroupQuotaCheck.isHeadroomBelowThreshold(2500, 1000, 25)).isFalse();
    }
}
