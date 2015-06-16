package com.sequenceiq.cloudbreak.cloud.task;


import java.util.List;

import javax.inject.Inject;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.event.LaunchStackResult;
import com.sequenceiq.cloudbreak.cloud.event.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.transform.ResourceStatusLists;

public class PollResourcesStateTask implements PollTask<LaunchStackResult> {

    private CloudConnector connector;

    private AuthenticatedContext authenticatedContext;

    private List<CloudResource> cloudResource;


    @Inject
    public PollResourcesStateTask(AuthenticatedContext authenticatedContext, CloudConnector connector,
            List<CloudResource> cloudResource) {
        this.authenticatedContext = authenticatedContext;
        this.connector = connector;
        this.cloudResource = cloudResource;
    }

    @Override
    public LaunchStackResult call() throws Exception {
        List<CloudResourceStatus> results = connector.resources().check(authenticatedContext, cloudResource);
        CloudResourceStatus status = ResourceStatusLists.aggregate(results);
        return new LaunchStackResult(authenticatedContext.getStackContext(), status.getStatus(), status.getStatusReason(), results);

    }

    @Override
    public boolean completed(LaunchStackResult launchStackResult) {
        return launchStackResult.getStatus().isPermanent();
    }
}
