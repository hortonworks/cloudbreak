package com.sequenceiq.it.cloudbreak.preflight;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.api.services.compute.model.Quota;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;

@ExtendWith(MockitoExtension.class)
class GcpComputeQuotaCheckTest {

    @Mock
    private CloudProviderEndpointResolver endpointResolver;

    @InjectMocks
    private GcpComputeQuotaCheck underTest;

    @Test
    void testCheckSkippedWhenNotGcp() {
        when(endpointResolver.activeCloudPlatform()).thenReturn(CloudPlatform.AWS);

        PreflightCheckResult result = underTest.check();

        assertThat(result.isSkipped()).isTrue();
    }

    @Test
    void testFindQuotaReturnsMatchingMetric() {
        Quota backendServices = createQuota("REGIONAL_INTERNAL_LB_BACKEND_SERVICES", 150, 140);
        Quota other = createQuota("CPUS", 1000, 10);

        Optional<Quota> found = GcpComputeQuotaCheck.findQuota(List.of(other, backendServices), "REGIONAL_INTERNAL_LB_BACKEND_SERVICES");

        assertThat(found).isPresent();
        assertThat(found.get().getMetric()).isEqualTo("REGIONAL_INTERNAL_LB_BACKEND_SERVICES");
    }

    @Test
    void testFindQuotaReturnsEmptyWhenMetricAbsent() {
        Quota other = createQuota("CPUS", 1000, 10);

        Optional<Quota> found = GcpComputeQuotaCheck.findQuota(List.of(other), "REGIONAL_INTERNAL_LB_BACKEND_SERVICES");

        assertThat(found).isEmpty();
    }

    @Test
    void testFindQuotaReturnsEmptyWhenListNull() {
        Optional<Quota> found = GcpComputeQuotaCheck.findQuota(null, "REGIONAL_INTERNAL_LB_BACKEND_SERVICES");

        assertThat(found).isEmpty();
    }

    @Test
    void testIsHeadroomBelowThresholdFlagsLowHeadroom() {
        Quota quota = createQuota("REGIONAL_INTERNAL_LB_BACKEND_SERVICES", 150, 145);

        assertThat(GcpComputeQuotaCheck.isHeadroomBelowThreshold(quota, 10)).isTrue();
    }

    @Test
    void testIsHeadroomBelowThresholdPassesWithEnoughHeadroom() {
        Quota quota = createQuota("REGIONAL_INTERNAL_LB_BACKEND_SERVICES", 150, 50);

        assertThat(GcpComputeQuotaCheck.isHeadroomBelowThreshold(quota, 10)).isFalse();
    }

    private Quota createQuota(String metric, double limit, double usage) {
        return new Quota().setMetric(metric).setLimit(limit).setUsage(usage);
    }
}
