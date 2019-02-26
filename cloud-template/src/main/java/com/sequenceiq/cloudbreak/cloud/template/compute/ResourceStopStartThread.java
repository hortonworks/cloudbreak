package com.sequenceiq.cloudbreak.cloud.template.compute;

import static com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup.CANCELLED;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup;
import com.sequenceiq.cloudbreak.cloud.scheduler.SyncPollingScheduler;
import com.sequenceiq.cloudbreak.cloud.store.InMemoryStateStore;
import com.sequenceiq.cloudbreak.cloud.task.PollTask;
import com.sequenceiq.cloudbreak.cloud.template.ComputeResourceBuilder;
import com.sequenceiq.cloudbreak.cloud.template.context.ResourceBuilderContext;
import com.sequenceiq.cloudbreak.cloud.template.task.ResourcePollTaskFactory;

@Component(ResourceStopStartThread.NAME)
@Scope("prototype")
public class ResourceStopStartThread implements Callable<ResourceRequestResult<List<CloudVmInstanceStatus>>> {

    public static final String NAME = "resourceStopStartThread";

    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceStopStartThread.class);

    @Inject
    private SyncPollingScheduler<List<CloudVmInstanceStatus>> syncPollingScheduler;

    @Inject
    private ResourcePollTaskFactory resourcePollTaskFactory;

    private final ResourceBuilderContext context;

    private final AuthenticatedContext auth;

    private final CloudResource resource;

    private final CloudInstance instance;

    private final ComputeResourceBuilder<ResourceBuilderContext> builder;

    public ResourceStopStartThread(ResourceBuilderContext context, AuthenticatedContext auth,
            CloudResource resource, CloudInstance instance, ComputeResourceBuilder<ResourceBuilderContext> builder) {
        this.context = context;
        this.auth = auth;
        this.resource = resource;
        this.instance = instance;
        this.builder = builder;
    }

    @Override
    public ResourceRequestResult<List<CloudVmInstanceStatus>> call() throws Exception {
        LOGGER.debug("{} compute resource {}", context.isBuild() ? "Starting" : "Stopping", resource);
        PollGroup pollGroup = InMemoryStateStore.getStack(auth.getCloudContext().getId());
        if (CANCELLED.equals(pollGroup)) {
            List<CloudVmInstanceStatus> result = createResult(InstanceStatus.UNKNOWN);
            return new ResourceRequestResult<>(FutureResult.SUCCESS, result);
        }
        CloudVmInstanceStatus status = context.isBuild() ? builder.start(context, auth, instance) : builder.stop(context, auth, instance);
        if (status != null) {
            PollTask<List<CloudVmInstanceStatus>> task = resourcePollTaskFactory.newPollComputeStatusTask(builder, auth, context, status.getCloudInstance());
            List<CloudVmInstanceStatus> pollResult = syncPollingScheduler.schedule(task);
            return new ResourceRequestResult<>(FutureResult.SUCCESS, pollResult);
        }
        return new ResourceRequestResult<>(FutureResult.SUCCESS, createResult(InstanceStatus.UNKNOWN));
    }

    private List<CloudVmInstanceStatus> createResult(InstanceStatus status) {
        return Collections.singletonList(new CloudVmInstanceStatus(instance, status));
    }

}
