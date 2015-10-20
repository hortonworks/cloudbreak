package com.sequenceiq.cloudbreak.cloud.task;


import java.util.List;

import javax.inject.Inject;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.ResourceConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.transform.ResourceStatusLists;

@Component(PollResourcesStateTask.NAME)
@Scope(value = "prototype")
public class PollResourcesStateTask extends AbstractPollTask<ResourcesStatePollerResult> {
    public static final String NAME = "pollResourcesStateTask";

    private List<CloudResource> cloudResource;
    private ResourceConnector resourceConnector;

    @Inject
    public PollResourcesStateTask(AuthenticatedContext authenticatedContext, ResourceConnector resourceConnector,
            List<CloudResource> cloudResource, boolean cancellable) {
        super(authenticatedContext, cancellable);
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
