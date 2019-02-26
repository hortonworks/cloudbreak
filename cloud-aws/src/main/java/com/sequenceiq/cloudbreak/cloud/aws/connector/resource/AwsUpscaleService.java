package com.sequenceiq.cloudbreak.cloud.aws.connector.resource;

import static java.util.Collections.singletonList;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.amazonaws.services.ec2.AmazonEC2Client;
import com.sequenceiq.cloudbreak.cloud.aws.AwsClient;
import com.sequenceiq.cloudbreak.cloud.aws.CloudFormationStackUtil;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonAutoScalingRetryClient;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonCloudFormationRetryClient;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsNetworkView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.cloudbreak.cloud.transform.CloudResourceHelper;
import com.sequenceiq.cloudbreak.common.type.ResourceType;

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

    public List<CloudResourceStatus> upscale(AuthenticatedContext ac, CloudStack stack, List<CloudResource> resources) {
        AmazonCloudFormationRetryClient cloudFormationClient = awsClient.createCloudFormationRetryClient(new AwsCredentialView(ac.getCloudCredential()),
                ac.getCloudContext().getLocation().getRegion().value());
        AmazonAutoScalingRetryClient amazonASClient = awsClient.createAutoScalingRetryClient(new AwsCredentialView(ac.getCloudCredential()),
                ac.getCloudContext().getLocation().getRegion().value());
        AmazonEC2Client amazonEC2Client = awsClient.createAccess(new AwsCredentialView(ac.getCloudCredential()),
                ac.getCloudContext().getLocation().getRegion().value());

        List<Group> scaledGroups = cloudResourceHelper.getScaledGroups(stack);

        Map<String, Group> groupMap = scaledGroups.stream().collect(
                Collectors.toMap(g -> cfStackUtil.getAutoscalingGroupName(ac, cloudFormationClient, g.getName()), g -> g));
        awsAutoScalingService.resumeAutoScaling(amazonASClient, groupMap.keySet(), UPSCALE_PROCESSES);
        for (Entry<String, Group> groupEntry : groupMap.entrySet()) {
            awsAutoScalingService.updateAutoscalingGroup(amazonASClient, groupEntry.getKey(), groupEntry.getValue(), ac.getCloudContext().getId());
        }
        awsAutoScalingService.scheduleStatusChecks(stack, ac, cloudFormationClient);
        awsAutoScalingService.suspendAutoScaling(ac, stack);

        List<CloudResource> instances = cfStackUtil.getInstanceCloudResources(ac, cloudFormationClient, amazonASClient, scaledGroups);
        associateElasticIpWithNewInstances(stack, resources, cloudFormationClient, amazonEC2Client, scaledGroups, instances);

        List<Group> groupsWithNewInstances = getGroupsWithNewInstances(scaledGroups);
        List<CloudResource> newInstances = getNewInstances(scaledGroups, instances);
        List<CloudResource> reattachableVolumeSets = getReattachableVolumeSets(scaledGroups, resources);
        List<CloudResource> networkResources = resources.stream()
                .filter(cloudResource -> ResourceType.AWS_SUBNET.equals(cloudResource.getType())).collect(Collectors.toList());
        awsComputeResourceService.buildComputeResourcesForUpscale(ac, stack, groupsWithNewInstances, newInstances, reattachableVolumeSets, networkResources);

        return singletonList(new CloudResourceStatus(cfStackUtil.getCloudFormationStackResource(resources), ResourceStatus.UPDATED));
    }

    private void associateElasticIpWithNewInstances(CloudStack stack, List<CloudResource> resources, AmazonCloudFormationRetryClient cloudFormationClient,
            AmazonEC2Client amazonEC2Client, List<Group> scaledGroups, List<CloudResource> instances) {
        boolean mapPublicIpOnLaunch = awsNetworkService.isMapPublicOnLaunch(new AwsNetworkView(stack.getNetwork()), amazonEC2Client);
        List<Group> gateways = awsNetworkService.getGatewayGroups(scaledGroups);
        Map<String, List<String>> gatewayGroupInstanceMapping = createGatewayToNewInstancesMap(instances, gateways);
        if (mapPublicIpOnLaunch && !gateways.isEmpty()) {
            String cFStackName = cfStackUtil.getCloudFormationStackResource(resources).getName();
            Map<String, String> eipAllocationIds =
                    awsElasticIpService.getElasticIpAllocationIds(cfStackUtil.getOutputs(cFStackName, cloudFormationClient), cFStackName);
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
                .filter(cloudResource -> Objects.isNull(cloudResource.getInstanceId()))
                .collect(Collectors.toList());
    }

    private List<Group> getGroupsWithNewInstances(List<Group> scaledGroups) {
        return scaledGroups.stream().map(group -> {
            List<CloudInstance> newInstances = group.getInstances().stream()
                    .filter(instance -> Objects.isNull(instance.getInstanceId())).collect(Collectors.toList());

            return new Group(group.getName(), group.getType(), newInstances, group.getSecurity(), null, group.getParameters(),
                    group.getInstanceAuthentication(), group.getLoginUserName(), group.getPublicKey(), group.getRootVolumeSize());
        }).collect(Collectors.toList());
    }

}
