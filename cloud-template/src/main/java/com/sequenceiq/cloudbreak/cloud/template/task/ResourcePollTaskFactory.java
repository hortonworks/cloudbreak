package com.sequenceiq.cloudbreak.cloud.template.task;

import java.util.List;

import javax.inject.Inject;

import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.task.PollTask;
import com.sequenceiq.cloudbreak.cloud.template.ComputeResourceBuilder;
import com.sequenceiq.cloudbreak.cloud.template.ResourceChecker;
import com.sequenceiq.cloudbreak.cloud.template.context.ResourceBuilderContext;

@Component
public class ResourcePollTaskFactory {
    @Inject
    private ApplicationContext applicationContext;

    public PollTask<List<CloudResourceStatus>> newPollResourceTask(ResourceChecker<?> checker, AuthenticatedContext authenticatedContext,
            List<CloudResource> cloudResource, ResourceBuilderContext context, boolean cancellable) {
        return createPollTask(PollResourceTask.NAME, authenticatedContext, checker, cloudResource, context, cancellable);
    }

    public PollTask<List<CloudVmInstanceStatus>> newPollComputeStatusTask(ComputeResourceBuilder<?> builder, AuthenticatedContext authenticatedContext,
            ResourceBuilderContext context, CloudInstance instance) {
        return createPollTask(PollComputeStatusTask.NAME, authenticatedContext, builder, context, instance);
    }

    @SuppressWarnings("unchecked")
    private <T> T createPollTask(String name, Object... args) {
        return (T) applicationContext.getBean(name, args);
    }
}
