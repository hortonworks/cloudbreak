package com.sequenceiq.cloudbreak.service.upgrade.image;

import java.util.List;

import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;

public class ImageFilterResult {

    public static final String EMPTY_REASON = "";

    private final List<Image> images;

    private final String reason;

    public ImageFilterResult(List<Image> images, String reason) {
        this.images = images;
        this.reason = reason;
    }

    public ImageFilterResult(List<Image> images) {
        this.images = images;
        this.reason = EMPTY_REASON;
    }

    public List<Image> getImages() {
        return images;
    }

    public String getReason() {
        return reason;
    }
}
