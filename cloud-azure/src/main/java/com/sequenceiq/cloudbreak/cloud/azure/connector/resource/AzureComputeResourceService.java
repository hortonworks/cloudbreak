package com.sequenceiq.cloudbreak.cloud.azure.connector.resource;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.PlatformParametersConsts;
import com.sequenceiq.cloudbreak.cloud.azure.AzureContextService;
import com.sequenceiq.cloudbreak.cloud.azure.context.AzureContext;
import com.sequenceiq.cloudbreak.cloud.azure.context.AzureContextBuilder;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.template.compute.ComputeResourceService;
import com.sequenceiq.cloudbreak.cloud.template.context.ResourceBuilderContext;
import com.sequenceiq.common.api.adjustment.AdjustmentTypeWithThreshold;

@Service
public class AzureComputeResourceService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AzureComputeResourceService.class);

    @Inject
    private AzureContextBuilder contextBuilder;

    @Inject
    private ComputeResourceService computeResourceService;

    @Inject
    private AzureContextService azureContextService;

    public List<CloudResourceStatus> buildComputeResourcesForLaunch(AuthenticatedContext ac, CloudStack stack,
            AdjustmentTypeWithThreshold adjustmentTypeWithThreshold, List<CloudResource> instances, List<CloudResource> networkResources) {
        LOGGER.info("Build Azure compute resources for launch with adjustment type and threshold: {}", adjustmentTypeWithThreshold);
        ResourceBuilderContext context = initContext(ac, stack);
        context.addNetworkResources(networkResources);
        LOGGER.info("Added Azure network resources to resource builder context for launch: {}", networkResources);

        azureContextService.addInstancesToContext(instances, context, stack.getGroups());
        LOGGER.info("Added Azure instances to Azure context for launch: {}", instances);
        return computeResourceService.buildResourcesForLaunch(context, ac, stack, adjustmentTypeWithThreshold);
    }

    public List<CloudResourceStatus> buildComputeResourcesForUpscale(AuthenticatedContext ac, CloudStack stack, List<Group> groupsWithNewInstances,
            List<CloudResource> newInstances, List<CloudResource> reattachableVolumeSets, List<CloudResource> networkResources,
            AdjustmentTypeWithThreshold adjustmentTypeWithThreshold) {
        LOGGER.info("Build Azure compute resources for upscale with adjustment type and threshold: {}. Groups with new instances: {}",
                adjustmentTypeWithThreshold, groupsWithNewInstances);
        ResourceBuilderContext context = initContext(ac, stack);
        context.addNetworkResources(networkResources);
        LOGGER.info("Added Azure network resources to resource builder context for upscale: {}", networkResources);

        if (reattachableVolumeSets.isEmpty()) {
            azureContextService.addInstancesToContext(newInstances, context, groupsWithNewInstances);
            LOGGER.info("Reattachable volume sets is empty. Added instances to Azure context for launch: {}", newInstances);
        } else {
            List<CloudResource> contextResources = new ArrayList<>(newInstances);
            contextResources.addAll(reattachableVolumeSets);
            azureContextService.addResourcesToContext(contextResources, context, groupsWithNewInstances);
            LOGGER.info("Reattachable volume sets are added to Azure context: {}", contextResources);
        }
        return computeResourceService.buildResourcesForUpscale(context, ac, stack, groupsWithNewInstances, adjustmentTypeWithThreshold);
    }

    public List<CloudResourceStatus> deleteComputeResources(AuthenticatedContext ac, CloudStack stack, List<CloudResource> cloudResources,
            List<CloudResource> networkResources) {
        ResourceBuilderContext context = initContext(ac, stack);
        context.addNetworkResources(networkResources);

        return computeResourceService.deleteResources(context, ac, cloudResources, false);
    }

    private AzureContext initContext(AuthenticatedContext ac, CloudStack cloudStack) {
        AzureContext context = contextBuilder.contextInit(ac.getCloudContext(), ac, cloudStack.getNetwork(), null, true);
        String stackCrn = cloudStack.getParameters().getOrDefault(PlatformParametersConsts.RESOURCE_CRN_PARAMETER, "");
        if (stackCrn.isEmpty()) {
            LOGGER.warn("Stack crn is not set in CloudStack, it can cause errors during infrastructure creation!");
        }
        context.putParameter(PlatformParametersConsts.RESOURCE_CRN_PARAMETER,
                cloudStack.getParameters().getOrDefault(PlatformParametersConsts.RESOURCE_CRN_PARAMETER, ""));
        return context;
    }
}
