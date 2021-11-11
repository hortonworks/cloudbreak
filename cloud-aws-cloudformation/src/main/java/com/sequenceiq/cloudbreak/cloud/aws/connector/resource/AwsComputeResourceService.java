package com.sequenceiq.cloudbreak.cloud.aws.connector.resource;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.aws.AwsContextService;
import com.sequenceiq.cloudbreak.cloud.aws.common.context.AwsContextBuilder;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.template.compute.ComputeResourceService;
import com.sequenceiq.cloudbreak.cloud.template.context.ResourceBuilderContext;
import com.sequenceiq.common.api.adjustment.AdjustmentTypeWithThreshold;

@Service
public class AwsComputeResourceService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsComputeResourceService.class);

    @Inject
    private AwsContextBuilder contextBuilder;

    @Inject
    private ComputeResourceService computeResourceService;

    @Inject
    private AwsContextService awsContextService;

    public List<CloudResourceStatus> buildComputeResourcesForLaunch(AuthenticatedContext ac, CloudStack stack,
            AdjustmentTypeWithThreshold adjustmentTypeWithThreshold, List<CloudResource> instances, List<CloudResource> networkResources) {
        LOGGER.info("Build AWS compute resources for launch with adjustment type and threshold: {}", adjustmentTypeWithThreshold);
        CloudContext cloudContext = ac.getCloudContext();
        ResourceBuilderContext context = contextBuilder.contextInit(cloudContext, ac, stack.getNetwork(), null, true);
        context.addNetworkResources(networkResources);
        LOGGER.info("Added AWS network resources to resource builder context for launch: {}", networkResources);
        awsContextService.addInstancesToContext(instances, context, stack.getGroups());
        LOGGER.info("Added AWS instances to AWS context for launch: {}", instances);
        return computeResourceService.buildResourcesForLaunch(context, ac, stack, adjustmentTypeWithThreshold);
    }

    public List<CloudResourceStatus> buildComputeResourcesForUpscale(AuthenticatedContext ac, CloudStack stack, List<Group> groupsWithNewInstances,
            List<CloudResource> newInstances, List<CloudResource> reattachableVolumeSets, List<CloudResource> networkResources,
            AdjustmentTypeWithThreshold adjustmentTypeWithThreshold) {
        LOGGER.info("Build AWS compute resources for upscale with adjustment type and threshold: {}. Groups with new instances: {}",
                adjustmentTypeWithThreshold, groupsWithNewInstances);
        CloudContext cloudContext = ac.getCloudContext();
        ResourceBuilderContext context = contextBuilder.contextInit(cloudContext, ac, stack.getNetwork(), null, true);
        context.addNetworkResources(networkResources);
        LOGGER.info("Added AWS network resources to resource builder context for upscale: {}", networkResources);

        if (reattachableVolumeSets.isEmpty()) {
            awsContextService.addInstancesToContext(newInstances, context, groupsWithNewInstances);
            LOGGER.info("Reattachable volume sets is empty. Added instances to AWS context for launch: {}", newInstances);
        } else {
            List<CloudResource> contextResources = new ArrayList<>(newInstances);
            contextResources.addAll(reattachableVolumeSets);
            awsContextService.addResourcesToContext(contextResources, context, groupsWithNewInstances);
            LOGGER.info("Reattachable volume sets are added to AWS context: {}", contextResources);
        }

        return computeResourceService.buildResourcesForUpscale(context, ac, stack, groupsWithNewInstances, adjustmentTypeWithThreshold);
    }

    public List<CloudResourceStatus> deleteComputeResources(AuthenticatedContext ac, CloudStack stack, List<CloudResource> cloudResources) {
        CloudContext cloudContext = ac.getCloudContext();
        ResourceBuilderContext context = contextBuilder.contextInit(cloudContext, ac, stack.getNetwork(), null, true);

        return computeResourceService.deleteResources(context, ac, cloudResources, false);
    }
}
