package com.sequenceiq.cloudbreak.cloud.model.database;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The live instance type related state of an external database server, as reported by the cloud provider. Used by the change
 * instance type flow to validate the request against the real values and to persist the values applied by the provider.
 *
 * @param instanceType           the provider specific instance type the database server currently runs on
 * @param storageSizeInGb        the allocated storage size in GB
 * @param highAvailability       whether the database server is highly available (Multi-AZ, zone redundant or regional) right now
 * @param modificationInProgress whether a modification is still being applied, normalized by the provider: AWS reports a status other
 *                               than available or storage-optimization, or pending modified values, Azure a state other than Ready or
 *                               an HA state other than Healthy or NotEnabled (servers without HA report NotEnabled), GCP a state other
 *                               than RUNNABLE
 */
public record DatabaseServerInstanceTypeState(
        @JsonProperty("instanceType") String instanceType,
        @JsonProperty("storageSizeInGb") Long storageSizeInGb,
        @JsonProperty("highAvailability") boolean highAvailability,
        @JsonProperty("modificationInProgress") boolean modificationInProgress) {
}
