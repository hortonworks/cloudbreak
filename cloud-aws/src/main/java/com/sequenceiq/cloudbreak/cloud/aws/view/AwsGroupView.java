package com.sequenceiq.cloudbreak.cloud.aws.view;

import java.util.List;
import java.util.Map;

import com.sequenceiq.cloudbreak.cloud.aws.AwsPlatformParameters.AwsDiskType;
import com.sequenceiq.cloudbreak.cloud.model.SecurityRule;

public class AwsGroupView {

    public static final String AUTOSCALING_GROUP_NAME_PREFIX = "AmbariNodes";

    public static final String LAUNCH_TEMPLATE_NAME_PREFIX = "ClusterManagerNodeLaunchTemplate";

    public static final String PLACEMENT_GROUP_NAME_PREFIX = "PlacementGroup";

    private final Integer instanceCount;

    private final String type;

    private final String flavor;

    private final String groupName;

    private final Integer rootVolumeSize;

    private final Boolean ebsEncrypted;

    private final Boolean kmsKeyDefined;

    private final String kmsKey;

    private final Map<String, Long> volumeCounts;

    private final List<SecurityRule> rules;

    private final List<String> cloudSecurityIds;

    private final String subnetId;

    private final String encryptedAMI;

    private final String autoScalingGroupName;

    private final String launchTemplateName;

    private final String placementGroupName;

    private final Boolean useNetworkCidrAsSourceForDefaultRules;

    private final Boolean hasInstanceProfile;

    private final String instanceProfile;

    private final int onDemandPercentage;

    private final Double spotMaxPrice;

    private final String placementGroupStrategy;

    public AwsGroupView(Integer instanceCount, String type, String flavor, String groupName, Boolean ebsEncrypted, Integer rootVolumeSize,
            Map<String, Long> volumeCounts, List<SecurityRule> rules, List<String> cloudSecurityIds, String subnetId, Boolean kmsKeyDefined,
            String kmsKey, String encryptedAMI, boolean useNetworkCidrAsSourceForDefaultRules, String instanceProfile, int onDemandPercentage,
            Double spotMaxPrice, String placementGroupStrategy) {
        this.instanceCount = instanceCount;
        this.type = type;
        this.flavor = flavor;
        this.groupName = groupName;
        this.ebsEncrypted = ebsEncrypted;
        this.rootVolumeSize = rootVolumeSize;
        this.rules = rules;
        this.cloudSecurityIds = cloudSecurityIds;
        this.subnetId = subnetId;
        this.kmsKeyDefined = kmsKeyDefined;
        this.kmsKey = kmsKey;
        this.encryptedAMI = encryptedAMI;
        this.volumeCounts = volumeCounts;
        autoScalingGroupName = getAutoScalingGroupName(groupName);
        launchTemplateName = getLaunchTemplateName(groupName);
        placementGroupName = getPlacementGroupName(groupName);
        this.useNetworkCidrAsSourceForDefaultRules = useNetworkCidrAsSourceForDefaultRules;
        this.instanceProfile = instanceProfile;
        hasInstanceProfile = instanceProfile != null;
        this.onDemandPercentage = onDemandPercentage;
        this.spotMaxPrice = spotMaxPrice;
        this.placementGroupStrategy = placementGroupStrategy;
    }

    public static String getAutoScalingGroupName(String groupName) {
        return AUTOSCALING_GROUP_NAME_PREFIX + sanitizeGroupName(groupName);
    }

    public static String getPlacementGroupName(String groupName) {
        return PLACEMENT_GROUP_NAME_PREFIX + sanitizeGroupName(groupName);
    }

    public static String getLaunchTemplateName(String groupName) {
        return LAUNCH_TEMPLATE_NAME_PREFIX + sanitizeGroupName(groupName);
    }

    private static String sanitizeGroupName(String groupName) {
        return groupName.replaceAll("_", "");
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

    public String getLaunchTemplateName() {
        return launchTemplateName;
    }

    public String getPlacementGroupName() {
        return placementGroupName;
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

    public int getOnDemandPercentage() {
        return onDemandPercentage;
    }

    public Double getSpotMaxPrice() {
        return spotMaxPrice;
    }

    public String getPlacementGroupStrategy() {
        return placementGroupStrategy.toLowerCase();
    }
}
