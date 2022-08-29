package com.sequenceiq.cloudbreak.cloud.event.instance;

import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;

public class StopStartDownscaleGetRecoveryCandidatesResult extends CloudPlatformResult {

    private final List<CloudInstance> startedInstancesWithServicesNotRunning;

    private final String hostGroupName;

    private final Set<Long> hostIds;

    @JsonCreator
    public StopStartDownscaleGetRecoveryCandidatesResult(
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("startedInstancesWithServicesNotRunning") List<CloudInstance> startedInstancesWithServicesNotRunning,
            @JsonProperty("hostGroupName") String hostGroupName,
            @JsonProperty("hostIds") Set<Long> hostIds) {
        super(resourceId);
        this.startedInstancesWithServicesNotRunning = startedInstancesWithServicesNotRunning;
        this.hostGroupName = hostGroupName;
        this.hostIds = hostIds;
    }

    public List<CloudInstance> getStartedInstancesWithServicesNotRunning() {
        return startedInstancesWithServicesNotRunning;
    }

    public String getHostGroupName() {
        return hostGroupName;
    }

    public Set<Long> getHostIds() {
        return hostIds;
    }
}
