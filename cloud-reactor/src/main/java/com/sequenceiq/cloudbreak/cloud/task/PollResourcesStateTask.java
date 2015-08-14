package com.sequenceiq.cloudbreak.cloud.task;


import java.util.List;

import javax.inject.Inject;

import com.sequenceiq.cloudbreak.cloud.ResourceConnector;
import com.sequenceiq.cloudbreak.cloud.event.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.transform.ResourceStatusLists;

public class PollResourcesStateTask extends PollTask<ResourcesStatePollerResult> {

    private List<CloudResource> cloudResource;
    private ResourceConnector resourceConnector;

    @Inject
    public PollResourcesStateTask(AuthenticatedContext authenticatedContext, ResourceConnector resourceConnector,
            List<CloudResource> cloudResource) {
        super(authenticatedContext);
        this.cloudResource = cloudResource;
        this.resourceConnector = resourceConnector;
    }

    @Override
    public ResourcesStatePollerResult call() throws Exception {
        List<CloudResourceStatus> results = resourceConnector.check(getAuthenticatedContext(), cloudResource);
        CloudResourceStatus status = ResourceStatusLists.aggregate(results);
        return new ResourcesStatePollerResult(getAuthenticatedContext().getCloudContext(), status.getStatus(), status.getStatusReason(), results);
    }

    @Override
    public boolean completed(ResourcesStatePollerResult resourcesStatePollerResult) {
        return resourcesStatePollerResult.getStatus().isPermanent();
    }
}
