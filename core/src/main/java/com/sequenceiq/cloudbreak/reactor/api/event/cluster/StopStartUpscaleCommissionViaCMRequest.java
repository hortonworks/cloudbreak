package com.sequenceiq.cloudbreak.reactor.api.event.cluster;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.ClusterPlatformRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.HostGroupPayload;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;

public class StopStartUpscaleCommissionViaCMRequest extends ClusterPlatformRequest implements HostGroupPayload {

    private final String hostGroupName;

    private final List<InstanceMetadataView> startedInstancesToCommission;

    private final List<InstanceMetadataView> servicesNotRunningInstancesToCommission;

    @JsonCreator
    public StopStartUpscaleCommissionViaCMRequest(
            @JsonProperty("stackId") Long stackId,
            @JsonProperty("hostGroupName") String hostGroupName,
            @JsonProperty("startedInstancesToCommission") List<InstanceMetadataView> startedInstancesToCommission,
            @JsonProperty("servicesNotRunningInstancesToCommission") List<InstanceMetadataView> servicesNotRunningInstancesToCommission) {
        super(stackId);
        this.hostGroupName = hostGroupName;
        this.startedInstancesToCommission = startedInstancesToCommission == null ? Collections.emptyList() : startedInstancesToCommission;
        this.servicesNotRunningInstancesToCommission =
                servicesNotRunningInstancesToCommission == null ? Collections.emptyList() : servicesNotRunningInstancesToCommission;
    }

    public List<InstanceMetadataView> getStartedInstancesToCommission() {
        return startedInstancesToCommission;
    }

    public List<InstanceMetadataView> getServicesNotRunningInstancesToCommission() {
        return servicesNotRunningInstancesToCommission;
    }

    @Override
    public String getHostGroupName() {
        return hostGroupName;
    }

    @Override
    public String toString() {
        return "StopStartUpscaleCommissionViaCMRequest{" +
                "startedInstancesToCommissionCount=" + startedInstancesToCommission.size() +
                ", servicesNotRunningInstancesToCommissionCount=" + servicesNotRunningInstancesToCommission.size() +
                '}';
    }

}
