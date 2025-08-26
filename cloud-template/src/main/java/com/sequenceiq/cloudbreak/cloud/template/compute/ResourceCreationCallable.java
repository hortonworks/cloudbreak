package com.sequenceiq.cloudbreak.cloud.template.compute;

import static com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup.CANCELLED;
import static java.lang.String.format;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

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

public class ResourceCreationCallable implements Callable<List<CloudResourceStatus>> {

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
    public List<CloudResourceStatus> call() {
        List<CloudResourceStatus> results = new ArrayList<>();
        String stackName = auth.getCloudContext().getName();
        for (CloudInstance instance : instances) {
            LOGGER.debug("Create all compute resources for instance: '{}' stack: '{}'", instance, stackName);
            Long privateId = instance.getTemplate().getPrivateId();
            try {
                Variant variant = auth.getCloudContext().getVariant();
                List<ComputeResourceBuilder<ResourceBuilderContext>> compute = resourceBuilders.compute(variant);
                boolean failedComputeCreation = false;
                for (ComputeResourceBuilder<ResourceBuilderContext> builder : compute) {
                    if (failedComputeCreation) {
                        LOGGER.info("Skipping compute resource creation for {} of '{}' instance group of '{}' stack",
                                builder.resourceType(),
                                group.getName(),
                                stackName);
                    } else {
                        LOGGER.info("Start building '{} ({})' resources of '{}' instance group of '{}' stack, for private id: {}",
                                builder.resourceType(),
                                builder.getClass().getSimpleName(),
                                group.getName(),
                                stackName,
                                privateId);
                        List<CloudResourceStatus> computeResults = createComputeResources(instance, privateId, builder);
                        results.addAll(computeResults);
                        if (containsFailed(computeResults)) {
                            failedComputeCreation = true;
                        }
                    }
                    LOGGER.info("Finished building '{} ({})' resources of '{}' instance group of '{}' stack, for private id: '{}'", builder.resourceType(),
                            builder.getClass().getSimpleName(), group.getName(), stackName, privateId);
                }
            } catch (CancellationException e) {
                LOGGER.warn("Cancellation exception has been arrived, throwing forward: {}", e.getMessage());
                throw e;
            }
            LOGGER.debug("Finished creating all compute resources for instance: '{}' stack: '{}'", instance, stackName);
        }

        return results;
    }

    private List<CloudResourceStatus> createComputeResources(CloudInstance instance, Long privateId, ComputeResourceBuilder<ResourceBuilderContext> builder) {
        List<CloudResourceStatus> computeResults = new ArrayList<>();
        List<CloudResource> cloudResources = builder.create(context, instance, privateId, auth, group, cloudStack.getImage());
        if (!CollectionUtils.isEmpty(cloudResources)) {
            try {
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
                    resources.stream().map(resource -> new CloudResourceStatus(resource, ResourceStatus.IN_PROGRESS, privateId)).forEach(computeResults::add);
                } else {
                    PollTask<List<CloudResourceStatus>> task = resourcePollTaskFactory.newPollResourceTask(builder, auth, resources, context, true);
                    List<CloudResourceStatus> pollerResult = scheduleTask(task, resources);
                    for (CloudResourceStatus resourceStatus : pollerResult) {
                        resourceStatus.setPrivateId(privateId);
                    }
                    computeResults.addAll(pollerResult);
                }
            } catch (Exception e) {
                String resources = cloudResources.stream().map(CloudResource::getDetailedInfo).collect(Collectors.joining(", "));
                LOGGER.error("Resource creation failed: {}", resources, e);
                computeResults.removeIf(crs -> crs.getPrivateId().equals(privateId));
                for (CloudResource cloudResource : cloudResources) {
                    computeResults.add(new CloudResourceStatus(cloudResource, ResourceStatus.FAILED, e.getMessage(), privateId));
                }
            }
        }
        return computeResults;
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

    private void persistResources(AuthenticatedContext auth, List<CloudResource> cloudResources) {
        List<CloudResource> resourcesToPersist = cloudResources.stream()
                .filter(CloudResource::isPersistent)
                .collect(Collectors.toList());
        persistenceNotifier.notifyAllocations(resourcesToPersist, auth.getCloudContext());
    }

    private boolean isCancelled(PollGroup pollGroup) {
        return pollGroup == null || CANCELLED.equals(pollGroup);
    }

    private void updateResource(AuthenticatedContext auth, List<CloudResource> cloudResources) {
        List<CloudResource> resourcesToUpdate = cloudResources.stream()
                .filter(CloudResource::isPersistent)
                .collect(Collectors.toList());
        if (!resourcesToUpdate.isEmpty()) {
            persistenceNotifier.notifyUpdates(resourcesToUpdate, auth.getCloudContext());
        }
    }

    private boolean containsFailed(List<CloudResourceStatus> results) {
        return results.stream().anyMatch(resultStatus -> ResourceStatus.FAILED.equals(resultStatus.getStatus()));
    }
}
