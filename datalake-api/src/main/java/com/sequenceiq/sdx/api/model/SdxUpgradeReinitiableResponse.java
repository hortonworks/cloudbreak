package com.sequenceiq.sdx.api.model;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.upgrade.UpgradeReinitiableV4Response;
import com.sequenceiq.distrox.api.v1.distrox.model.upgrade.reinit.UpgradeReinitiateStatus;

import io.swagger.v3.oas.annotations.media.Schema;

public record SdxUpgradeReinitiableResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UpgradeReinitiateStatus status,
        String reason
) {
    public static SdxUpgradeReinitiableResponse from(UpgradeReinitiableV4Response upgradeReinitiableV4Response) {
        return new SdxUpgradeReinitiableResponse(upgradeReinitiableV4Response.status(), upgradeReinitiableV4Response.reason());
    }
}
