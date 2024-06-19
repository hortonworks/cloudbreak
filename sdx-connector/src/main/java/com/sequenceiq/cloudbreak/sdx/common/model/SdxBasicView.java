package com.sequenceiq.cloudbreak.sdx.common.model;

import com.sequenceiq.cloudbreak.auth.crn.Crn;

public record SdxBasicView(
        String name,
        String crn,
        String runtime,
        boolean razEnabled,
        Long created,
        String dbServerCrn) {

    public Crn getCrn() {
        return Crn.safeFromString(crn());
    }
}
