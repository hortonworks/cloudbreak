package com.sequenceiq.it.cloudbreak.preflight;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import jakarta.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.api.services.compute.Compute;
import com.google.api.services.compute.model.Quota;
import com.google.api.services.compute.model.Region;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.it.cloudbreak.cloud.v4.gcp.GcpProperties;
import com.sequenceiq.it.cloudbreak.util.gcp.client.GcpClient;

@Component
public class GcpComputeQuotaCheck extends GcpClient implements PreflightCheck {

    private static final String REGIONAL_INTERNAL_LB_BACKEND_SERVICES = "REGIONAL_INTERNAL_LB_BACKEND_SERVICES";

    @Value("${integrationtest.preflight.gcp.minQuotaHeadroom:10}")
    private int minQuotaHeadroom;

    @Inject
    private CloudProviderEndpointResolver endpointResolver;

    @Inject
    private GcpProperties gcpProperties;

    @Override
    public PreflightCheckResult check() {
        if (CloudPlatform.GCP != endpointResolver.activeCloudPlatform()) {
            return PreflightCheckResult.skip(getName(), "not a GCP run");
        }
        String region = gcpProperties.getRegion();
        try {
            List<Quota> quotas = fetchRegionQuotas(region);
            Optional<Quota> backendServicesQuota = findQuota(quotas, REGIONAL_INTERNAL_LB_BACKEND_SERVICES);
            if (backendServicesQuota.isEmpty()) {
                return PreflightCheckResult.pass(getName(), "no " + REGIONAL_INTERNAL_LB_BACKEND_SERVICES + " quota reported for " + region);
            }
            if (isHeadroomBelowThreshold(backendServicesQuota.get(), minQuotaHeadroom)) {
                return PreflightCheckResult.fail(getName(), buildFailureMessage(region, backendServicesQuota.get(), minQuotaHeadroom));
            }
            return PreflightCheckResult.pass(getName(), REGIONAL_INTERNAL_LB_BACKEND_SERVICES + " quota OK in " + region);
        } catch (IOException e) {
            return PreflightCheckResult.fail(getName(), "could not fetch GCP compute quotas for " + region + ": " + e.getMessage());
        }
    }

    @Override
    public String getName() {
        return "gcp-compute-quota";
    }

    private List<Quota> fetchRegionQuotas(String region) throws IOException {
        Compute compute = buildCompute();
        Region regionInfo = compute.regions().get(getProjectId(), region).execute();
        return regionInfo.getQuotas();
    }

    static Optional<Quota> findQuota(List<Quota> quotas, String metric) {
        if (quotas == null) {
            return Optional.empty();
        }
        return quotas.stream().filter(quota -> metric.equals(quota.getMetric())).findFirst();
    }

    static boolean isHeadroomBelowThreshold(Quota quota, int minHeadroom) {
        return quota.getLimit() - quota.getUsage() < minHeadroom;
    }

    private static String buildFailureMessage(String region, Quota quota, int minHeadroom) {
        return "GCP quota headroom below " + minHeadroom + " for " + quota.getMetric() + " in " + region
                + " (limit: " + quota.getLimit() + ", usage: " + quota.getUsage() + ")";
    }
}
