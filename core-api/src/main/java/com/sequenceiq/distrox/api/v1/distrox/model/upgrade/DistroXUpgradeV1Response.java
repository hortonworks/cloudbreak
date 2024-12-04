package com.sequenceiq.distrox.api.v1.distrox.model.upgrade;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.image.ImageInfoV4Response;
import com.sequenceiq.flow.api.model.FlowIdentifier;

import io.swagger.v3.oas.annotations.media.Schema;

public record DistroXUpgradeV1Response(
        ImageInfoV4Response current,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<ImageInfoV4Response> upgradeCandidates,
        String reason,
        FlowIdentifier flowIdentifier) {

    public DistroXUpgradeV1Response(ImageInfoV4Response current, List<ImageInfoV4Response> upgradeCandidates, String reason, FlowIdentifier flowIdentifier) {
        this.current = current;
        this.upgradeCandidates = Objects.requireNonNullElse(upgradeCandidates, new ArrayList<>());
        this.reason = reason;
        this.flowIdentifier = flowIdentifier;
    }

    @Override
    public String toString() {
        return "DistroXUpgradeV1Response{" +
                "current=" + current +
                ", upgradeCandidates=" + upgradeCandidates +
                ", reason='" + reason + '\'' +
                ", flowIdentifier=" + flowIdentifier +
                '}';
    }
}
