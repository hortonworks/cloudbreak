package com.sequenceiq.cloudbreak.cloud.event.context;

import com.sequenceiq.cloudbreak.cloud.CloudPlatformVariant;
import com.sequenceiq.cloudbreak.domain.AdjustmentType;
import com.sequenceiq.cloudbreak.domain.OnFailureAction;
import com.sequenceiq.cloudbreak.domain.Stack;

public class CloudContext {

    private Long stackId;
    private String stackName;
    private String platform;
    private String variant;
    private String owner;
    private String region;
    private OnFailureAction onFailureAction = OnFailureAction.ROLLBACK;
    private AdjustmentType adjustmentType;
    private Long threshold;
    private Long created;

    public CloudContext(Stack stack) {
        this.stackId = stack.getId();
        this.stackName = stack.getName();
        this.platform = stack.cloudPlatform().name();
        this.variant = stack.getPlatformVariant();
        this.region = stack.getRegion();
        this.owner = stack.getOwner();
        this.onFailureAction = stack.getOnFailureActionAction();
        this.created = stack.getCreated();
        if (stack.getFailurePolicy() != null) {
            this.adjustmentType = stack.getFailurePolicy().getAdjustmentType();
            this.threshold = stack.getFailurePolicy().getThreshold();
        }
    }

    public CloudContext(Long stackId, String stackName, String platform, String owner) {
        this.stackId = stackId;
        this.stackName = stackName;
        this.platform = platform;
        this.owner = owner;
    }

    public CloudContext(Long stackId, String stackName, String platform, String variant, String owner) {
        this.stackId = stackId;
        this.stackName = stackName;
        this.platform = platform;
        this.variant = variant;
        this.owner = owner;
    }

    public Long getStackId() {
        return stackId;
    }

    public String getStackName() {
        return stackName;
    }

    public String getPlatform() {
        return platform;
    }

    public String getVariant() {
        return variant;
    }

    public CloudPlatformVariant getPlatformVariant() {
        return new CloudPlatformVariant(platform, variant);
    }

    public String getOwner() {
        return owner;
    }

    public String getRegion() {
        return region;
    }

    public OnFailureAction getOnFailureAction() {
        return onFailureAction;
    }

    public AdjustmentType getAdjustmentType() {
        return adjustmentType;
    }

    public Long getThreshold() {
        return threshold;
    }

    public void setAdjustmentType(AdjustmentType adjustmentType) {
        this.adjustmentType = adjustmentType;
    }

    public Long getCreated() {
        return created;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("CloudContext{");
        sb.append("stackId=").append(stackId);
        sb.append(", stackName='").append(stackName).append('\'');
        sb.append(", platform='").append(platform).append('\'');
        sb.append(", region='").append(region).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
