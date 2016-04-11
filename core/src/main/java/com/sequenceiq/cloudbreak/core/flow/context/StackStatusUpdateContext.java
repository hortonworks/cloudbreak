package com.sequenceiq.cloudbreak.core.flow.context;

import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.event.Payload;

public class StackStatusUpdateContext extends DefaultFlowContext implements FlowContext, Payload {
    private boolean start;

    public StackStatusUpdateContext(Long stackId, Platform cloudPlatform, boolean start) {
        super(stackId, cloudPlatform);
        this.start = start;
    }

    public StackStatusUpdateContext(Long stackId, Platform cloudPlatform, boolean start, String statusReason) {
        super(stackId, cloudPlatform, statusReason);
        this.start = start;
    }

    public boolean isStart() {
        return start;
    }

}
