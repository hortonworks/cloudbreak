package com.sequenceiq.it.cloudbreak.preflight;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.azure.core.util.Context;
import com.azure.resourcemanager.AzureResourceManager;
import com.azure.resourcemanager.compute.fluent.models.ResourceSkuInner;
import com.azure.resourcemanager.compute.models.ComputeUsage;
import com.azure.resourcemanager.compute.models.ResourceSkuRestrictions;
import com.azure.resourcemanager.compute.models.ResourceSkuRestrictionsType;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.it.cloudbreak.cloud.v4.azure.AzureProperties;

@Component
public class AzureComputeCapacityCheck implements PreflightCheck {

    @Value("${integrationtest.preflight.azure.minQuotaHeadroom:8}")
    private int minQuotaHeadroom;

    @Inject
    private CloudProviderEndpointResolver endpointResolver;

    @Inject
    private AzureProperties azureProperties;

    @Inject
    private AzureResourceManager azure;

    @Override
    public PreflightCheckResult check() {
        if (CloudPlatform.AZURE != endpointResolver.activeCloudPlatform()) {
            return PreflightCheckResult.skip(getName(), "not an Azure run");
        }
        Set<String> vmSizes = collectConfiguredVmSizes(azureProperties);
        String region = normalizeAzureRegionName(azureProperties.getRegion());
        List<ResourceSkuInner> skus = listVmSkus(region);
        List<ComputeUsage> usages = listUsages(region);

        List<String> restrictedSizes = findRestrictedSizes(skus, vmSizes, region);
        List<String> quotaExhaustedFamilies = findQuotaExhaustedFamilies(skus, usages, vmSizes, minQuotaHeadroom);

        if (restrictedSizes.isEmpty() && quotaExhaustedFamilies.isEmpty()) {
            return PreflightCheckResult.pass(getName(), "capacity OK for " + vmSizes + " in " + region);
        }
        return PreflightCheckResult.fail(getName(), buildFailureMessage(region, restrictedSizes, quotaExhaustedFamilies, minQuotaHeadroom));
    }

    @Override
    public String getName() {
        return "azure-compute-capacity";
    }

    private List<ResourceSkuInner> listVmSkus(String region) {
        String criteria = "location eq '" + region + "'";
        List<ResourceSkuInner> skus = new ArrayList<>();
        azure.virtualMachines().manager().serviceClient().getResourceSkus().list(criteria, null, Context.NONE)
                .forEach(skus::add);
        return skus;
    }

    private List<ComputeUsage> listUsages(String region) {
        List<ComputeUsage> usages = new ArrayList<>();
        azure.computeUsages().listByRegion(region).forEach(usages::add);
        return usages;
    }

    static Set<String> collectConfiguredVmSizes(AzureProperties properties) {
        Set<String> sizes = new HashSet<>();
        sizes.add(properties.getVerticalScale().getDatalake().getInstanceType());
        sizes.add(properties.getVerticalScale().getDatahub().getInstanceType());
        sizes.add(properties.getVerticalScale().getFreeipa().getInstanceType());
        sizes.add(properties.getInstance().getTypes().getDefault());
        sizes.add(properties.getInstance().getTypes().getDatalake().get("master"));
        sizes.add(properties.getInstance().getTypes().getDatalake().get("idbroker"));
        sizes.add(properties.getInstance().getTypes().getDatalake().get("custom"));
        sizes.add(properties.getStorageOptimizedInstance().getType());
        sizes.add(properties.getArm64Instance().getType());
        sizes.add(properties.getFreeipa().getInstanceType());
        sizes.removeIf(StringUtils::isBlank);
        return sizes;
    }

    static List<String> findRestrictedSizes(List<ResourceSkuInner> skus, Set<String> vmSizes, String region) {
        List<String> restricted = new ArrayList<>();
        for (ResourceSkuInner sku : skus) {
            if (!vmSizes.contains(sku.name()) || sku.restrictions() == null) {
                continue;
            }
            boolean restrictedHere = sku.restrictions().stream().anyMatch(restriction -> isLocationRestricted(restriction, region));
            if (restrictedHere) {
                restricted.add(sku.name());
            }
        }
        return restricted;
    }

    private static boolean isLocationRestricted(ResourceSkuRestrictions restriction, String region) {
        return ResourceSkuRestrictionsType.LOCATION.equals(restriction.type())
                && restriction.restrictionInfo() != null
                && restriction.restrictionInfo().locations() != null
                && restriction.restrictionInfo().locations().stream().anyMatch(region::equalsIgnoreCase);
    }

    static List<String> findQuotaExhaustedFamilies(List<ResourceSkuInner> skus, List<ComputeUsage> usages, Set<String> vmSizes, int minHeadroom) {
        Set<String> families = new HashSet<>();
        for (ResourceSkuInner sku : skus) {
            if (vmSizes.contains(sku.name()) && sku.family() != null) {
                families.add(sku.family());
            }
        }
        List<String> exhausted = new ArrayList<>();
        for (ComputeUsage usage : usages) {
            if (families.contains(usage.name().value()) && usage.limit() - usage.currentValue() < minHeadroom) {
                exhausted.add(usage.name().value());
            }
        }
        return exhausted;
    }

    private static String buildFailureMessage(String region, List<String> restrictedSizes, List<String> quotaExhaustedFamilies, int minHeadroom) {
        StringBuilder message = new StringBuilder("Azure compute capacity issue in ").append(region).append(": ");
        if (!restrictedSizes.isEmpty()) {
            message.append("restricted VM sizes: ").append(restrictedSizes).append("; ");
        }
        if (!quotaExhaustedFamilies.isEmpty()) {
            message.append("quota headroom below ").append(minHeadroom).append(" for families: ").append(quotaExhaustedFamilies);
        }
        return message.toString();
    }

    static String normalizeAzureRegionName(String regionLabel) {
        return regionLabel == null ? null : regionLabel.toLowerCase(Locale.ROOT).replace(" ", "");
    }
}
