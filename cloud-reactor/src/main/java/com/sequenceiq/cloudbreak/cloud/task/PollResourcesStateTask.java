package com.sequenceiq.cloudbreak.cloud.task;


import java.util.List;

import javax.inject.Inject;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.event.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.transform.ResourceStatusLists;

public class PollResourcesStateTask implements PollTask<ResourcesStatePollerResult> {

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
    public ResourcesStatePollerResult call() throws Exception {
        List<CloudResourceStatus> results = connector.resources().check(authenticatedContext, cloudResource);
        CloudResourceStatus status = ResourceStatusLists.aggregate(results);
        return new ResourcesStatePollerResult(authenticatedContext.getCloudContext(), status.getStatus(), status.getStatusReason(), results);

    }

    @Override
    public boolean completed(ResourcesStatePollerResult resourcesStatePollerResult) {
        return resourcesStatePollerResult.getStatus().isPermanent();
    }
}
