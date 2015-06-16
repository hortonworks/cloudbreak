package com.sequenceiq.cloudbreak.domain;

public enum GcpRawDiskType {

    SSD("pd-ssd"), HDD("pd-standard");

    private final String value;

    private GcpRawDiskType(String value) {
        this.value = value;
    }

    public String getUrl(String projectId, CloudRegion zone) {
        return String.format("https://www.googleapis.com/compute/v1/projects/%s/zones/%s/diskTypes/%s", projectId, zone.value(), value);
    }

}
