package com.sequenceiq.cloudbreak.service.upgrade;

import com.sequenceiq.cloudbreak.cloud.model.catalog.Images;

public class ImageFilterResult {

    private final Images availableImages;

    private final String reason;

    public ImageFilterResult(Images availableImages, String reason) {
        this.availableImages = availableImages;
        this.reason = reason;
    }

    public Images getAvailableImages() {
        return availableImages;
    }

    public String getReason() {
        return reason;
    }
}
