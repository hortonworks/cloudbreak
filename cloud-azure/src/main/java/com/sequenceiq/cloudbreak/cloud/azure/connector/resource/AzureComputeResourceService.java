package com.sequenceiq.cloudbreak.cloud.azure.connector.resource;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.azure.AzureContextService;
import com.sequenceiq.cloudbreak.cloud.azure.context.AzureContextBuilder;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.template.compute.ComputeResourceService;
import com.sequenceiq.cloudbreak.cloud.template.context.ResourceBuilderContext;
import com.sequenceiq.common.api.type.AdjustmentType;

@Service
public class AzureComputeResourceService {

    @Inject
    private AzureContextBuilder contextBuilder;

    @Inject
    private ComputeResourceService computeResourceService;

    @Inject
    private AzureContextService azureContextService;

    public List<CloudResourceStatus> buildComputeResourcesForLaunch(AuthenticatedContext ac, CloudStack stack, AdjustmentType adjustmentType, Long threshold,
            List<CloudResource> instances, List<CloudResource> networkResources) {
        CloudContext cloudContext = ac.getCloudContext();
        ResourceBuilderContext context = contextBuilder.contextInit(cloudContext, ac, stack.getNetwork(), null, true);
        context.addNetworkResources(networkResources);

        azureContextService.addInstancesToContext(instances, context, stack.getGroups());
        return computeResourceService.buildResourcesForLaunch(context, ac, stack, adjustmentType, threshold);
    }

    public List<CloudResourceStatus> buildComputeResourcesForUpscale(AuthenticatedContext ac, CloudStack stack, List<Group> groupsWithNewInstances,
            List<CloudResource> newInstances, List<CloudResource> reattachableVolumeSets, List<CloudResource> networkResources) {
        CloudContext cloudContext = ac.getCloudContext();
        ResourceBuilderContext context = contextBuilder.contextInit(cloudContext, ac, stack.getNetwork(), null, true);
        context.addNetworkResources(networkResources);

        if (reattachableVolumeSets.isEmpty()) {
            azureContextService.addInstancesToContext(newInstances, context, groupsWithNewInstances);
        } else {
            List<CloudResource> contextResources = new ArrayList<>(newInstances);
            contextResources.addAll(reattachableVolumeSets);
            azureContextService.addResourcesToContext(contextResources, context, groupsWithNewInstances);
        }
        return computeResourceService.buildResourcesForUpscale(context, ac, stack, groupsWithNewInstances);
    }

    public List<CloudResourceStatus> deleteComputeResources(AuthenticatedContext ac, CloudStack stack, List<CloudResource> cloudResources,
            List<CloudResource> networkResources) {
        CloudContext cloudContext = ac.getCloudContext();
        ResourceBuilderContext context = contextBuilder.contextInit(cloudContext, ac, stack.getNetwork(), null, true);
        context.addNetworkResources(networkResources);

        return computeResourceService.deleteResources(context, ac, cloudResources, false);
    }
}
