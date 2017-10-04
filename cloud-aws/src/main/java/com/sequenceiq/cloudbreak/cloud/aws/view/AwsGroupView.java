package com.sequenceiq.cloudbreak.cloud.aws.view;

import java.util.List;

import com.sequenceiq.cloudbreak.cloud.aws.AwsPlatformParameters.AwsDiskType;
import com.sequenceiq.cloudbreak.cloud.model.SecurityRule;

public class AwsGroupView {

    private final Integer instanceCount;

    private final String type;

    private final String flavor;

    private final String groupName;

    private final Integer volumeCount;

    private final Integer volumeSize;

    private final Boolean ebsEncrypted;

    private final Boolean kmsKeyDefined;

    private final String kmsKey;

    private final String volumeType;

    private final Double spotPrice;

    private final List<SecurityRule> rules;

    private final String cloudSecurityId;

    private final String subnetId;

    private final String snapshotId;

    public AwsGroupView(Integer instanceCount, String type, String flavor, String groupName, Integer volumeCount,
            Boolean ebsEncrypted, Integer volumeSize, String volumeType, Double spotPrice, List<SecurityRule> rules, String cloudSecurityId, String subnetId,
            Boolean kmsKeyDefined, String kmsKey, String snapshotId) {
        this.instanceCount = instanceCount;
        this.type = type;
        this.flavor = flavor;
        this.groupName = groupName;
        this.volumeCount = volumeCount;
        this.ebsEncrypted = ebsEncrypted;
        this.volumeSize = volumeSize;
        this.spotPrice = spotPrice;
        this.volumeType = volumeType;
        this.rules = rules;
        this.cloudSecurityId = cloudSecurityId;
        this.subnetId = subnetId;
        this.kmsKeyDefined = kmsKeyDefined;
        this.kmsKey = kmsKey;
        this.snapshotId = snapshotId;
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

    public List<SecurityRule> getRules() {
        return rules;
    }

    public Boolean getEbsOptimized() {
        return AwsDiskType.St1.value().equals(volumeType);
    }

    public String getCloudSecurityId() {
        return cloudSecurityId;
    }

    public String getSubnetId() {
        return subnetId;
    }

    public Boolean getKmsKeyDefined() {
        return kmsKeyDefined;
    }

    public String getKmsKey() {
        return kmsKey;
    }

    public String getSnapshotId() {
        return snapshotId;
    }
}
