package com.sequenceiq.cloudbreak.cloud.event.context;

import com.sequenceiq.cloudbreak.domain.AdjustmentType;
import com.sequenceiq.cloudbreak.domain.OnFailureAction;
import com.sequenceiq.cloudbreak.domain.Stack;

public class CloudContext {

    private Long stackId;
    private String stackName;
    private String platform;
    private int parallelResourceRequest;
    private String owner;
    private String region;
    private OnFailureAction onFailureAction = OnFailureAction.ROLLBACK;
    private AdjustmentType adjustmentType;
    private Long threshold;


    public CloudContext(Stack stack) {
        this.stackId = stack.getId();
        this.stackName = stack.getName();
        this.platform = stack.cloudPlatform().name();
        this.region = stack.getRegion();
        this.owner = stack.getOwner();
        this.parallelResourceRequest = stack.cloudPlatform().parallelNumber();
        this.onFailureAction = stack.getOnFailureActionAction();
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

    public Long getStackId() {
        return stackId;
    }

    public String getStackName() {
        return stackName;
    }

    public String getPlatform() {
        return platform;
    }

    public String getOwner() {
        return owner;
    }

    public String getRegion() {
        return region;
    }

    public int getParallelResourceRequest() {
        return parallelResourceRequest;
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

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("CloudContext{");
        sb.append("stackId=").append(stackId);
        sb.append(", stackName='").append(stackName).append('\'');
        sb.append(", platform='").append(platform).append('\'');
        sb.append(", parallelResourceAction=").append(parallelResourceRequest);
        sb.append(", region='").append(region).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
