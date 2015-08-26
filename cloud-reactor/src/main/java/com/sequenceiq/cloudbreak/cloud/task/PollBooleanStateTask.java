package com.sequenceiq.cloudbreak.cloud.task;


import javax.inject.Inject;

import com.sequenceiq.cloudbreak.cloud.BooleanStateConnector;
import com.sequenceiq.cloudbreak.cloud.event.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.event.instance.BooleanResult;

public class PollBooleanStateTask extends PollTask<BooleanResult> {

    private BooleanStateConnector booleanStateConnector;

    @Inject
    public PollBooleanStateTask(AuthenticatedContext authenticatedContext, BooleanStateConnector booleanStateConnector) {
        super(authenticatedContext);
        this.booleanStateConnector = booleanStateConnector;
    }

    @Override
    public BooleanResult call() throws Exception {
        Boolean result = booleanStateConnector.check(getAuthenticatedContext());
        return new BooleanResult(getAuthenticatedContext().getCloudContext(), result);
    }

    @Override
    public boolean completed(BooleanResult booleanResult) {
        return booleanResult.getResult().equals(Boolean.TRUE);
    }
}
