package com.sequenceiq.cloudbreak.cloud.aws.connector.resource;

import static com.sequenceiq.common.api.type.ResourceType.AWS_CLOUD_WATCH;
import static com.sequenceiq.common.api.type.ResourceType.AWS_INSTANCE;
import static com.sequenceiq.common.api.type.ResourceType.AWS_SECURITY_GROUP;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.aws.AwsContextService;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsConstants;
import com.sequenceiq.cloudbreak.cloud.aws.common.context.AwsContextBuilder;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.template.compute.ComputeResourceService;
import com.sequenceiq.cloudbreak.cloud.template.context.ResourceBuilderContext;
import com.sequenceiq.cloudbreak.cloud.template.group.GroupResourceService;
import com.sequenceiq.common.api.adjustment.AdjustmentTypeWithThreshold;
import com.sequenceiq.common.api.type.ResourceType;

@Service
public class AwsComputeResourceService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsComputeResourceService.class);

    private static final Set<ResourceType> AWS_NATIVE_RESOURCE_TYPES = Set.of(AWS_SECURITY_GROUP, AWS_INSTANCE, AWS_CLOUD_WATCH);

    @Inject
    private AwsContextBuilder contextBuilder;

    @Inject
    private ComputeResourceService computeResourceService;

    @Inject
    private AwsContextService awsContextService;

    @Inject
    private GroupResourceService groupResourceService;

    public List<CloudResourceStatus> buildComputeResourcesForLaunch(AuthenticatedContext ac, CloudStack stack,
            AdjustmentTypeWithThreshold adjustmentTypeWithThreshold, List<CloudResource> instances, List<CloudResource> networkResources) {
        LOGGER.info("Build AWS compute resources for launch with adjustment type and threshold: {}", adjustmentTypeWithThreshold);
        CloudContext cloudContext = ac.getCloudContext();
        ResourceBuilderContext context = contextBuilder.contextInit(cloudContext, ac, stack.getNetwork(), true);
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
        ResourceBuilderContext context = contextBuilder.contextInit(cloudContext, ac, stack.getNetwork(), true);
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
        List<CloudResourceStatus> cloudResourceStatuses = new ArrayList<>();
        CloudContext cloudContext = ac.getCloudContext();
        ResourceBuilderContext context = contextBuilder.contextInit(cloudContext, ac, stack.getNetwork(), true);

        cloudResourceStatuses.addAll(computeResourceService.deleteResources(context, ac, cloudResources, false, true));
        cloudResourceStatuses.addAll(deleteAwsNativeResourcesIfNecessary(ac, cloudResources, stack, cloudResourceStatuses));
        return cloudResourceStatuses;
    }

    private List<CloudResourceStatus> deleteAwsNativeResourcesIfNecessary(AuthenticatedContext ac, List<CloudResource> cloudResources,
            CloudStack stack, List<CloudResourceStatus> cloudResourceStatuses) {
        List<CloudResourceStatus> result =  new ArrayList<>();
        List<String> deletedResourceNames = cloudResourceStatuses.stream()
                .map(cloudResourceStatus -> cloudResourceStatus.getCloudResource().getName())
                .toList();
        List<CloudResource> nonDeletedResources =
                cloudResources.stream()
                        .filter(cloudResource -> !deletedResourceNames.contains(cloudResource.getName()))
                        .toList();
        boolean anyAwsNativeResourcesFound = nonDeletedResources.stream()
                .anyMatch(csr -> AWS_NATIVE_RESOURCE_TYPES.contains(csr.getType()));

        if (anyAwsNativeResourcesFound) {
            ResourceBuilderContext context = contextBuilder.contextInit(ac.getCloudContext(), ac, stack.getNetwork(), false);
            result.addAll(deleteAwsNativeResources(ac, context, stack, nonDeletedResources));
        }
        return result;
    }

    private List<CloudResourceStatus> deleteAwsNativeResources(AuthenticatedContext ac, ResourceBuilderContext context, CloudStack stack,
            List<CloudResource> nonDeletedCloudResources) {
        List<CloudResourceStatus> result = new ArrayList<>();
        LOGGER.info("The non-deleted cloud resource statuses indicate that AWS_NATIVE resources also exist, triggering deletion of them: {}",
                nonDeletedCloudResources);
        CloudContext nativeCloudContext = ac.getCloudContext()
                .createPrototype()
                .withVariant(AwsConstants.AwsVariant.AWS_NATIVE_VARIANT.variant())
                .build();
        AuthenticatedContext nativeAuthenticatedContext = new AuthenticatedContext(nativeCloudContext, ac.getCloudCredential());
        LOGGER.debug("Starting to delete compute resources for AWS NATIVE");
        result.addAll(computeResourceService.deleteResources(context, nativeAuthenticatedContext, nonDeletedCloudResources, false, true));
        result.addAll(deleteGroupResources(context, stack, nonDeletedCloudResources, nativeAuthenticatedContext));
        return result;
    }

    private List<CloudResourceStatus> deleteGroupResources(ResourceBuilderContext context, CloudStack stack, List<CloudResource> nonDeletedCloudResources,
            AuthenticatedContext authenticatedContext) {
        List<CloudResourceStatus> result = new  ArrayList<>();
        if (nonDeletedCloudResources.stream().anyMatch(cr -> AWS_SECURITY_GROUP.equals(cr.getType()))) {
            LOGGER.debug("Starting to delete group resources for AWS NATIVE");
            try {
                result.addAll(groupResourceService.deleteResources(context, authenticatedContext, nonDeletedCloudResources, stack.getNetwork(), false));
            } catch (Exception e) {
                String msg = "Failed to delete group resources for AWS NATIVE";
                LOGGER.warn(msg, e);
                throw new CloudConnectorException(msg, e);
            }
        }
        return result;
    }
}
