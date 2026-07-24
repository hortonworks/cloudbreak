package com.sequenceiq.cloudbreak.cloud.azure.connector.resource;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public enum AzureDeploymentCapacityError {

    SKU_NOT_AVAILABLE("SkuNotAvailable"),
    ALLOCATION_FAILED("AllocationFailed"),
    ZONAL_ALLOCATION_FAILED("ZonalAllocationFailed"),
    OVERCONSTRAINED_ALLOCATION_REQUEST("OverconstrainedAllocationRequest"),
    QUOTA_EXCEEDED("QuotaExceeded"),
    SUBSCRIPTION_QUOTA_REACHED("SubscriptionQuotaReached");

    private static final Set<String> ALL_CODES_LOWERCASE = Arrays.stream(values())
            .map(e -> e.code.toLowerCase(Locale.ROOT))
            .collect(Collectors.toUnmodifiableSet());

    private final String code;

    AzureDeploymentCapacityError(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static boolean isCapacityErrorCode(String code) {
        return code != null && ALL_CODES_LOWERCASE.contains(code.toLowerCase(Locale.ROOT));
    }
}
