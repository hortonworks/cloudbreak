package com.sequenceiq.cloudbreak.cloud.event.instance;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;

public class StopStartUpscaleGetRecoveryCandidatesResult extends CloudPlatformResult {

    private final StopStartUpscaleGetRecoveryCandidatesRequest getStartedInstancesRequest;

    private final List<CloudInstance> startedInstancesWithServicesNotRunning;

    private final Integer adjustment;

    private final String hostGroupName;

    @JsonCreator
    public StopStartUpscaleGetRecoveryCandidatesResult(
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("getStartedInstancesRequest") StopStartUpscaleGetRecoveryCandidatesRequest getStartedInstancesRequest,
            @JsonProperty("startedInstancesWithServicesNotRunning") List<CloudInstance> startedInstancesWithServicesNotRunning,
            @JsonProperty("adjustment") Integer adjustment,
            @JsonProperty("hostGroupName") String hostGroupName) {
        super(resourceId);
        this.getStartedInstancesRequest = getStartedInstancesRequest;
        this.startedInstancesWithServicesNotRunning = startedInstancesWithServicesNotRunning;
        this.adjustment = adjustment;
        this.hostGroupName = hostGroupName;
    }

    public StopStartUpscaleGetRecoveryCandidatesRequest getGetStartedInstancesRequest() {
        return getStartedInstancesRequest;
    }

    public List<CloudInstance> getStartedInstancesWithServicesNotRunning() {
        return startedInstancesWithServicesNotRunning;
    }

    public Integer getAdjustment() {
        return adjustment;
    }

    public String getHostGroupName() {
        return hostGroupName;
    }
}
