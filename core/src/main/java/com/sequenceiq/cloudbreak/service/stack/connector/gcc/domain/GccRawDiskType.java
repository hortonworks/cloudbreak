package com.sequenceiq.cloudbreak.service.stack.connector.gcc.domain;

public enum GccRawDiskType {

    SSD("pd-ssd"), HDD("pd-standard");

    private final String value;

    private GccRawDiskType(String value) {
        this.value = value;
    }

    public String getUrl(String projectId, GccZone zone) {
        return String.format("https://www.googleapis.com/compute/v1/projects/%s/zones/%s/diskTypes/%s", projectId, zone.getValue(), value);
    }

}
