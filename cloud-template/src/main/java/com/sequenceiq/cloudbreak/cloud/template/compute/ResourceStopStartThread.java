package com.sequenceiq.cloudbreak.cloud.template.compute;

import static com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup.CANCELLED;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup;
import com.sequenceiq.cloudbreak.cloud.store.InMemoryStateStore;
import com.sequenceiq.cloudbreak.cloud.template.ComputeResourceBuilder;
import com.sequenceiq.cloudbreak.cloud.template.context.ResourceBuilderContext;

@Component(ResourceStopStartThread.NAME)
@Scope("prototype")
public class ResourceStopStartThread implements Callable<ResourceRequestResult<List<CloudVmInstanceStatus>>> {

    public static final String NAME = "resourceStopStartThread";

    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceStopStartThread.class);

    private final ResourceBuilderContext context;

    private final AuthenticatedContext auth;

    private final List<CloudInstance> instances;

    private final ComputeResourceBuilder<ResourceBuilderContext> builder;

    public ResourceStopStartThread(ResourceBuilderContext context, AuthenticatedContext auth,
            List<CloudInstance> instances, ComputeResourceBuilder<ResourceBuilderContext> builder) {
        this.context = context;
        this.auth = auth;
        this.instances = instances;
        this.builder = builder;
    }

    @Override
    public ResourceRequestResult<List<CloudVmInstanceStatus>> call() {
        PollGroup pollGroup = InMemoryStateStore.getStack(auth.getCloudContext().getId());
        if (CANCELLED.equals(pollGroup)) {
            LOGGER.debug("Polling is cancelled for stop/start operation for instances: {}", instances);
            return new ResourceRequestResult<>(FutureResult.SUCCESS, null);
        }
        List<CloudVmInstanceStatus> result = new ArrayList<>();
        for (CloudInstance instance : instances) {
            LOGGER.debug("{} instance {}", context.isBuild() ? "Starting" : "Stopping", instance);
            CloudVmInstanceStatus status = context.isBuild() ? builder.start(context, auth, instance) : builder.stop(context, auth, instance);
            result.add(status);
        }
        return new ResourceRequestResult<>(FutureResult.SUCCESS, result);
    }

}
