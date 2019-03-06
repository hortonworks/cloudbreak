package com.sequenceiq.cloudbreak.cloud.azure;

import java.util.Arrays;

import org.springframework.stereotype.Service;

@Service
public class AzurePremiumValidatorService {

    public boolean validPremiumConfiguration(String flavor) {
        return isPremiumStorageSupportedByInstance(flavor);
    }

    public boolean premiumDiskTypeConfigured(AzureDiskType diskType) {
        return AzureDiskType.PREMIUM_LOCALLY_REDUNDANT.equals(diskType);
    }

    private boolean isPremiumStorageSupportedByInstance(String flavor) {
        String segment = flavor.split("_")[1];
        String transformedSegment = segment
                .replaceAll("[0-9]", "")
                .replaceAll("-", "")
                .toLowerCase();

        String transformedFlavor = flavor.replaceAll(segment, transformedSegment).toLowerCase();
        String[] items = { "_ds", "_ls", "_gs", "_fs", "_es_v3" };
        return Arrays.stream(items).parallel().anyMatch(transformedFlavor::contains);
    }
}
