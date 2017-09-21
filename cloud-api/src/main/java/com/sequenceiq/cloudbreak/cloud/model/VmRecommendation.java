package com.sequenceiq.cloudbreak.cloud.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class VmRecommendation {

    private String type;

    private String flavor;

    private long cardinality;

    private String volumeType;

    private long volumeCount;

    private long volumeSizeGB;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getFlavor() {
        return flavor;
    }

    public void setFlavor(String flavor) {
        this.flavor = flavor;
    }

    public long getCardinality() {
        return cardinality;
    }

    public void setCardinality(long cardinality) {
        this.cardinality = cardinality;
    }

    public String getVolumeType() {
        return volumeType;
    }

    public void setVolumeType(String volumeType) {
        this.volumeType = volumeType;
    }

    public long getVolumeCount() {
        return volumeCount;
    }

    public void setVolumeCount(long volumeCount) {
        this.volumeCount = volumeCount;
    }

    public long getVolumeSizeGB() {
        return volumeSizeGB;
    }

    public void setVolumeSizeGB(long volumeSizeGB) {
        this.volumeSizeGB = volumeSizeGB;
    }
}
