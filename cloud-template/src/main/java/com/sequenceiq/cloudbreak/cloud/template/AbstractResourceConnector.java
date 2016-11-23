package com.sequenceiq.cloudbreak.cloud.template;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.inject.Inject;

import com.sequenceiq.cloudbreak.api.model.AdjustmentType;
import com.sequenceiq.cloudbreak.cloud.ResourceConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.cloud.template.compute.ComputeResourceService;
import com.sequenceiq.cloudbreak.cloud.template.context.ResourceBuilderContext;
import com.sequenceiq.cloudbreak.cloud.template.group.GroupResourceService;
import com.sequenceiq.cloudbreak.cloud.template.init.ContextBuilders;
import com.sequenceiq.cloudbreak.cloud.template.network.NetworkResourceService;

/**
 * Abstract base implementation of {@link ResourceConnector} for cloud provider which do not have template based deployments. It provides the
 * functionality to call the resource builders in order starting from the {@link NetworkResourceBuilder} and continueing with the
 * {@link ComputeResourceBuilder}. Before calling any resource builder it constructs a generic {@link ResourceBuilderContext}. This context object
 * will be extended with the created resources as the builder finish creating them. The resources are grouped by private id.
 * <p/>
 * Compute resource can be rolled back based on the different failure policies configured. Network resource failure immediately results in a failing deployment.
 */
public abstract class AbstractResourceConnector implements ResourceConnector {

    @Inject
    private NetworkResourceService networkResourceService;

    @Inject
    private GroupResourceService groupResourceService;

    @Inject
    private ComputeResourceService computeResourceService;

    @Inject
    private ContextBuilders contextBuilders;

    @Override
    public List<CloudResourceStatus> launch(AuthenticatedContext auth, CloudStack stack, PersistenceNotifier notifier,
            AdjustmentType adjustmentType, Long threshold) throws Exception {
        CloudContext cloudContext = auth.getCloudContext();
        Platform platform = cloudContext.getPlatform();

        //context
        ResourceBuilderContext context = contextBuilders.get(platform).contextInit(cloudContext, auth, stack.getNetwork(), null, true);

        //network
        List<CloudResourceStatus> networkStatuses = networkResourceService.buildResources(context, auth, stack.getNetwork(), stack.getCloudSecurity());
        context.addNetworkResources(getCloudResources(networkStatuses));

        //group
        List<CloudResourceStatus> groupStatuses = groupResourceService.buildResources(context, auth, stack.getGroups(), stack.getNetwork(),
                stack.getCloudSecurity());
        networkStatuses.addAll(groupStatuses);

        //compute
        List<CloudResourceStatus> computeStatuses = computeResourceService.buildResourcesForLaunch(context, auth, stack.getGroups(), stack.getImage(),
                adjustmentType, threshold);
        networkStatuses.addAll(computeStatuses);

        return networkStatuses;
    }

    @Override
    public List<CloudResourceStatus> terminate(AuthenticatedContext auth, CloudStack stack, List<CloudResource> cloudResources) throws Exception {
        CloudContext cloudContext = auth.getCloudContext();
        Platform platform = cloudContext.getPlatform();

        //context
        ResourceBuilderContext context = contextBuilders.get(platform).contextInit(cloudContext, auth, stack.getNetwork(), cloudResources, false);

        //compute
        List<CloudResourceStatus> computeStatuses = computeResourceService.deleteResources(context, auth, cloudResources, false);

        //group
        List<CloudResourceStatus> groupStatuses = groupResourceService.deleteResources(context, auth, cloudResources, stack.getNetwork(), false);
        computeStatuses.addAll(groupStatuses);

        //network
        List<CloudResourceStatus> networkStatuses = networkResourceService.deleteResources(context, auth, cloudResources, stack.getNetwork(), false);
        computeStatuses.addAll(networkStatuses);

        return computeStatuses;
    }

    @Override
    public List<CloudResourceStatus> upscale(AuthenticatedContext auth, CloudStack stack, List<CloudResource> resources) throws Exception {
        CloudContext cloudContext = auth.getCloudContext();
        Platform platform = cloudContext.getPlatform();

        //context
        ResourceBuilderContext context = contextBuilders.get(platform).contextInit(cloudContext, auth, stack.getNetwork(), resources, true);

        //network
        context.addNetworkResources(networkResourceService.getNetworkResources(platform, resources));

        Group scalingGroup = getScalingGroup(getGroup(stack.getGroups(), getGroupName(stack)));

        //group
        context.addGroupResources(scalingGroup.getName(), groupResourceService.getGroupResources(platform, resources));

        //compute
        return computeResourceService.buildResourcesForUpscale(context, auth, Collections.singletonList(scalingGroup), stack.getImage());
    }

    @Override
    public List<CloudResourceStatus> downscale(AuthenticatedContext auth, CloudStack stack,
            List<CloudResource> resources, List<CloudInstance> vms) {
        CloudContext cloudContext = auth.getCloudContext();
        Platform platform = cloudContext.getPlatform();

        //context
        ResourceBuilderContext context = contextBuilders.get(platform).contextInit(cloudContext, auth, stack.getNetwork(), resources, false);

        //compute
        List<CloudResource> deleteResources = getDeleteResources(resources, vms);
        return computeResourceService.deleteResources(context, auth, deleteResources, true);
    }

    @Override
    public List<CloudResourceStatus> update(AuthenticatedContext auth, CloudStack stack, List<CloudResource> resources) throws Exception {
        CloudContext cloudContext = auth.getCloudContext();
        Platform platform = cloudContext.getPlatform();

        //context
        ResourceBuilderContext context = contextBuilders.get(platform).contextInit(cloudContext, auth, stack.getNetwork(), resources, true);

        //group
        List<CloudResource> groupResources = groupResourceService.getGroupResources(platform, resources);
        List<CloudResourceStatus> groupStatuses = groupResourceService.update(context, auth, stack.getNetwork(), stack.getCloudSecurity(), groupResources);

        //network
        List<CloudResource> networkResources = networkResourceService.getNetworkResources(platform, resources);
        List<CloudResourceStatus> networkStatuses = networkResourceService.update(context, auth, stack.getNetwork(), stack.getCloudSecurity(), networkResources);

        groupStatuses.addAll(networkStatuses);
        return groupStatuses;
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

    private Group getScalingGroup(Group scalingGroup) {
        List<CloudInstance> instances = new ArrayList<>(scalingGroup.getInstances());
        Iterator<CloudInstance> iterator = instances.iterator();
        while (iterator.hasNext()) {
            if (InstanceStatus.CREATE_REQUESTED != iterator.next().getTemplate().getStatus()) {
                iterator.remove();
            }
        }
        return new Group(scalingGroup.getName(), scalingGroup.getType(), instances, scalingGroup.getSecurity());
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
            for (CloudInstance instance : group.getInstances()) {
                InstanceTemplate instanceTemplate = instance.getTemplate();
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
