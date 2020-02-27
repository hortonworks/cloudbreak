package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response;

import java.util.List;

public class UpgradeOptionsV4Response {

    private ImageInfoV4Response current;

    private List<ImageInfoV4Response> upgradeCandidates;

    private String reason;

    public UpgradeOptionsV4Response() {
    }

    public UpgradeOptionsV4Response(ImageInfoV4Response current, List<ImageInfoV4Response> upgradeCandidates) {
        this.current = current;
        this.upgradeCandidates = upgradeCandidates;
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

}
