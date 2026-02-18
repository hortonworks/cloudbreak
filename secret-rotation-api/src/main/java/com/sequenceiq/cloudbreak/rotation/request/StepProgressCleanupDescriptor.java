package com.sequenceiq.cloudbreak.rotation.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record StepProgressCleanupDescriptor(
        RotationSource rotationSource,
        StepProgressCleanupStatus status,
        String crn,
        String secretType) {

    public static StepProgressCleanupDescriptor of(RotationSource rotationSource, StepProgressCleanupStatus status, String crn, String secretType) {
        return new StepProgressCleanupDescriptor(rotationSource, status, crn, secretType);
    }
}
