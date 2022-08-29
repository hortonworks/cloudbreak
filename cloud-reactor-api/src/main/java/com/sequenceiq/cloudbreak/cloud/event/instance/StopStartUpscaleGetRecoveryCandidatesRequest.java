package com.sequenceiq.cloudbreak.cloud.event.instance;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.resource.CloudStackRequest;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;

public class StopStartUpscaleGetRecoveryCandidatesRequest extends CloudStackRequest<StopStartUpscaleGetRecoveryCandidatesResult> {

    private final String hostGroupName;

    private final Integer adjustment;

    private final List<CloudInstance> allInstancesInHostGroup;

    private final boolean failureRecoveryEnabled;

    @JsonCreator
    public StopStartUpscaleGetRecoveryCandidatesRequest(
            @JsonProperty("cloudContext") CloudContext cloudContext,
            @JsonProperty("cloudCredential") CloudCredential cloudCredential,
            @JsonProperty("cloudStack") CloudStack cloudStack,
            @JsonProperty("hostGroupName") String hostGroupName,
            @JsonProperty("adjustment") Integer adjustment,
            @JsonProperty("allInstancesInHostGroup") List<CloudInstance> allInstancesInHostGroup,
            @JsonProperty("failureRecoveryEnabled") boolean failureRecoveryEnabled) {
        super(cloudContext, cloudCredential, cloudStack);
        this.hostGroupName = hostGroupName;
        this.adjustment = adjustment;
        this.allInstancesInHostGroup = allInstancesInHostGroup;
        this.failureRecoveryEnabled = failureRecoveryEnabled;
    }

    public String getHostGroupName() {
        return hostGroupName;
    }

    public List<CloudInstance> getAllInstancesInHostGroup() {
        return allInstancesInHostGroup;
    }

    public Integer getAdjustment() {
        return adjustment;
    }

    public boolean isFailureRecoveryEnabled() {
        return failureRecoveryEnabled;
    }
}
