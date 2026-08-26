package com.sequenceiq.cloudbreak.cloud.azure.validator;

import java.util.Locale;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.azure.AzureDiskType;

@Service
public class AzurePremiumValidatorService {

    private static final Set<Character> PREMIUM_ELIGIBLE_FAMILIES = Set.of('d', 'e', 'f', 'g', 'l', 'm');

    public boolean validPremiumConfiguration(String flavor) {
        return isPremiumStorageSupportedByInstance(flavor);
    }

    public boolean premiumDiskTypeConfigured(AzureDiskType diskType) {
        return AzureDiskType.PREMIUM_LOCALLY_REDUNDANT.equals(diskType);
    }

    private boolean isPremiumStorageSupportedByInstance(String flavor) {
        String[] parts = flavor.split("_");
        if (parts.length < 2) {
            return false;
        }
        String features = parts[1]
                .replaceAll("[0-9]", "")
                .replaceAll("-", "")
                .toLowerCase(Locale.ROOT);
        return !features.isEmpty()
                && PREMIUM_ELIGIBLE_FAMILIES.contains(features.charAt(0))
                && features.endsWith("s");
    }
}
