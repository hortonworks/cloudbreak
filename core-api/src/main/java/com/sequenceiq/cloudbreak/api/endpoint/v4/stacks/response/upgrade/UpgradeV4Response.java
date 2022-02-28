package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.upgrade;

import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.image.ImageInfoV4Response;
import com.sequenceiq.flow.api.model.FlowIdentifier;

public class UpgradeV4Response {

    private ImageInfoV4Response current;

    private List<ImageInfoV4Response> upgradeCandidates;

    private String reason;

    private FlowIdentifier flowIdentifier;

    private boolean replaceVms;

    public UpgradeV4Response() {
    }

    public UpgradeV4Response(String reason, FlowIdentifier flowIdentifier) {
        this.reason = reason;
        this.flowIdentifier = flowIdentifier;
    }

    public UpgradeV4Response(ImageInfoV4Response current,
            List<ImageInfoV4Response> upgradeCandidates, String reason) {
        this.current = current;
        this.upgradeCandidates = upgradeCandidates;
        this.reason = reason;
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
            this.reason += " " + reason;
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

    public boolean isReplaceVms() {
        return replaceVms;
    }

    public void setReplaceVms(boolean replaceVms) {
        this.replaceVms = replaceVms;
    }

    @Override
    public String toString() {
        return "UpgradeOptionsV4Response{" +
                "current=" + current +
                ", upgradeCandidates=" + upgradeCandidates +
                ", reason='" + reason + '\'' +
                ", replaceVms='" + replaceVms + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        UpgradeV4Response that = (UpgradeV4Response) o;
        return replaceVms == that.replaceVms && Objects.equals(current, that.current) && Objects.equals(upgradeCandidates, that.upgradeCandidates)
                && Objects.equals(reason, that.reason) && Objects.equals(flowIdentifier, that.flowIdentifier);
    }

    @Override
    public int hashCode() {
        return Objects.hash(current, upgradeCandidates, reason, flowIdentifier, replaceVms);
    }
}
