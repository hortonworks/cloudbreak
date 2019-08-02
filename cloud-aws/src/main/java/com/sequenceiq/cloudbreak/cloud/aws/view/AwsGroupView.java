package com.sequenceiq.cloudbreak.cloud.aws.view;

import java.util.List;
import java.util.Map;

import com.sequenceiq.cloudbreak.cloud.aws.AwsPlatformParameters.AwsDiskType;
import com.sequenceiq.cloudbreak.cloud.model.SecurityRule;

public class AwsGroupView {

    public static final String AUTSCALING_GROUP_NAME_PREFIX = "AmbariNodes";

    private final Integer instanceCount;

    private final String type;

    private final String flavor;

    private final String groupName;

    private final Integer rootVolumeSize;

    private final Boolean ebsEncrypted;

    private final Boolean kmsKeyDefined;

    private final String kmsKey;

    private final Map<String, Long> volumeCounts;

    private final Double spotPrice;

    private final List<SecurityRule> rules;

    private final List<String> cloudSecurityIds;

    private final String subnetId;

    private final String encryptedAMI;

    private final String autoScalingGroupName;

    private final Boolean useNetworkCidrAsSourceForDefaultRules;

    private final Boolean hasInstanceProfile;

    private final String instanceProfile;

    public AwsGroupView(Integer instanceCount, String type, String flavor, String groupName, Boolean ebsEncrypted, Integer rootVolumeSize,
            Map<String, Long> volumeCounts, Double spotPrice, List<SecurityRule> rules, List<String> cloudSecurityIds, String subnetId, Boolean kmsKeyDefined,
            String kmsKey, String encryptedAMI, boolean useNetworkCidrAsSourceForDefaultRules, String instanceProfile) {
        this.instanceCount = instanceCount;
        this.type = type;
        this.flavor = flavor;
        this.groupName = groupName;
        this.ebsEncrypted = ebsEncrypted;
        this.rootVolumeSize = rootVolumeSize;
        this.spotPrice = spotPrice;
        this.rules = rules;
        this.cloudSecurityIds = cloudSecurityIds;
        this.subnetId = subnetId;
        this.kmsKeyDefined = kmsKeyDefined;
        this.kmsKey = kmsKey;
        this.encryptedAMI = encryptedAMI;
        this.volumeCounts = volumeCounts;
        autoScalingGroupName = getAutoScalingGroupName(groupName);
        this.useNetworkCidrAsSourceForDefaultRules = useNetworkCidrAsSourceForDefaultRules;
        this.instanceProfile = instanceProfile;
        if (instanceProfile != null) {
            hasInstanceProfile = true;
        } else {
            hasInstanceProfile = false;
        }
    }

    public static String getAutoScalingGroupName(String groupName) {
        return AUTSCALING_GROUP_NAME_PREFIX + groupName.replaceAll("_", "");
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

    public Boolean getEbsEncrypted() {
        return ebsEncrypted;
    }

    public Long getVolumeCount(String volumeType) {
        return volumeCounts.getOrDefault(volumeType, 0L);
    }

    public Double getSpotPrice() {
        return spotPrice;
    }

    public Map<String, Long> getVolumeCounts() {
        return volumeCounts;
    }

    public List<SecurityRule> getRules() {
        return rules;
    }

    public Boolean getEbsOptimized() {
        return volumeCounts.keySet().contains(AwsDiskType.St1.value());
    }

    public List<String> getCloudSecurityIds() {
        return cloudSecurityIds;
    }

    public String getCloudSecurityId() {
        return cloudSecurityIds.isEmpty() ? null : cloudSecurityIds.get(0);
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

    public Integer getRootVolumeSize() {
        return rootVolumeSize;
    }

    public String getEncryptedAMI() {
        return encryptedAMI;
    }

    public String getAutoScalingGroupName() {
        return autoScalingGroupName;
    }

    public Boolean getUseNetworkCidrAsSourceForDefaultRules() {
        return useNetworkCidrAsSourceForDefaultRules;
    }

    public Boolean getHasInstanceProfile() {
        return hasInstanceProfile;
    }

    public String getInstanceProfile() {
        return instanceProfile;
    }
}
