package com.sequenceiq.distrox.api.v1.distrox.model.upgrade;

import java.util.List;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.image.ImageInfoV4Response;
import com.sequenceiq.flow.api.model.FlowIdentifier;

public record DistroXUpgradeV1Response(
        ImageInfoV4Response current,
        List<ImageInfoV4Response> upgradeCandidates,
        String reason,
        FlowIdentifier flowIdentifier) {

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
