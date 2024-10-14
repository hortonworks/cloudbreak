package com.sequenceiq.cloudbreak.cloud.model;

import java.util.Objects;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        VmRecommendation that = (VmRecommendation) o;
        return cardinality == that.cardinality &&
                volumeCount == that.volumeCount &&
                volumeSizeGB == that.volumeSizeGB &&
                Objects.equals(type, that.type) &&
                Objects.equals(flavor, that.flavor) &&
                Objects.equals(volumeType, that.volumeType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, flavor, cardinality, volumeType, volumeCount, volumeSizeGB);
    }
}
