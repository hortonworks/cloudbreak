package com.sequenceiq.cloudbreak.cloud.notification;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;

/**
 * When the Cloud provider rejects an instance type as unavailable / capacity-limited and the
 * fallback feature (CDP_FALLBACK_INSTANCETYPE) switches to a fallback instance type, the connector
 * uses this interface to inform Cloudbreak so a user-visible notification can be emitted.
 * <br>
 * Duplicate reports for the same {@code (stack, group, originalType, fallbackType)} tuple are
 * absorbed by the implementation, so callers on hot paths (per-instance retry loops in AWS/GCP,
 * ARM deployment retries in Azure) may call these methods freely.
 */
public interface InstanceTypeFallbackReporter {

    /**
     * Report that a group's provisioning is being retried with a different instance type.
     *
     * @param cloudContext   context containing the stack id and account
     * @param instanceGroup  the instance group whose flavor is being switched
     * @param originalType   the originally requested instance type
     * @param fallbackType   the fallback instance type to be tried next
     * @param reasonSummary  short human-readable reason extracted from the cloud error
     *                       (e.g. {@code "InsufficientInstanceCapacity"}, {@code "SkuNotAvailable"},
     *                       {@code "ZONE_RESOURCE_POOL_EXHAUSTED"}); may be {@code null}
     */
    void reportFallback(CloudContext cloudContext, String instanceGroup, String originalType, String fallbackType, String reasonSummary);

    /**
     * Report that the fallback chain for a group has been exhausted and provisioning is about to fail.
     *
     * @param cloudContext   context containing the stack id and account
     * @param instanceGroup  the instance group that exhausted its fallback chain
     * @param originalType   the originally requested instance type
     * @param reasonSummary  short human-readable reason extracted from the last cloud error;
     *                       may be {@code null}
     */
    void reportFallbackExhausted(CloudContext cloudContext, String instanceGroup, String originalType, String reasonSummary);

}
