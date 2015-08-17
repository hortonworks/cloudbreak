package com.sequenceiq.cloudbreak.cloud.task;

import java.util.List;

import com.sequenceiq.cloudbreak.cloud.event.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.event.context.ResourceBuilderContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.template.ResourceChecker;

public class PollResourceTask extends PollTask<List<CloudResourceStatus>> {

    private ResourceChecker checker;
    private List<CloudResource> cloudResources;
    private ResourceBuilderContext context;
    private boolean cancellable;

    public PollResourceTask(AuthenticatedContext authenticatedContext, ResourceChecker checker,
            List<CloudResource> cloudResources, ResourceBuilderContext context, boolean cancellable) {
        super(authenticatedContext);
        this.checker = checker;
        this.cloudResources = cloudResources;
        this.context = context;
        this.cancellable = cancellable;
    }

    @Override
    public List<CloudResourceStatus> call() throws Exception {
        return checker.checkResources(context, getAuthenticatedContext(), cloudResources);
    }

    @Override
    public boolean completed(List<CloudResourceStatus> resourceStatuses) {
        for (CloudResourceStatus resourceStatus : resourceStatuses) {
            if (resourceStatus.getStatus().isTransient()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean cancelled() {
        return cancellable && super.cancelled();
    }
}
