package com.sequenceiq.cloudbreak.cloud.task;


import javax.inject.Inject;

import java.util.List;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.event.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.transform.ResourceStatusLists;

public class PollResourcesStateTask extends PollTask<ResourcesStatePollerResult> {

    private List<CloudResource> cloudResource;

    @Inject
    public PollResourcesStateTask(AuthenticatedContext authenticatedContext, CloudConnector connector,
            List<CloudResource> cloudResource) {
        super(connector, authenticatedContext);
        this.cloudResource = cloudResource;
    }

    @Override
    public ResourcesStatePollerResult call() throws Exception {
        List<CloudResourceStatus> results = getConnector().resources().check(getAuthenticatedContext(), cloudResource);
        CloudResourceStatus status = ResourceStatusLists.aggregate(results);
        return new ResourcesStatePollerResult(getAuthenticatedContext().getCloudContext(), status.getStatus(), status.getStatusReason(), results);
    }

    @Override
    public boolean completed(ResourcesStatePollerResult resourcesStatePollerResult) {
        return resourcesStatePollerResult.getStatus().isPermanent();
    }
}
