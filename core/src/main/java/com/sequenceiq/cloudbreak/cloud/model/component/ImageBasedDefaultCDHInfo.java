package com.sequenceiq.cloudbreak.cloud.model.component;

import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;

public class ImageBasedDefaultCDHInfo {

    private DefaultCDHInfo defaultCDHInfo;

    private Image image;

    public ImageBasedDefaultCDHInfo(DefaultCDHInfo defaultCDHInfo, Image image) {
        this.defaultCDHInfo = defaultCDHInfo;
        this.image = image;
    }

    public DefaultCDHInfo getDefaultCDHInfo() {
        return defaultCDHInfo;
    }

    public Image getImage() {
        return image;
    }
}
