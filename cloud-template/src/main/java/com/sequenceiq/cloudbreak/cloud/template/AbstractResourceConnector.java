package com.sequenceiq.cloudbreak.cloud.template;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.inject.Inject;

import com.sequenceiq.cloudbreak.cloud.ResourceConnector;
import com.sequenceiq.cloudbreak.cloud.event.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.event.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.context.ResourceBuilderContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.cloud.template.compute.ComputeResourceService;
import com.sequenceiq.cloudbreak.cloud.template.init.ContextBuilders;
import com.sequenceiq.cloudbreak.cloud.template.network.NetworkResourceService;
import com.sequenceiq.cloudbreak.common.type.AdjustmentType;

public abstract class AbstractResourceConnector implements ResourceConnector {

    @Inject
    private NetworkResourceService networkResourceService;
    @Inject
    private ComputeResourceService computeResourceService;
    @Inject
    private ContextBuilders contextBuilders;

    @Override
    public List<CloudResourceStatus> launch(AuthenticatedContext auth, CloudStack stack, PersistenceNotifier notifier, AdjustmentType adjustmentType,
            Long threshold) throws Exception {
        CloudContext cloudContext = auth.getCloudContext();
        String platform = cloudContext.getPlatform();

        //context
        ResourceBuilderContext context = contextBuilders.get(platform).contextInit(cloudContext, auth, true);

        //network
        List<CloudResourceStatus> networkStatuses = networkResourceService.buildResources(context, auth, stack.getNetwork(), stack.getSecurity());
        context.addNetworkResources(getCloudResources(networkStatuses));

        //compute
        List<CloudResourceStatus> computeStatuses = computeResourceService.buildResourcesForLaunch(context, auth, stack.getGroups(), stack.getImage(),
                adjustmentType, threshold);

        networkStatuses.addAll(computeStatuses);
        return networkStatuses;
    }

    @Override
    public List<CloudResourceStatus> terminate(AuthenticatedContext auth, CloudStack stack, List<CloudResource> cloudResources) throws Exception {
        CloudContext cloudContext = auth.getCloudContext();
        String platform = cloudContext.getPlatform();

        //context
        ResourceBuilderContext context = contextBuilders.get(platform).terminationContextInit(cloudContext, auth, cloudResources);

        //compute
        List<CloudResourceStatus> computeStatuses = computeResourceService.deleteResources(context, auth, cloudResources, false);

        //network
        List<CloudResourceStatus> networkStatuses = networkResourceService.deleteResources(context, auth, cloudResources, false);

        computeStatuses.addAll(networkStatuses);
        return computeStatuses;
    }

    @Override
    public List<CloudResourceStatus> upscale(AuthenticatedContext auth, CloudStack stack, List<CloudResource> resources) throws Exception {
        CloudContext cloudContext = auth.getCloudContext();
        String platform = cloudContext.getPlatform();

        //context
        ResourceBuilderContext context = contextBuilders.get(platform).contextInit(cloudContext, auth, true);

        //network
        context.addNetworkResources(networkResourceService.getNetworkResources(platform, resources));

        //compute
        String scalingGroup = getGroupName(stack);
        Group group = createScalingGroup(stack.getGroups(), scalingGroup);

        return computeResourceService.buildResourcesForUpscale(context, auth, Arrays.asList(group), stack.getImage());
    }

    @Override
    public List<CloudResourceStatus> downscale(AuthenticatedContext auth, CloudStack stack,
            List<CloudResource> resources, List<CloudInstance> vms) throws Exception {
        CloudContext cloudContext = auth.getCloudContext();
        String platform = cloudContext.getPlatform();

        //context
        ResourceBuilderContext context = contextBuilders.get(platform).contextInit(cloudContext, auth, false);

        //compute
        //TODO we should somehow group the corresponding resources together
        List<CloudResource> deleteResources = getDeleteResources(resources, vms);
        return computeResourceService.deleteResources(context, auth, deleteResources, true);
    }

    @Override
    public List<CloudResourceStatus> update(AuthenticatedContext auth, CloudStack stack, List<CloudResource> resources) throws Exception {
        CloudContext cloudContext = auth.getCloudContext();
        String platform = cloudContext.getPlatform();

        //context
        ResourceBuilderContext context = contextBuilders.get(platform).contextInit(cloudContext, auth, true);

        //network
        List<CloudResource> networkResources = networkResourceService.getNetworkResources(platform, resources);
        return networkResourceService.update(context, auth, stack.getNetwork(), stack.getSecurity(), networkResources);
    }

    @Override
    public List<CloudResourceStatus> check(AuthenticatedContext authenticatedContext, List<CloudResource> resources) {
        throw new UnsupportedOperationException();
    }

    private List<CloudResource> getCloudResources(List<CloudResourceStatus> resourceStatuses) {
        List<CloudResource> resources = new ArrayList<>();
        for (CloudResourceStatus status : resourceStatuses) {
            resources.add(status.getCloudResource());
        }
        return resources;
    }

    private Group createScalingGroup(List<Group> originalGroups, String groupName) {
        Group scalingGroup = getGroup(originalGroups, groupName);
        List<InstanceTemplate> instances = scalingGroup.getInstances();
        Iterator<InstanceTemplate> iterator = instances.iterator();
        while (iterator.hasNext()) {
            if (InstanceStatus.CREATE_REQUESTED != iterator.next().getStatus()) {
                iterator.remove();
            }
        }
        scalingGroup.setInstances(instances);
        return scalingGroup;
    }

    private Group getGroup(List<Group> groups, String groupName) {
        Group resultGroup = null;
        for (Group group : groups) {
            if (groupName.equalsIgnoreCase(group.getName())) {
                resultGroup = group;
                break;
            }
        }
        return resultGroup;
    }

    private String getGroupName(CloudStack stack) {
        for (Group group : stack.getGroups()) {
            for (InstanceTemplate instanceTemplate : group.getInstances()) {
                if (InstanceStatus.CREATE_REQUESTED == instanceTemplate.getStatus()) {
                    return instanceTemplate.getGroupName();
                }
            }
        }
        return null;
    }

    private List<CloudResource> getDeleteResources(List<CloudResource> resources, List<CloudInstance> instances) {
        List<CloudResource> result = new ArrayList<>();
        for (CloudInstance instance : instances) {
            String instanceId = instance.getInstanceId();
            for (CloudResource resource : resources) {
                if (instanceId.equalsIgnoreCase(resource.getName())) {
                    result.add(resource);
                }
            }
        }
        return result;
    }
}
