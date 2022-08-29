package com.sequenceiq.cloudbreak.cloud.event.instance;

import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.resource.CloudStackRequest;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;

public class StopStartDownscaleGetRecoveryCandidatesRequest extends CloudStackRequest<StopStartDownscaleGetRecoveryCandidatesResult> {

    private final String hostGroupName;

    private final List<CloudInstance> allInstancesInHostGroup;

    private final Set<Long> hostIds;

    private final boolean failureRecoveryEnabled;

    @JsonCreator
    public StopStartDownscaleGetRecoveryCandidatesRequest(
            @JsonProperty("cloudContext") CloudContext cloudContext,
            @JsonProperty("cloudCredential") CloudCredential cloudCredential,
            @JsonProperty("cloudStack") CloudStack cloudStack,
            @JsonProperty("hostGroupName") String hostGroupName,
            @JsonProperty("allInstancesInHostGroup") List<CloudInstance> allInstancesInHostGroup,
            @JsonProperty("hostIds") Set<Long> hostIds,
            @JsonProperty("failureRecoveryEnabled") boolean failureRecoveryEnabled) {
        super(cloudContext, cloudCredential, cloudStack);
        this.hostGroupName = hostGroupName;
        this.allInstancesInHostGroup = allInstancesInHostGroup;
        this.hostIds = hostIds;
        this.failureRecoveryEnabled = failureRecoveryEnabled;
    }

    public String getHostGroupName() {
        return hostGroupName;
    }

    public List<CloudInstance> getAllInstancesInHostGroup() {
        return allInstancesInHostGroup;
    }

    public Set<Long> getHostIds() {
        return hostIds;
    }

    public boolean isFailureRecoveryEnabled() {
        return failureRecoveryEnabled;
    }
}
