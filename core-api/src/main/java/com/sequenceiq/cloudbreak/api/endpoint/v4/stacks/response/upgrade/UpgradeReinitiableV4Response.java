package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.upgrade;

import com.sequenceiq.distrox.api.v1.distrox.model.upgrade.reinit.UpgradeReinitiateStatus;

import io.swagger.v3.oas.annotations.media.Schema;

public record UpgradeReinitiableV4Response(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UpgradeReinitiateStatus status,
        String reason
) {

    public UpgradeReinitiableV4Response(UpgradeReinitiateStatus status) {
        this(status, null);
    }
}
