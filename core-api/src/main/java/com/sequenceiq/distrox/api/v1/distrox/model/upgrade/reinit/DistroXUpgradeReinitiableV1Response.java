package com.sequenceiq.distrox.api.v1.distrox.model.upgrade.reinit;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.upgrade.UpgradeReinitiableV4Response;

import io.swagger.v3.oas.annotations.media.Schema;

public record DistroXUpgradeReinitiableV1Response(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UpgradeReinitiateStatus status,
        String reason
) {
    public static DistroXUpgradeReinitiableV1Response from(UpgradeReinitiableV4Response upgradeReinitiableV4Response) {
        return new DistroXUpgradeReinitiableV1Response(upgradeReinitiableV4Response.status(), upgradeReinitiableV4Response.reason());
    }
}
