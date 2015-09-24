package com.sequenceiq.cloudbreak.domain;

public class ImageStatusResult {

    public static final int COMPLETED = 100;
    public static final int HALF = 100;
    public static final int INIT = 0;

    private ImageStatus imageStatus;
    private Integer statusProgressValue;

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
