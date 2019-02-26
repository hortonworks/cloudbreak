package com.sequenceiq.cloudbreak.cloud.template.task;

import java.util.List;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.task.AbstractPollTask;
import com.sequenceiq.cloudbreak.cloud.template.ResourceChecker;
import com.sequenceiq.cloudbreak.cloud.template.context.ResourceBuilderContext;

@Component(PollResourceTask.NAME)
@Scope("prototype")
public class PollResourceTask extends AbstractPollTask<List<CloudResourceStatus>> {
    public static final String NAME = "pollResourceTask";

    private final ResourceChecker<ResourceBuilderContext> checker;

    private final List<CloudResource> cloudResources;

    private final ResourceBuilderContext context;

    public PollResourceTask(AuthenticatedContext authenticatedContext, ResourceChecker<ResourceBuilderContext> checker,
            List<CloudResource> cloudResources, ResourceBuilderContext context, boolean cancellable) {
        super(authenticatedContext, cancellable);
        this.checker = checker;
        this.cloudResources = cloudResources;
        this.context = context;
    }

    @Override
    protected List<CloudResourceStatus> doCall() {
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

}
