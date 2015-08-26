package com.sequenceiq.cloudbreak.cloud.task;

import java.util.List;

import com.sequenceiq.cloudbreak.cloud.ResourceConnector;
import com.sequenceiq.cloudbreak.cloud.event.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;

public class PollResourceTerminationTask extends PollResourcesStateTask {

    public PollResourceTerminationTask(AuthenticatedContext authenticatedContext, ResourceConnector connector, List<CloudResource> cloudResource) {
        super(authenticatedContext, connector, cloudResource);
    }

    @Override
    public boolean cancelled() {
        return false;
    }
}
