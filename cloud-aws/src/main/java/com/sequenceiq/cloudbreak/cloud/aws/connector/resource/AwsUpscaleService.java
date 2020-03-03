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

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsRequest;
import com.amazonaws.services.autoscaling.model.Instance;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.sequenceiq.cloudbreak.cloud.aws.AwsClient;
import com.sequenceiq.cloudbreak.cloud.aws.AwsTaggingService;
import com.sequenceiq.cloudbreak.cloud.aws.CloudFormationStackUtil;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonAutoScalingRetryClient;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonCloudFormationRetryClient;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsNetworkView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.cloudbreak.cloud.transform.CloudResourceHelper;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;

@Service
public class AwsUpscaleService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AwsUpscaleService.class);

    private static final List<String> UPSCALE_PROCESSES = singletonList("Launch");

    @Inject
    private AwsNetworkService awsNetworkService;

    @Inject
    private AwsClient awsClient;

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

    public List<CloudResourceStatus> upscale(AuthenticatedContext ac, CloudStack stack, List<CloudResource> resources) {
        AmazonCloudFormationRetryClient cloudFormationClient = getCloudFormationRetryClient(ac);
        AmazonAutoScalingRetryClient amazonASClient = getAutoScalingRetryClient(ac);
        AmazonEC2Client amazonEC2Client = getEC2Client(ac);

        List<Group> scaledGroups = cloudResourceHelper.getScaledGroups(stack);
        Map<String, Group> desiredAutoscalingGroupsByName = getAutoScaleGroupsByNameFromCloudFormationTemplate(ac, cloudFormationClient, scaledGroups);
        LOGGER.info("Desired autoscaling groups: {}", desiredAutoscalingGroupsByName);
        awsAutoScalingService.resumeAutoScaling(amazonASClient, desiredAutoscalingGroupsByName.keySet(), UPSCALE_PROCESSES);
        Map<String, Integer> originalAutoScalingGroupsBySize = getAutoScalingGroupsBySize(desiredAutoscalingGroupsByName.keySet(), amazonASClient);
        LOGGER.info("Update autoscaling groups for stack: {}", ac.getCloudContext().getName());
        Date timeBeforeASUpdate = new Date();
        updateAutoscalingGroups(amazonASClient, desiredAutoscalingGroupsByName, originalAutoScalingGroupsBySize);
        try {
            awsAutoScalingService.scheduleStatusChecks(scaledGroups, ac, cloudFormationClient, timeBeforeASUpdate);
        } catch (AmazonAutoscalingFailed amazonAutoscalingFailed) {
            return recoverOriginalState(ac, stack, amazonASClient, desiredAutoscalingGroupsByName, originalAutoScalingGroupsBySize,
                    amazonAutoscalingFailed);
        }
        awsAutoScalingService.suspendAutoScaling(ac, stack);
        List<CloudResource> instances = cfStackUtil.getInstanceCloudResources(ac, cloudFormationClient, amazonASClient, scaledGroups);
        associateElasticIpWithNewInstances(stack, resources, cloudFormationClient, amazonEC2Client, scaledGroups, instances);

        List<Group> groupsWithNewInstances = getGroupsWithNewInstances(scaledGroups);
        List<CloudResource> newInstances = getNewInstances(scaledGroups, instances);
        List<CloudResource> reattachableVolumeSets = getReattachableVolumeSets(scaledGroups, resources);
        List<CloudResource> networkResources = resources.stream()
                .filter(cloudResource -> ResourceType.AWS_SUBNET.equals(cloudResource.getType()))
                .collect(Collectors.toList());
        awsComputeResourceService.buildComputeResourcesForUpscale(ac, stack, groupsWithNewInstances, newInstances, reattachableVolumeSets, networkResources);

        awsTaggingService.tagRootVolumes(ac, amazonEC2Client, instances, stack.getTags());

        return singletonList(new CloudResourceStatus(cfStackUtil.getCloudFormationStackResource(resources), ResourceStatus.UPDATED));
    }

    private List<CloudResourceStatus> recoverOriginalState(AuthenticatedContext ac,
            CloudStack stack,
            AmazonAutoScalingRetryClient amazonASClient,
            Map<String, Group> desiredAutoscalingGroupsByName,
            Map<String, Integer> originalAutoScalingGroupsBySize,
            AmazonAutoscalingFailed amazonAutoscalingFailedException) {
        LOGGER.info("Amazon autoscaling group update failed, suspend autoscaling", amazonAutoscalingFailedException);
        try {
            awsAutoScalingService.suspendAutoScaling(ac, stack);
            List<Instance> instancesFromASGs = getInstancesInAutoscalingGroups(amazonASClient, desiredAutoscalingGroupsByName.keySet());
            LOGGER.info("Instances from ASGs: {}", instancesFromASGs);
            List<String> knownInstanceIdsByCloudbreak = getKnownInstancesByCloudbreak(stack);
            LOGGER.info("Known instance ids by cloudbreak: {}", knownInstanceIdsByCloudbreak);
            List<String> unknownInstancesByCloudbreak = getUnknownInstancesFromASGs(instancesFromASGs, knownInstanceIdsByCloudbreak);
            LOGGER.info("Unknown instances by cloudbreak: {}", unknownInstancesByCloudbreak);
            if (!unknownInstancesByCloudbreak.isEmpty()) {
                for (String unknownInstance : unknownInstancesByCloudbreak) {
                    LOGGER.info("Terminate unknown instance: {}", unknownInstance);
                    awsAutoScalingService.terminateInstance(amazonASClient, unknownInstance);
                }
            }
            Date timeBeforeASUpdate = new Date();
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
            awsAutoScalingService.scheduleStatusChecks(originalAutoScalingGroupsBySize, ac, timeBeforeASUpdate);
        } catch (Exception autoscalingFailed) {
            autoscalingFailed.addSuppressed(amazonAutoscalingFailedException);
            LOGGER.info("AWS Autoscaling group update failed, original autoscaling group state recover is failed", autoscalingFailed);
            throw new CloudConnectorException("AWS Autoscaling Group update failed: '" + amazonAutoscalingFailedException.getMessage() +
                    "'. We tried to recover to the original state, but it failed also: '" + autoscalingFailed.getMessage() +
                    "' Please check your autoscaling groups on AWS.", autoscalingFailed);
        }
        LOGGER.info("AWS Autoscaling group update failed, original autoscaling group state has been recovered");
        throw new CloudConnectorException("Autoscaling group update failed: '" + amazonAutoscalingFailedException.getMessage() +
                "' Original autoscaling group state has been recovered.", amazonAutoscalingFailedException);
    }

    private void updateAutoscalingGroups(AmazonAutoScalingRetryClient amazonASClient,
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
                        .filter(instance -> !knownInstanceIdsByCloudbreak.contains(instance.getInstanceId()))
                        .map(Instance::getInstanceId)
                        .collect(Collectors.toList());
    }

    private List<String> getKnownInstancesByCloudbreak(CloudStack stack) {
        return stack.getGroups()
                        .stream()
                        .flatMap(group -> group.getInstances().stream())
                        .map(CloudInstance::getInstanceId)
                        .collect(Collectors.toList());
    }

    private List<Instance> getInstancesInAutoscalingGroups(AmazonAutoScalingRetryClient amazonASClient, Set<String> autoscalingGroupNames) {
        List<AutoScalingGroup> autoscalingGroups = awsAutoScalingService.getAutoscalingGroups(amazonASClient, autoscalingGroupNames);
        return autoscalingGroups.stream()
                .flatMap(autoScalingGroup -> autoScalingGroup.getInstances().stream()).collect(Collectors.toList());
    }

    private Map<String, Integer> getAutoScalingGroupsBySize(Set<String> autoScalingGroupNames, AmazonAutoScalingRetryClient amazonASClient) {
        DescribeAutoScalingGroupsRequest request = new DescribeAutoScalingGroupsRequest();
        request.setAutoScalingGroupNames(new ArrayList<>(autoScalingGroupNames));
        return amazonASClient.describeAutoScalingGroups(request).getAutoScalingGroups().stream()
                .collect(Collectors.toMap(AutoScalingGroup::getAutoScalingGroupName,
                        autoScalingGroup -> autoScalingGroup.getInstances().size()));
    }

    private Map<String, Group> getAutoScaleGroupsByNameFromCloudFormationTemplate(AuthenticatedContext ac, AmazonCloudFormationRetryClient cloudFormationClient,
            List<Group> scaledGroups) {
        return scaledGroups.stream()
                .collect(Collectors.toMap(g -> cfStackUtil.getAutoscalingGroupName(ac, cloudFormationClient, g.getName()), g -> g));
    }

    private AmazonEC2Client getEC2Client(AuthenticatedContext ac) {
        return awsClient.createAccess(new AwsCredentialView(ac.getCloudCredential()),
                ac.getCloudContext().getLocation().getRegion().value());
    }

    private AmazonAutoScalingRetryClient getAutoScalingRetryClient(AuthenticatedContext ac) {
        return awsClient.createAutoScalingRetryClient(new AwsCredentialView(ac.getCloudCredential()),
                ac.getCloudContext().getLocation().getRegion().value());
    }

    private AmazonCloudFormationRetryClient getCloudFormationRetryClient(AuthenticatedContext ac) {
        return awsClient.createCloudFormationRetryClient(new AwsCredentialView(ac.getCloudCredential()),
                ac.getCloudContext().getLocation().getRegion().value());
    }

    private void associateElasticIpWithNewInstances(CloudStack stack, List<CloudResource> resources, AmazonCloudFormationRetryClient cloudFormationClient,
            AmazonEC2Client amazonEC2Client, List<Group> scaledGroups, List<CloudResource> instances) {
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
        return instances.stream().filter(instance -> {
            Group group = scaledGroups.stream().filter(scaledGroup -> scaledGroup.getName().equals(instance.getGroup())).findFirst().get();
            return group.getInstances().stream().noneMatch(inst -> instance.getInstanceId().equals(inst.getInstanceId()));
        }).collect(Collectors.toList());
    }

    private List<CloudResource> getReattachableVolumeSets(List<Group> scaledGroups, List<CloudResource> resources) {
        return resources.stream()
                .filter(cloudResource -> ResourceType.AWS_VOLUMESET.equals(cloudResource.getType()))
                .filter(cloudResource -> CommonStatus.DETACHED.equals(cloudResource.getStatus()))
                .collect(Collectors.toList());
    }

    private List<Group> getGroupsWithNewInstances(List<Group> scaledGroups) {
        return scaledGroups.stream().map(group -> {
            List<CloudInstance> newInstances = group.getInstances().stream()
                    .filter(instance -> Objects.isNull(instance.getInstanceId())).collect(Collectors.toList());

            return new Group(group.getName(), group.getType(), newInstances, group.getSecurity(), null, group.getParameters(),
                    group.getInstanceAuthentication(), group.getLoginUserName(), group.getPublicKey(), group.getRootVolumeSize(), group.getIdentity());
        }).collect(Collectors.toList());
    }

}
