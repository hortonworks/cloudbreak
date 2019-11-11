package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response;

public class UpgradeOptionV4Response {

    private ImageInfoV4Response current;

    private ImageInfoV4Response upgrade;

    public UpgradeOptionV4Response() {
    }

    public UpgradeOptionV4Response(ImageInfoV4Response current, ImageInfoV4Response upgrade) {
        this.current = current;
        this.upgrade = upgrade;
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
}
