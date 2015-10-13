package com.sequenceiq.cloudbreak.cloud.aws.view;

public class AwsGroupView {

    private Integer instanceCount;
    private String type;
    private String flavor;
    private String groupName;
    private Integer volumeCount;
    private Integer volumeSize;
    private Boolean ebsEncrypted;
    private String volumeType;
    private Double spotPrice;

    public AwsGroupView(Integer instanceCount, String type, String flavor, String groupName, Integer volumeCount,
            Boolean ebsEncrypted, Integer volumeSize, String volumeType, Double spotPrice) {
        this.instanceCount = instanceCount;
        this.type = type;
        this.flavor = flavor;
        this.groupName = groupName;
        this.volumeCount = volumeCount;
        this.ebsEncrypted = ebsEncrypted;
        this.volumeSize = volumeSize;
        this.spotPrice = spotPrice;
        this.volumeType = volumeType;
    }

    public Integer getInstanceCount() {
        return instanceCount;
    }

    public String getType() {
        return type;
    }

    public String getFlavor() {
        return flavor;
    }

    public String getGroupName() {
        return groupName;
    }

    public Integer getVolumeCount() {
        return volumeCount;
    }

    public Boolean getEbsEncrypted() {
        return ebsEncrypted;
    }

    public Integer getVolumeSize() {
        return volumeSize;
    }

    public String getVolumeType() {
        return volumeType;
    }

    public Double getSpotPrice() {
        return spotPrice;
    }
}