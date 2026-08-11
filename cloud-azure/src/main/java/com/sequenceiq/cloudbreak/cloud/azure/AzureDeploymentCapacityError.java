package com.sequenceiq.cloudbreak.cloud.azure;

/**
 * ARM deployment error codes that signal the requested VM size cannot be used in the target location at this time.
 * These are the codes we react to when running the instance-type fallback loop in {@link AzureFallbackAwareDeploymentService}.
 *
 * Modelled after {@link AzureDeploymentMarketplaceError} so the matching logic stays consistent across error families.
 */
public enum AzureDeploymentCapacityError {

    SKU_NOT_AVAILABLE("SkuNotAvailable"),

    OVERCONSTRAINED_ALLOCATION_REQUEST("OverconstrainedAllocationRequest"),

    ALLOCATION_FAILED("AllocationFailed"),

    ZONAL_ALLOCATION_FAILED("ZonalAllocationFailed"),

    QUOTA_EXCEEDED("QuotaExceeded"),

    SUBSCRIPTION_QUOTA_REACHED("SubscriptionQuotaReached"),

    OPERATION_NOT_ALLOWED("OperationNotAllowed");

    private final String code;

    AzureDeploymentCapacityError(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
