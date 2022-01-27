package com.sequenceiq.cloudbreak.cloud.event.instance;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.resource.CloudStackRequest;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;

public class StopStartUpscaleStartInstancesRequest extends CloudStackRequest<StopStartUpscaleStartInstancesResult> {

    private final String hostGroupName;

    private final List<CloudInstance> stoppedCloudInstancesInHg;

    private final List<CloudInstance> allInstancesInHg;

    private final List<CloudInstance> startedInstancesWithServicesNotRunning;

    private final int numInstancesToStart;

    public StopStartUpscaleStartInstancesRequest(CloudContext cloudContext, CloudCredential cloudCredential, CloudStack cloudStack,
            String hostGroupName, List<CloudInstance> stoppedCloudInstancesInHg, List<CloudInstance> allInstancesInHg,
            List<CloudInstance> startedInstancesWithServicesNotRunning, int numInstancesToStart) {
        super(cloudContext, cloudCredential, cloudStack);
        this.hostGroupName = hostGroupName;
        this.stoppedCloudInstancesInHg = stoppedCloudInstancesInHg;
        this.allInstancesInHg = allInstancesInHg;
        this.startedInstancesWithServicesNotRunning = Optional.ofNullable(startedInstancesWithServicesNotRunning).orElse(Collections.emptyList());
        this.numInstancesToStart = numInstancesToStart;
    }

    public String getHostGroupName() {
        return hostGroupName;
    }

    public List<CloudInstance> getStoppedCloudInstancesInHg() {
        return stoppedCloudInstancesInHg;
    }

    public List<CloudInstance> getAllInstancesInHg() {
        return allInstancesInHg;
    }

    public List<CloudInstance> getStartedInstancesWithServicesNotRunning() {
        return startedInstancesWithServicesNotRunning;
    }

    public int getNumInstancesToStart() {
        return numInstancesToStart;
    }

    @Override
    public String toString() {
        return "StopStartUpscaleStartInstancesRequest{" +
                "hostGroupName=" + hostGroupName +
                "stoppedCloudInstancesInHgCount=" + stoppedCloudInstancesInHg.size() +
                "allInstancesInHgCount=" + allInstancesInHg.size() +
                "startedInstancesWithServicesNotRunningCount=" + startedInstancesWithServicesNotRunning.size() +
                ", numInstancesToStart=" + numInstancesToStart +
                '}';
    }
}