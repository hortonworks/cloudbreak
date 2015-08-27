package com.sequenceiq.cloudbreak.cloud.task;

import com.sequenceiq.cloudbreak.cloud.BooleanStateConnector;
import com.sequenceiq.cloudbreak.cloud.event.context.AuthenticatedContext;

public class PollBooleanTerminationTask extends PollBooleanStateTask {

    public PollBooleanTerminationTask(AuthenticatedContext authenticatedContext, BooleanStateConnector booleanStateConnector) {
        super(authenticatedContext, booleanStateConnector);
    }

    @Override
    public boolean cancelled() {
        return false;
    }
}
