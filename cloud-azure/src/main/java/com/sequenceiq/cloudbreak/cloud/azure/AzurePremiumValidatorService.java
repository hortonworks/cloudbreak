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
        String transformedFlavor = flavor.replaceAll("[0-9]", "").toLowerCase();
        String[] items = { "_ds", "_ls", "_gs", "_fs" };
        return Arrays.stream(items).parallel().anyMatch(transformedFlavor::contains);
    }
}
