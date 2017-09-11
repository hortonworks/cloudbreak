package com.sequenceiq.cloudbreak.common.type;

public class ImageStatusResult {

    public static final int COMPLETED = 100;

    public static final int HALF = 100;

    public static final int INIT = 0;

    private final ImageStatus imageStatus;

    private final Integer statusProgressValue;

    public ImageStatusResult(ImageStatus imageStatus, Integer statusProgressValue) {
        this.imageStatus = imageStatus;
        this.statusProgressValue = statusProgressValue;
    }

    public ImageStatus getImageStatus() {
        return imageStatus;
    }

    public Integer getStatusProgressValue() {
        return statusProgressValue;
    }
}
