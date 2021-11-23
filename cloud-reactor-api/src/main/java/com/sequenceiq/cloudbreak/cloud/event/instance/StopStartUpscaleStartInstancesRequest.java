package com.sequenceiq.cloudbreak.cloud.event.instance;

import java.util.List;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.resource.CloudStackRequest;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;

public class StopStartUpscaleStartInstancesRequest<T> extends CloudStackRequest<T> {

    // TODO CB-14929: The CB state of STOPPED instances may not be up-to-date. It may be better to send a list of all isntances,
    //  and have the handler determine the instance state from the cloud-provider - at least as a fallback mechanism.
    //  This would require sending 1) The stopped instances to the handler, and 2) a list of all-instances to the handler.
    private final List<CloudInstance> stoppedCloudInstancesInHg;

    private final int numInstancesToStart;

    public StopStartUpscaleStartInstancesRequest(CloudContext cloudContext, CloudCredential cloudCredential, CloudStack cloudStack,
            List<CloudInstance> stoppedCloudInstancesInHg, int numInstancesToStart) {
        super(cloudContext, cloudCredential, cloudStack);
        this.stoppedCloudInstancesInHg = stoppedCloudInstancesInHg;
        this.numInstancesToStart = numInstancesToStart;
    }

    public List<CloudInstance> getStoppedCloudInstancesInHg() {
        return stoppedCloudInstancesInHg;
    }

    public int getNumInstancesToStart() {
        return numInstancesToStart;
    }

    @Override
    public String toString() {
        return "StopStartUpscaleStartInstancesRequest{" +
                "stoppedCloudInstancesInHg=" + stoppedCloudInstancesInHg +
                ", numInstancesToStart=" + numInstancesToStart +
                '}';
    }
}
