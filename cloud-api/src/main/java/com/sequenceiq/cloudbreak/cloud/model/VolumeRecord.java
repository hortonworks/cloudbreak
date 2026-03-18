package com.sequenceiq.cloudbreak.cloud.model;

public record VolumeRecord(
        String id,
        String device,
        Integer size,
        String type
) {
}
