package com.sequenceiq.cloudbreak.sdx.common.model;

public record SdxBasicView(
        String name,
        String crn,
        String runtime,
        String environmentCrn,
        boolean razEnabled,
        Long created,
        String dbServerCrn) {
}
