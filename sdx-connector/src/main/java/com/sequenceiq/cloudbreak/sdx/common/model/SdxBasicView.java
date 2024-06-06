package com.sequenceiq.cloudbreak.sdx.common.model;

public record SdxBasicView(
        String name,
        String crn,
        String runtime,
        boolean razEnabled,
        Long created,
        String dbServerCrn) {
}
