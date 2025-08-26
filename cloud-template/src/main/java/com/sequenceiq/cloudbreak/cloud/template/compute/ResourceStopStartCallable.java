package com.sequenceiq.cloudbreak.cloud.template.compute;

import static com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup.CANCELLED;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup;
import com.sequenceiq.cloudbreak.cloud.store.InMemoryStateStore;
import com.sequenceiq.cloudbreak.cloud.template.ComputeResourceBuilder;
import com.sequenceiq.cloudbreak.cloud.template.context.ResourceBuilderContext;

public class ResourceStopStartCallable implements Callable<List<CloudVmInstanceStatus>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceStopStartCallable.class);

    private final ResourceBuilderContext context;

    private final AuthenticatedContext auth;

    private final List<CloudInstance> instances;

    private final ComputeResourceBuilder<ResourceBuilderContext> builder;

    public ResourceStopStartCallable(ResourceStopStartCallablePayload payload) {
        this.context = payload.getContext();
        this.auth = payload.getAuth();
        this.instances = payload.getInstances();
        this.builder = payload.getBuilder();
    }

    @Override
    public List<CloudVmInstanceStatus> call() throws InstanceResourceStopStartException {
        PollGroup pollGroup = InMemoryStateStore.getStack(auth.getCloudContext().getId());
        if (CANCELLED.equals(pollGroup)) {
            LOGGER.debug("Polling is cancelled for stop/start operation for instances: {}", instances);
            return null;
        }
        List<CloudVmInstanceStatus> result = new ArrayList<>();
        for (CloudInstance instance : instances) {
            String operationName = context.isBuild() ? "Starting" : "Stopping";
            try {
                LOGGER.debug("{} instance {}", operationName, instance);
                CloudVmInstanceStatus status = context.isBuild() ? builder.start(context, auth, instance) : builder.stop(context, auth, instance);
                LOGGER.debug("Operation result for instance ({}): {}", instance.getInstanceId(), status);
                result.add(status);
            } catch (Exception ex) {
                String msg = String.format("%s of instance '%s' failed, please retry the failed operation", operationName, instance.getInstanceId());
                LOGGER.warn(msg, ex);
                throw new InstanceResourceStopStartException(msg, ex, instance, context.isBuild());
            }
        }
        return result;
    }

}
