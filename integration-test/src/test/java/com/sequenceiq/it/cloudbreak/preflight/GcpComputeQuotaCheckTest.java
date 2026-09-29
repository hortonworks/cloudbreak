package com.sequenceiq.it.cloudbreak.preflight;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.springframework.test.util.ReflectionTestUtils;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.api.services.compute.model.Quota;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;

public class GcpComputeQuotaCheckTest {

    private CloudProviderEndpointResolver endpointResolver;

    private GcpComputeQuotaCheck underTest;

    @BeforeMethod
    public void setUp() {
        endpointResolver = mock(CloudProviderEndpointResolver.class);

        underTest = new GcpComputeQuotaCheck();
        ReflectionTestUtils.setField(underTest, "endpointResolver", endpointResolver);
    }

    @Test
    public void testCheckSkippedWhenNotGcp() {
        when(endpointResolver.activeCloudPlatform()).thenReturn(CloudPlatform.AWS);

        PreflightCheckResult result = underTest.check();

        assertThat(result.isSkipped()).isTrue();
    }

    @Test
    public void testFindQuotaReturnsMatchingMetric() {
        Quota backendServices = mockQuota("REGIONAL_INTERNAL_LB_BACKEND_SERVICES", 150, 140);
        Quota other = mockQuota("CPUS", 1000, 10);

        Optional<Quota> found = GcpComputeQuotaCheck.findQuota(List.of(other, backendServices), "REGIONAL_INTERNAL_LB_BACKEND_SERVICES");

        assertThat(found).isPresent();
        assertThat(found.get().getMetric()).isEqualTo("REGIONAL_INTERNAL_LB_BACKEND_SERVICES");
    }

    @Test
    public void testFindQuotaReturnsEmptyWhenMetricAbsent() {
        Quota other = mockQuota("CPUS", 1000, 10);

        Optional<Quota> found = GcpComputeQuotaCheck.findQuota(List.of(other), "REGIONAL_INTERNAL_LB_BACKEND_SERVICES");

        assertThat(found).isEmpty();
    }

    @Test
    public void testFindQuotaReturnsEmptyWhenListNull() {
        Optional<Quota> found = GcpComputeQuotaCheck.findQuota(null, "REGIONAL_INTERNAL_LB_BACKEND_SERVICES");

        assertThat(found).isEmpty();
    }

    @Test
    public void testIsHeadroomBelowThresholdFlagsLowHeadroom() {
        Quota quota = mockQuota("REGIONAL_INTERNAL_LB_BACKEND_SERVICES", 150, 145);

        assertThat(GcpComputeQuotaCheck.isHeadroomBelowThreshold(quota, 10)).isTrue();
    }

    @Test
    public void testIsHeadroomBelowThresholdPassesWithEnoughHeadroom() {
        Quota quota = mockQuota("REGIONAL_INTERNAL_LB_BACKEND_SERVICES", 150, 50);

        assertThat(GcpComputeQuotaCheck.isHeadroomBelowThreshold(quota, 10)).isFalse();
    }

    private Quota mockQuota(String metric, double limit, double usage) {
        Quota quota = mock(Quota.class);
        when(quota.getMetric()).thenReturn(metric);
        when(quota.getLimit()).thenReturn(limit);
        when(quota.getUsage()).thenReturn(usage);
        return quota;
    }
}
