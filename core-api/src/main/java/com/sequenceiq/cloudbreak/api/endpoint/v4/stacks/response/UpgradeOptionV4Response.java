package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response;

public class UpgradeOptionV4Response {

    private ImageInfoV4Response current;

    private ImageInfoV4Response upgrade;

    private String reason;

    public UpgradeOptionV4Response() {
    }

    public ImageInfoV4Response getCurrent() {
        return current;
    }

    public void setCurrent(ImageInfoV4Response current) {
        this.current = current;
    }

    public ImageInfoV4Response getUpgrade() {
        return upgrade;
    }

    public void setUpgrade(ImageInfoV4Response upgrade) {
        this.upgrade = upgrade;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
