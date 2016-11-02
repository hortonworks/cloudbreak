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
@Scope(value = "prototype")
public class PollResourceTask extends AbstractPollTask<List<CloudResourceStatus>> {
    public static final String NAME = "pollResourceTask";

    private ResourceChecker checker;

    private List<CloudResource> cloudResources;

    private ResourceBuilderContext context;

    public PollResourceTask(AuthenticatedContext authenticatedContext, ResourceChecker checker,
            List<CloudResource> cloudResources, ResourceBuilderContext context, boolean cancellable) {
        super(authenticatedContext, cancellable);
        this.checker = checker;
        this.cloudResources = cloudResources;
        this.context = context;
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

}
