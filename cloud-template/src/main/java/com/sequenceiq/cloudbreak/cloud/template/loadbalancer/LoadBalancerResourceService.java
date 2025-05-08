package com.sequenceiq.cloudbreak.cloud.template.loadbalancer;

import static com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup.CANCELLED;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudLoadBalancer;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup;
import com.sequenceiq.cloudbreak.cloud.scheduler.SyncPollingScheduler;
import com.sequenceiq.cloudbreak.cloud.store.InMemoryStateStore;
import com.sequenceiq.cloudbreak.cloud.task.PollTask;
import com.sequenceiq.cloudbreak.cloud.template.LoadBalancerResourceBuilder;
import com.sequenceiq.cloudbreak.cloud.template.ResourceNotNeededException;
import com.sequenceiq.cloudbreak.cloud.template.context.ResourceBuilderContext;
import com.sequenceiq.cloudbreak.cloud.template.init.ResourceBuilders;
import com.sequenceiq.cloudbreak.cloud.template.task.ResourcePollTaskFactory;
import com.sequenceiq.common.api.type.ResourceType;

@Service
public class LoadBalancerResourceService {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoadBalancerResourceService.class);

    @Inject
    private ResourceBuilders resourceBuilders;

    @Inject
    private PersistenceNotifier resourceNotifier;

    @Inject
    private ResourcePollTaskFactory statusCheckFactory;

    @Inject
    private SyncPollingScheduler<List<CloudResourceStatus>> syncPollingScheduler;

    public List<CloudResourceStatus> buildResources(ResourceBuilderContext context, AuthenticatedContext auth, CloudStack stack) throws Exception {
        CloudContext cloudContext = auth.getCloudContext();
        List<CloudResourceStatus> results = new ArrayList<>();
        for (LoadBalancerResourceBuilder<ResourceBuilderContext> builder : resourceBuilders.loadBalancer(cloudContext.getVariant())) {
            for (CloudLoadBalancer loadBalancer : stack.getLoadBalancers()) {
                PollGroup pollGroup = InMemoryStateStore.getStack(auth.getCloudContext().getId());
                if (CANCELLED.equals(pollGroup)) {
                    break;
                }
                try {
                    List<CloudResource> buildableResources = builder.create(context, auth, loadBalancer, stack.getNetwork());
                    createResource(auth, buildableResources);
                    LOGGER.debug("Building resources [{}], with {} for {}",
                            buildableResources.stream().map(CloudResource::getName).collect(Collectors.joining(",")),
                            builder.getClass().getName(), context.getName());
                    List<CloudResource> resources = builder.build(context, auth, buildableResources, loadBalancer, stack);
                    updateResource(auth, resources);
                    PollTask<List<CloudResourceStatus>> task = statusCheckFactory.newPollResourceTask(builder, auth,
                            resources, context, true);
                    List<CloudResourceStatus> pollerResult = syncPollingScheduler.schedule(task);
                    context.addLoadBalancerResources(loadBalancer.getType(), resources);
                    results.addAll(pollerResult);
                } catch (ResourceNotNeededException e) {
                    LOGGER.debug("Skipping resource creation: {}", e.getMessage());
                }
            }
        }
        return results;

    }

    public List<CloudResourceStatus> deleteResources(ResourceBuilderContext context, AuthenticatedContext auth,
            List<CloudResource> cloudResources, boolean cancellable) throws Exception {
        CloudContext cloudContext = auth.getCloudContext();
        List<CloudResourceStatus> results = new ArrayList<>();
        List<LoadBalancerResourceBuilder<ResourceBuilderContext>> builderChain = resourceBuilders.loadBalancer(cloudContext.getVariant());
        for (int i = builderChain.size() - 1; i >= 0; i--) {
            LoadBalancerResourceBuilder<ResourceBuilderContext> builder = builderChain.get(i);
            List<CloudResource> specificResources = getResources(cloudResources, builder.resourceType());
            for (CloudResource resource : specificResources) {
                if (resource.getStatus().resourceExists()) {
                    LOGGER.debug("deleting resources [{}], with {} for {}",
                            resource.getName(), builder.getClass().getName(), context.getName());

                    CloudResource deletedResource = builder.delete(context, auth, resource);
                    if (deletedResource != null) {
                        PollTask<List<CloudResourceStatus>> task = statusCheckFactory.newPollResourceTask(
                                builder, auth, Collections.singletonList(deletedResource), context, cancellable);
                        List<CloudResourceStatus> pollerResult = syncPollingScheduler.schedule(task);
                        results.addAll(pollerResult);
                    }
                }
                resourceNotifier.notifyDeletion(resource, cloudContext);
            }
        }
        return results;
    }

    protected void createResource(AuthenticatedContext auth, List<CloudResource> buildableResources) {
        for (CloudResource buildableResource : buildableResources) {
            if (buildableResource.isPersistent()) {
                resourceNotifier.notifyAllocation(buildableResource, auth.getCloudContext());
            }
        }
    }

    protected void updateResource(AuthenticatedContext auth, List<CloudResource> buildableResources) {
        for (CloudResource buildableResource : buildableResources) {
            if (buildableResource.isPersistent()) {
                resourceNotifier.notifyUpdate(buildableResource, auth.getCloudContext());
            }
        }
    }

    private List<CloudResource> getResources(Collection<CloudResource> resources, ResourceType type) {
        return getResources(resources, Collections.singletonList(type));
    }

    private List<CloudResource> getResources(Collection<CloudResource> resources, Collection<ResourceType> types) {
        List<CloudResource> filtered = new ArrayList<>(resources.size());
        for (CloudResource resource : resources) {
            if (types.contains(resource.getType())) {
                filtered.add(resource);
            }
        }
        return filtered;
    }

}
