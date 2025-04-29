package com.sequenceiq.cloudbreak.cloud.aws.connector.resource;

import static java.util.Collections.singletonList;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.inject.Inject;

import org.apache.commons.collections4.ListUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.aws.AwsCloudFormationClient;
import com.sequenceiq.cloudbreak.cloud.aws.AwsMetadataCollector;
import com.sequenceiq.cloudbreak.cloud.aws.AwsSyncUserDataService;
import com.sequenceiq.cloudbreak.cloud.aws.CloudFormationStackUtil;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonAutoScalingClient;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonCloudFormationClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsTaggingService;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.aws.common.connector.resource.AwsElasticIpService;
import com.sequenceiq.cloudbreak.cloud.aws.common.connector.resource.AwsNetworkService;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsNetworkView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudLoadBalancer;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.cloudbreak.cloud.transform.CloudResourceHelper;
import com.sequenceiq.common.api.adjustment.AdjustmentTypeWithThreshold;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.autoscaling.model.AutoScalingGroup;
import software.amazon.awssdk.services.autoscaling.model.DescribeAutoScalingGroupsRequest;
import software.amazon.awssdk.services.autoscaling.model.Instance;
import software.amazon.awssdk.services.ec2.model.InstanceStateName;

@Service
public class AwsUpscaleService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AwsUpscaleService.class);

    private static final List<String> UPSCALE_PROCESSES = singletonList("Launch");

    @Inject
    private AwsNetworkService awsNetworkService;

    @Inject
    private AwsCloudFormationClient awsClient;

    @Inject
    private CloudFormationStackUtil cfStackUtil;

    @Inject
    private CloudResourceHelper cloudResourceHelper;

    @Inject
    private AwsAutoScalingService awsAutoScalingService;

    @Inject
    private AwsComputeResourceService awsComputeResourceService;

    @Inject
    private AwsElasticIpService awsElasticIpService;

    @Inject
    private AwsTaggingService awsTaggingService;

    @Inject
    private AwsCloudWatchService awsCloudWatchService;

    @Inject
    private AwsMetadataCollector awsMetadataCollector;

    @Inject
    private AwsSyncUserDataService syncUserDataService;

    @Inject
    private EntitlementService entitlementService;

    public List<CloudResourceStatus> upscale(AuthenticatedContext ac, CloudStack stack, List<CloudResource> resources,
            AdjustmentTypeWithThreshold adjustmentTypeWithThreshold) {
        LOGGER.info("Upscale AWS cluster with adjustment and threshold: {}. Resources: {}", adjustmentTypeWithThreshold, resources);
        String regionName = ac.getCloudContext().getLocation().getRegion().value();
        AwsCredentialView credentialView = new AwsCredentialView(ac.getCloudCredential());

        AmazonCloudFormationClient cloudFormationClient = awsClient.createCloudFormationClient(credentialView, regionName);
        AmazonAutoScalingClient amazonASClient = awsClient.createAutoScalingClient(credentialView, regionName);
        AmazonEc2Client amazonEC2Client = awsClient.createEc2Client(credentialView, regionName);
        syncUserDataService.syncUserData(ac, stack, resources);
        List<Group> scaledGroups = cloudResourceHelper.getScaledGroups(stack);
        Map<String, Group> desiredAutoscalingGroupsByName = getAutoScaleGroupsByNameFromCloudFormationTemplate(ac, cloudFormationClient, scaledGroups);
        LOGGER.info("Desired autoscaling groups: {}", desiredAutoscalingGroupsByName);
        awsAutoScalingService.resumeAutoScaling(amazonASClient, desiredAutoscalingGroupsByName.keySet(), UPSCALE_PROCESSES);
        Map<String, Integer> originalAutoScalingGroupsBySize = getAutoScalingGroupsBySize(desiredAutoscalingGroupsByName.keySet(), amazonASClient);
        LOGGER.info("Update autoscaling groups for stack: {}", ac.getCloudContext().getName());
        Date timeBeforeASUpdate = new Date();
        updateAutoscalingGroups(amazonASClient, desiredAutoscalingGroupsByName, originalAutoScalingGroupsBySize);
        List<String> knownInstances = getKnownInstancesByCloudbreak(stack);
        try {
            awsAutoScalingService.scheduleStatusChecks(scaledGroups, ac, cloudFormationClient, timeBeforeASUpdate, knownInstances);
        } catch (AmazonAutoscalingFailedException amazonAutoscalingFailedException) {
            LOGGER.warn("Amazon autoscaling group update failed", amazonAutoscalingFailedException);
            recoverOriginalState(ac, stack, amazonASClient, desiredAutoscalingGroupsByName, originalAutoScalingGroupsBySize, amazonAutoscalingFailedException);
            sendASGUpdateFailedMessage(amazonASClient, desiredAutoscalingGroupsByName, amazonAutoscalingFailedException);
        }
        try {
            awsAutoScalingService.suspendAutoScaling(ac, stack);
            validateInstanceStatusesInScaledGroups(ac, amazonASClient, amazonEC2Client, cloudFormationClient, scaledGroups);
            List<CloudResource> instances = cfStackUtil.getInstanceCloudResources(ac, cloudFormationClient, amazonASClient, scaledGroups);
            associateElasticIpWithNewInstances(stack, resources, cloudFormationClient, amazonEC2Client, scaledGroups, instances);

            List<Group> groupsWithNewInstances = getGroupsWithNewInstances(scaledGroups);
            List<CloudResource> newInstances = getNewInstances(scaledGroups, instances);
            List<CloudResource> reattachableVolumeSets = getReattachableVolumeSets(resources);
            List<CloudResource> networkResources = resources.stream()
                    .filter(cloudResource -> ResourceType.AWS_SUBNET.equals(cloudResource.getType()))
                    .collect(Collectors.toList());
            cloudResourceHelper.updateDeleteOnTerminationFlag(reattachableVolumeSets, false, ac.getCloudContext());
            List<CloudResourceStatus> cloudResourceStatuses = awsComputeResourceService
                    .buildComputeResourcesForUpscale(ac, stack, groupsWithNewInstances, newInstances, reattachableVolumeSets, networkResources,
                            adjustmentTypeWithThreshold);

            List<String> failedResources = cloudResourceStatuses.stream()
                    .filter(cloudResourceStatus -> ResourceStatus.FAILED == cloudResourceStatus.getStatus())
                    .map(status -> status.getCloudResource().getDetailedInfo() + ": " + status.getStatusReason()).toList();
            if (!failedResources.isEmpty()) {
                awsComputeResourceService.deleteComputeResources(ac, stack, cloudResourceStatuses.stream().map(CloudResourceStatus::getCloudResource).toList());
                throw new RuntimeException("Additional resource creation failed: " + failedResources);
            }
            cloudResourceHelper.updateDeleteOnTerminationFlag(reattachableVolumeSets, true, ac.getCloudContext());
            awsTaggingService.tagRootVolumes(ac, amazonEC2Client, instances, stack.getTags());
            awsCloudWatchService.addCloudWatchAlarmsForSystemFailures(instances, regionName, credentialView, stack.getTags());

            for (CloudLoadBalancer loadBalancer : stack.getLoadBalancers()) {
                cfStackUtil.addLoadBalancerTargets(ac, loadBalancer, newInstances);
            }
            List<CloudResourceStatus> successfulInstances = newInstances.stream()
                    .map(cloudResource ->
                            new CloudResourceStatus(cloudResource, ResourceStatus.CREATED, cloudResource.getParameter(CloudResource.PRIVATE_ID, Long.class)))
                    .collect(Collectors.toList());
            return ListUtils.union(singletonList(new CloudResourceStatus(cfStackUtil.getCloudFormationStackResource(resources), ResourceStatus.UPDATED)),
                    successfulInstances);
        } catch (RuntimeException runtimeException) {
            recoverOriginalState(ac, stack, amazonASClient, desiredAutoscalingGroupsByName,
                    originalAutoScalingGroupsBySize, runtimeException);
            throw new CloudConnectorException(String.format("Failed to create some resource on AWS for upscaled nodes, please check your quotas on AWS. " +
                    "Original autoscaling group state has been recovered. Exception: %s", runtimeException.getMessage()), runtimeException);
        }
    }

    private void validateInstanceStatusesInScaledGroups(AuthenticatedContext ac, AmazonAutoScalingClient amazonASClient,
            AmazonEc2Client amazonEC2Client, AmazonCloudFormationClient cloudFormationClient, List<Group> scaledGroups) {
        for (Group group : scaledGroups) {
            List<software.amazon.awssdk.services.ec2.model.Instance> instancesForGroup =
                    awsMetadataCollector.collectInstancesForGroup(ac, amazonASClient, amazonEC2Client, cloudFormationClient, group.getName());
            for (software.amazon.awssdk.services.ec2.model.Instance i : instancesForGroup) {
                List<InstanceStateName> allowedInstanceStates = getAllowedInstanceStates(ac.getCloudCredential().getAccountId());
                LOGGER.info("Allowed instance states in the scaled group: {}", allowedInstanceStates);
                if (allowedInstanceStates.stream().noneMatch(state -> state.equals(i.state().name()))) {
                    throw new RuntimeException(String.format("Instance (%s) in the group (%s) is not running (%s).",
                            i.instanceId(), group.getName(), i.state().name()));
                }
            }
        }
    }

    private List<InstanceStateName> getAllowedInstanceStates(String accountId) {
        if (entitlementService.isUnboundEliminationSupported(accountId) && entitlementService.targetedUpscaleSupported(accountId)) {
            return List.of(InstanceStateName.RUNNING, InstanceStateName.STOPPED);
        } else {
            return List.of(InstanceStateName.RUNNING);
        }
    }

    private void sendASGUpdateFailedMessage(AmazonAutoScalingClient amazonASClient, Map<String, Group> desiredAutoscalingGroupsByName,
            AmazonAutoscalingFailedException amazonAutoscalingFailedException) {
        int desiredCount = desiredAutoscalingGroupsByName.values().stream().map(Group::getInstancesSize).reduce(Integer::sum).orElse(0);
        Map<String, Integer> asgSizeAfterFail = getAutoScalingGroupsBySize(desiredAutoscalingGroupsByName.keySet(), amazonASClient);
        int successCount = asgSizeAfterFail.values().stream().reduce(Integer::sum).orElse(0);
        LOGGER.info("AWS Autoscaling group update failed, original autoscaling group state has been recovered");
        throw new CloudConnectorException(String.format("Autoscaling group update failed: Amazon Autoscaling Group was not able to reach the desired state "
                        + "(%d instances instead of %d). Original autoscaling group state has been recovered. Failure reason: %s",
                successCount, desiredCount, amazonAutoscalingFailedException.getMessage()), amazonAutoscalingFailedException);
    }

    private void recoverOriginalState(AuthenticatedContext ac,
            CloudStack stack,
            AmazonAutoScalingClient amazonASClient,
            Map<String, Group> desiredAutoscalingGroupsByName,
            Map<String, Integer> originalAutoScalingGroupsBySize,
            Exception originalException) {
        LOGGER.info("Recover original state of the autoscaling group. Collecting info about desired and achieved instance counts", originalException);

        try {
            awsAutoScalingService.suspendAutoScaling(ac, stack);
            if (desiredAutoscalingGroupsByName.isEmpty()) {
                throw new CloudConnectorException("'Desired autoscaling groups' is empty, cannot decide what to recover!");
            } else {
                List<Instance> instancesFromASGs = getInstancesInAutoscalingGroups(amazonASClient, desiredAutoscalingGroupsByName.keySet());
                LOGGER.info("Instances from ASGs: {}", instancesFromASGs);
                List<String> knownInstanceIdsByCloudbreak = getKnownInstancesByCloudbreak(stack);
                LOGGER.info("Known instance ids by cloudbreak: {}", knownInstanceIdsByCloudbreak);
                List<String> unknownInstancesByCloudbreak = getUnknownInstancesFromASGs(instancesFromASGs, knownInstanceIdsByCloudbreak);
                LOGGER.info("Unknown instances by cloudbreak: {}", unknownInstancesByCloudbreak);
                if (!unknownInstancesByCloudbreak.isEmpty()) {
                    for (String unknownInstance : unknownInstancesByCloudbreak) {
                        LOGGER.info("Terminate unknown instance: {}", unknownInstance);
                        try {
                            awsAutoScalingService.terminateInstance(amazonASClient, unknownInstance);
                        } catch (AwsServiceException e) {
                            if (e.getMessage().contains("Instance Id not found")) {
                                LOGGER.info("{} was not found on AWS, it was terminated previously", unknownInstance, e);
                            } else {
                                throw e;
                            }
                        }
                    }
                }
                for (Entry<String, Group> desiredAutoscalingGroup : desiredAutoscalingGroupsByName.entrySet()) {
                    String autoscalingGroupName = desiredAutoscalingGroup.getKey();
                    Integer originalInstanceSizeForTheGroup = originalAutoScalingGroupsBySize.get(autoscalingGroupName);
                    LOGGER.info("Original instance size for the group: {}", originalInstanceSizeForTheGroup);
                    Integer desiredInstanceSize = desiredAutoscalingGroup.getValue().getInstancesSize();
                    if (originalInstanceSizeForTheGroup < desiredInstanceSize) {
                        LOGGER.info("Restore original instance size [{}] for autoscaling group: {}", originalInstanceSizeForTheGroup, autoscalingGroupName);
                        awsAutoScalingService.updateAutoscalingGroup(amazonASClient, autoscalingGroupName, originalInstanceSizeForTheGroup);
                    }
                }
            }
        } catch (RuntimeException recoverFailedException) {
            recoverFailedException.addSuppressed(originalException);
            LOGGER.info("Original autoscaling group state recover is failed", recoverFailedException);
            throw new CloudConnectorException("Upscale failed: " + originalException.getMessage() + ", We tried to recover to the original state, " +
                    "but recover failed also: '" + recoverFailedException.getMessage() + "' Please check your autoscaling groups on AWS.");
        }
    }

    private void updateAutoscalingGroups(AmazonAutoScalingClient amazonASClient,
            Map<String, Group> desiredAutoscalingGroupsByName, Map<String, Integer> originalAutoScalingGroupsBySize) {
        for (Entry<String, Group> groupEntry : desiredAutoscalingGroupsByName.entrySet()) {
            Integer newInstanceSize = groupEntry.getValue().getInstancesSize();
            if (originalAutoScalingGroupsBySize.get(groupEntry.getKey()) < newInstanceSize) {
                awsAutoScalingService.updateAutoscalingGroup(amazonASClient, groupEntry.getKey(), newInstanceSize);
            }
        }
    }

    private List<String> getUnknownInstancesFromASGs(List<Instance> instancesFromASGs, List<String> knownInstanceIdsByCloudbreak) {
        return instancesFromASGs.stream()
                .map(Instance::instanceId)
                .filter(id -> !knownInstanceIdsByCloudbreak.contains(id))
                .collect(Collectors.toList());
    }

    private List<String> getKnownInstancesByCloudbreak(CloudStack stack) {
        return stack.getGroups()
                .stream()
                .flatMap(group -> group.getInstances().stream())
                .map(CloudInstance::getInstanceId)
                .collect(Collectors.toList());
    }

    private List<Instance> getInstancesInAutoscalingGroups(AmazonAutoScalingClient amazonASClient, Set<String> autoscalingGroupNames) {
        List<AutoScalingGroup> autoscalingGroups = awsAutoScalingService.getAutoscalingGroups(amazonASClient, autoscalingGroupNames);
        return autoscalingGroups.stream()
                .flatMap(autoScalingGroup -> autoScalingGroup.instances().stream()).collect(Collectors.toList());
    }

    private Map<String, Integer> getAutoScalingGroupsBySize(Set<String> autoScalingGroupNames, AmazonAutoScalingClient amazonASClient) {
        DescribeAutoScalingGroupsRequest request = DescribeAutoScalingGroupsRequest.builder()
                .autoScalingGroupNames(new ArrayList<>(autoScalingGroupNames))
                .build();
        return amazonASClient.describeAutoScalingGroups(request).autoScalingGroups().stream()
                .collect(Collectors.toMap(AutoScalingGroup::autoScalingGroupName,
                        autoScalingGroup -> autoScalingGroup.instances().size()));
    }

    private Map<String, Group> getAutoScaleGroupsByNameFromCloudFormationTemplate(AuthenticatedContext ac, AmazonCloudFormationClient cloudFormationClient,
            List<Group> scaledGroups) {
        return scaledGroups.stream()
                .collect(Collectors.toMap(g -> cfStackUtil.getAutoscalingGroupName(ac, cloudFormationClient, g.getName()), g -> g));
    }

    private void associateElasticIpWithNewInstances(CloudStack stack, List<CloudResource> resources, AmazonCloudFormationClient cloudFormationClient,
            AmazonEc2Client amazonEC2Client, List<Group> scaledGroups, List<CloudResource> instances) {
        boolean mapPublicIpOnLaunch = awsNetworkService.isMapPublicOnLaunch(new AwsNetworkView(stack.getNetwork()), amazonEC2Client);
        List<Group> gateways = awsNetworkService.getGatewayGroups(scaledGroups);
        Map<String, List<String>> gatewayGroupInstanceMapping = createGatewayToNewInstancesMap(instances, gateways);
        if (mapPublicIpOnLaunch && !gateways.isEmpty()) {
            String cFStackName = cfStackUtil.getCloudFormationStackResource(resources).getName();
            Map<String, String> eipAllocationIds = awsElasticIpService.getElasticIpAllocationIds(cfStackUtil.getOutputs(cFStackName, cloudFormationClient),
                    cFStackName);
            for (Group gateway : gateways) {
                List<String> eips = awsElasticIpService.getEipsForGatewayGroup(eipAllocationIds, gateway);
                List<String> freeEips = awsElasticIpService.getFreeIps(eips, amazonEC2Client);
                List<String> newInstances = gatewayGroupInstanceMapping.get(gateway.getName());
                awsElasticIpService.associateElasticIpsToInstances(amazonEC2Client, freeEips, newInstances);
            }
        }
    }

    private Map<String, List<String>> createGatewayToNewInstancesMap(List<CloudResource> instances, List<Group> gateways) {
        return instances.stream()
                .filter(instance -> gateways.stream().anyMatch(gw -> gw.getName().equals(instance.getGroup())))
                .filter(instance -> {
                    Group gateway = gateways.stream().filter(gw -> gw.getName().equals(instance.getGroup())).findFirst().get();
                    return gateway.getInstances().stream().noneMatch(inst -> instance.getInstanceId().equals(inst.getInstanceId()));
                })
                .collect(Collectors.toMap(
                        CloudResource::getGroup,
                        instance -> List.of(instance.getInstanceId()),
                        (listOne, listTwo) -> Stream.concat(listOne.stream(), listTwo.stream()).collect(Collectors.toList())));
    }

    private List<CloudResource> getNewInstances(List<Group> scaledGroups, List<CloudResource> instances) {
        List<CloudResource> collectedInstances = instances.stream().filter(instance -> {
            Group group = scaledGroups.stream().filter(scaledGroup -> scaledGroup.getName().equals(instance.getGroup())).findFirst().get();
            return group.getInstances().stream().noneMatch(inst -> instance.getInstanceId().equals(inst.getInstanceId()));
        }).collect(Collectors.toList());
        LOGGER.debug("Collected instances: {}", collectedInstances);
        return collectedInstances;
    }

    private List<CloudResource> getReattachableVolumeSets(List<CloudResource> resources) {
        List<CloudResource> volumeSets = resources.stream()
                .filter(cloudResource -> ResourceType.AWS_VOLUMESET.equals(cloudResource.getType()))
                .filter(cloudResource -> CommonStatus.DETACHED.equals(cloudResource.getStatus()))
                .collect(Collectors.toList());
        LOGGER.debug("Collected detached volumesets for reattachment: {}", volumeSets);
        return volumeSets;
    }

    private List<Group> getGroupsWithNewInstances(List<Group> scaledGroups) {
        List<Group> collectedGroupsWithNewInstances = scaledGroups.stream().map(group -> {
            List<CloudInstance> newInstances = group.getInstances().stream()
                    .filter(instance -> Objects.isNull(instance.getInstanceId())).collect(Collectors.toList());

            return Group.builder()
                    .withName(group.getName())
                    .withType(group.getType())
                    .withInstances(newInstances)
                    .withSecurity(group.getSecurity())
                    .withParameters(group.getParameters())
                    .withInstanceAuthentication(group.getInstanceAuthentication())
                    .withLoginUserName(group.getLoginUserName())
                    .withPublicKey(group.getPublicKey())
                    .withRootVolumeSize(group.getRootVolumeSize())
                    .withIdentity(group.getIdentity())
                    .withNetwork(group.getNetwork())
                    .withTags(group.getTags())
                    .withRootVolumeType(group.getRootVolumeType())
                    .build();
        }).collect(Collectors.toList());
        LOGGER.debug("Collected groups with new instances: {}", collectedGroupsWithNewInstances);
        return collectedGroupsWithNewInstances;
    }
}
