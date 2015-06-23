package com.sequenceiq.cloudbreak.core.flow.context;

import com.sequenceiq.cloudbreak.service.stack.event.RemoveInstanceRequest;

public class StackInstanceUpdateContext extends DefaultFlowContext implements FlowContext {

    private final String instanceId;

    public StackInstanceUpdateContext(RemoveInstanceRequest removeInstanceRequest) {
        super(removeInstanceRequest.getStackId(), removeInstanceRequest.getCloudPlatform());
        this.instanceId = removeInstanceRequest.getInstanceId();
    }

    public String getInstanceId() {
        return instanceId;
    }
}
