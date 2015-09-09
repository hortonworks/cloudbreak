package com.sequenceiq.cloudbreak.cloud.task;

import java.util.List;
import java.util.Set;

import com.sequenceiq.cloudbreak.cloud.BooleanStateConnector;
import com.sequenceiq.cloudbreak.cloud.InstanceConnector;
import com.sequenceiq.cloudbreak.cloud.event.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.event.context.ResourceBuilderContext;
import com.sequenceiq.cloudbreak.cloud.event.instance.BooleanResult;
import com.sequenceiq.cloudbreak.cloud.event.instance.InstanceConsoleOutputResult;
import com.sequenceiq.cloudbreak.cloud.event.instance.InstancesStatusResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.template.ComputeResourceBuilder;
import com.sequenceiq.cloudbreak.cloud.template.ResourceChecker;

public interface PollTaskFactory {

    PollTask<ResourcesStatePollerResult> newPollResourcesStateTask(AuthenticatedContext authenticatedContext, List<CloudResource> cloudResource);

    PollTask<ResourcesStatePollerResult> newPollResourceTerminationTask(AuthenticatedContext authenticatedContext, List<CloudResource> cloudResource);

    PollTask<InstancesStatusResult> newPollInstanceStateTask(AuthenticatedContext authenticatedContext, List<CloudInstance> instances);

    PollTask<InstancesStatusResult> newPollInstanceStateTask(AuthenticatedContext authenticatedContext, List<CloudInstance> instances,
            Set<InstanceStatus> completedStatuses);

    PollTask<InstanceConsoleOutputResult> newPollConsoleOutputTask(InstanceConnector instanceConnector,
            AuthenticatedContext authenticatedContext, CloudInstance instance);

    PollTask<List<CloudResourceStatus>> newPollResourceTask(ResourceChecker checker, AuthenticatedContext authenticatedContext,
            List<CloudResource> cloudResource, ResourceBuilderContext context, boolean cancellable);

    PollTask<List<CloudVmInstanceStatus>> newPollComputeStatusTask(ComputeResourceBuilder builder, AuthenticatedContext authenticatedContext,
            ResourceBuilderContext context, CloudInstance instance);

    PollTask<BooleanResult> newPollBooleanStateTask(AuthenticatedContext authenticatedContext, BooleanStateConnector connector);

    PollTask<BooleanResult> newPollBooleanTerminationTask(AuthenticatedContext authenticatedContext, BooleanStateConnector connector);
}
