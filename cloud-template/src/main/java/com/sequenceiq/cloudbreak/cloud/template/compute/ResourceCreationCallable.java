package com.sequenceiq.cloudbreak.cloud.template.compute;

import static com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup.CANCELLED;
import static java.lang.String.format;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.cloud.scheduler.CancellationException;
import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup;
import com.sequenceiq.cloudbreak.cloud.scheduler.SyncPollingScheduler;
import com.sequenceiq.cloudbreak.cloud.store.InMemoryStateStore;
import com.sequenceiq.cloudbreak.cloud.task.PollTask;
import com.sequenceiq.cloudbreak.cloud.template.ComputeResourceBuilder;
import com.sequenceiq.cloudbreak.cloud.template.context.ResourceBuilderContext;
import com.sequenceiq.cloudbreak.cloud.template.init.ResourceBuilders;
import com.sequenceiq.cloudbreak.cloud.template.task.ResourcePollTaskFactory;
import com.sequenceiq.common.api.type.ResourceType;

public class ResourceCreationCallable implements Callable<ResourceRequestResult<List<CloudResourceStatus>>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceCreationCallable.class);

    private static final List<ResourceType> VOLUME_SET_RESOURCE_TYPES = List.of(ResourceType.AWS_VOLUMESET, ResourceType.AZURE_VOLUMESET);

    // half an hour with 1 sec delays
    private static final int VOLUME_SET_POLLING_ATTEMPTS = 1800;

    private final ResourceBuilders resourceBuilders;

    private final SyncPollingScheduler<List<CloudResourceStatus>> syncPollingScheduler;

    private final ResourcePollTaskFactory resourcePollTaskFactory;

    private final PersistenceNotifier persistenceNotifier;

    private final List<CloudInstance> instances;

    private final Group group;

    private final ResourceBuilderContext context;

    private final AuthenticatedContext auth;

    private final CloudStack cloudStack;

    public ResourceCreationCallable(ResourceCreationCallablePayload payload, ResourceBuilders resourceBuilders,
            SyncPollingScheduler<List<CloudResourceStatus>> syncPollingScheduler, ResourcePollTaskFactory resourcePollTaskFactory,
            PersistenceNotifier persistenceNotifier) {
        this.resourceBuilders = resourceBuilders;
        this.syncPollingScheduler = syncPollingScheduler;
        this.resourcePollTaskFactory = resourcePollTaskFactory;
        this.persistenceNotifier = persistenceNotifier;
        this.instances = payload.getInstances();
        this.group = payload.getGroup();
        this.context = payload.getContext();
        this.auth = payload.getAuth();
        this.cloudStack = payload.getCloudStack();
    }

    @Override
    public ResourceRequestResult<List<CloudResourceStatus>> call() {
        List<CloudResourceStatus> results = new ArrayList<>();
        String stackName = auth.getCloudContext().getName();
        for (CloudInstance instance : instances) {
            LOGGER.debug("Create all compute resources for instance: '{}' stack: '{}'", instance, stackName);
            Collection<CloudResource> buildableResources = new ArrayList<>();
            Long privateId = instance.getTemplate().getPrivateId();
            try {
                Variant variant = auth.getCloudContext().getVariant();
                List<ComputeResourceBuilder<ResourceBuilderContext>> compute = resourceBuilders.compute(variant);
                for (ComputeResourceBuilder<ResourceBuilderContext> builder : compute) {
                    LOGGER.info("Start building '{} ({})' resources of '{}' instance group of '{}' stack", builder.resourceType(),
                            builder.getClass().getSimpleName(), group.getName(), stackName);
                    List<CloudResource> cloudResources = builder.create(context, instance, privateId, auth, group, cloudStack.getImage());
                    if (!CollectionUtils.isEmpty(cloudResources)) {
                        buildableResources.addAll(cloudResources);
                        persistResources(auth, cloudResources);

                        PollGroup pollGroup = InMemoryStateStore.getStack(auth.getCloudContext().getId());
                        if (isCancelled(pollGroup)) {
                            throw new CancellationException(format("Building of %s has been cancelled", cloudResources));
                        }

                        List<CloudResource> resources = builder.build(context, instance, privateId, auth, group, cloudResources, cloudStack);
                        updateResource(auth, resources);
                        context.addComputeResources(privateId, resources);

                        if (ResourceType.GCP_INSTANCE.equals(builder.resourceType())) {
                            LOGGER.debug("Skip instance polling in case of GCP");
                            resources.stream().map(resource -> new CloudResourceStatus(resource, ResourceStatus.IN_PROGRESS, privateId)).forEach(results::add);
                        } else {
                            PollTask<List<CloudResourceStatus>> task = resourcePollTaskFactory.newPollResourceTask(builder, auth, resources, context, true);
                            List<CloudResourceStatus> pollerResult = scheduleTask(task, resources);
                            for (CloudResourceStatus resourceStatus : pollerResult) {
                                resourceStatus.setPrivateId(privateId);
                            }
                            results.addAll(pollerResult);
                        }
                    }
                    LOGGER.info("Finished building '{} ({})' resources of '{}' instance group of '{}' stack", builder.resourceType(),
                            builder.getClass().getSimpleName(), group.getName(), stackName);
                }
            } catch (CancellationException e) {
                throw e;
            } catch (Exception e) {
                String errorMessage = format("Failed to create resources for instance with id: '%s', message: '%s'", instance.getTemplate().getPrivateId(),
                        e.getMessage());
                LOGGER.error(errorMessage, e);
                results.removeIf(crs -> crs.getPrivateId().equals(privateId));
                for (CloudResource buildableResource : buildableResources) {
                    results.add(new CloudResourceStatus(buildableResource, ResourceStatus.FAILED, errorMessage, privateId));
                }
            }
            LOGGER.debug("Finished creating all compute resources for instance: '{}' stack: '{}'", instance, stackName);
        }

        return new ResourceRequestResult<>(FutureResult.SUCCESS, results);
    }

    private List<CloudResourceStatus> scheduleTask(PollTask<List<CloudResourceStatus>> task, List<CloudResource> resources) throws Exception {
        boolean volumeSet = resources != null && resources.stream().allMatch(r -> VOLUME_SET_RESOURCE_TYPES.contains(r.getType()));
        if (volumeSet) {
            LOGGER.debug("Scheduling volume set poller with shorter timeout.");
            return syncPollingScheduler.schedule(task, SyncPollingScheduler.POLLING_INTERVAL, VOLUME_SET_POLLING_ATTEMPTS,
                    SyncPollingScheduler.FAILURE_TOLERANT_ATTEMPT);
        } else {
            LOGGER.debug("Scheduling resource poller with default timeout.");
            return syncPollingScheduler.schedule(task);
        }
    }

    private void persistResources(AuthenticatedContext auth, Iterable<CloudResource> cloudResources) {
        for (CloudResource cloudResource : cloudResources) {
            if (cloudResource.isPersistent()) {
                persistenceNotifier.notifyAllocation(cloudResource, auth.getCloudContext());
            }
        }
    }

    private boolean isCancelled(PollGroup pollGroup) {
        return pollGroup == null || CANCELLED.equals(pollGroup);
    }

    private void updateResource(AuthenticatedContext auth, Iterable<CloudResource> cloudResources) {
        for (CloudResource cloudResource : cloudResources) {
            if (cloudResource.isPersistent()) {
                persistenceNotifier.notifyUpdate(cloudResource, auth.getCloudContext());
            }
        }
    }
}
