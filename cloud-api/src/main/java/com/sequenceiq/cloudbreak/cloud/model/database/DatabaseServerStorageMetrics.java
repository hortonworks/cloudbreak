package com.sequenceiq.cloudbreak.cloud.model.database;

/**
 * Normalized storage utilization of an external database server, as reported by the cloud provider's monitoring metrics.
 * Providers expose this differently (AWS reports free bytes, Azure a used percentage, GCP a utilization ratio), so the
 * common denominator surfaced here is {@code freeStoragePercentage}. {@code freeBytes} and {@code allocatedBytes} are
 * populated when the provider makes them available and may be {@code null} otherwise.
 *
 * @param freeStoragePercentage the percentage (0-100) of allocated storage that is still free; never {@code null}
 * @param freeBytes             the free storage in bytes, or {@code null} if the provider does not report it
 * @param allocatedBytes        the allocated storage in bytes, or {@code null} if the provider does not report it
 */
public record DatabaseServerStorageMetrics(
        Double freeStoragePercentage,
        Long freeBytes,
        Long allocatedBytes) {
}
