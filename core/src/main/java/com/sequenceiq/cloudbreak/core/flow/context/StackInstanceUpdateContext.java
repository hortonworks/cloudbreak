package com.sequenceiq.cloudbreak.core.flow.context;

import com.sequenceiq.cloudbreak.cloud.event.InstancePayload;
import com.sequenceiq.cloudbreak.service.stack.event.RemoveInstanceRequest;

public class StackInstanceUpdateContext extends DefaultFlowContext implements InstancePayload {

    private final String instanceId;

    public StackInstanceUpdateContext(RemoveInstanceRequest removeInstanceRequest) {
        super(removeInstanceRequest.getStackId(), removeInstanceRequest.getCloudPlatform());
        this.instanceId = removeInstanceRequest.getInstanceId();
    }

    @Override
    public String getInstanceId() {
        return instanceId;
    }
}
