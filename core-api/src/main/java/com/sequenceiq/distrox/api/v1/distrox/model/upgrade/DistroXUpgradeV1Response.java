package com.sequenceiq.distrox.api.v1.distrox.model.upgrade;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.image.ImageInfoV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.upgrade.UpgradeOptionV4Response;
import com.sequenceiq.flow.api.model.FlowIdentifier;

public class DistroXUpgradeV1Response {

    private ImageInfoV4Response current;

    private List<ImageInfoV4Response> upgradeCandidates;

    private String reason;

    private FlowIdentifier flowIdentifier;

    public DistroXUpgradeV1Response() {
    }

    public DistroXUpgradeV1Response(UpgradeOptionV4Response upgradeResponse) {
        current = upgradeResponse.getCurrent();
        upgradeCandidates = upgradeResponse.getUpgrade() != null ? List.of(upgradeResponse.getUpgrade()) : List.of();
        reason = upgradeResponse.getReason();
    }

    public DistroXUpgradeV1Response(ImageInfoV4Response current, List<ImageInfoV4Response> upgradeCandidates, String reason, FlowIdentifier flowIdentifier) {
        this.current = current;
        this.upgradeCandidates = upgradeCandidates;
        this.reason = reason;
        this.flowIdentifier = flowIdentifier;
    }

    public DistroXUpgradeV1Response(String reason, FlowIdentifier flowIdentifier) {
        this.reason = reason;
        this.flowIdentifier = flowIdentifier;
    }

    public ImageInfoV4Response getCurrent() {
        return current;
    }

    public void setCurrent(ImageInfoV4Response current) {
        this.current = current;
    }

    public List<ImageInfoV4Response> getUpgradeCandidates() {
        return upgradeCandidates;
    }

    public void setUpgradeCandidates(List<ImageInfoV4Response> upgradeCandidates) {
        this.upgradeCandidates = upgradeCandidates;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public void appendReason(String reason) {
        if (StringUtils.isNotEmpty(this.reason)) {
            this.reason += reason;
        } else {
            setReason(reason);
        }
    }

    public FlowIdentifier getFlowIdentifier() {
        return flowIdentifier;
    }

    public void setFlowIdentifier(FlowIdentifier flowIdentifier) {
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
