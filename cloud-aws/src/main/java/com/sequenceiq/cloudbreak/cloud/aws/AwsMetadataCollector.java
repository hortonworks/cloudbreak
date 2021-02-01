package com.sequenceiq.cloudbreak.cloud.aws;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.elasticloadbalancingv2.model.LoadBalancer;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.sequenceiq.cloudbreak.cloud.MetadataCollector;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonAutoScalingClient;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonCloudFormationClient;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.aws.loadbalancer.AwsLoadBalancer;
import com.sequenceiq.cloudbreak.cloud.aws.loadbalancer.converter.LoadBalancerTypeConverter;
import com.sequenceiq.cloudbreak.cloud.aws.util.AwsLifeCycleMapper;
import com.sequenceiq.cloudbreak.cloud.aws.view.AuthenticatedContextView;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstanceMetaData;
import com.sequenceiq.cloudbreak.cloud.model.CloudLoadBalancerMetadata;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.common.api.type.LoadBalancerType;

@Service
public class AwsMetadataCollector implements MetadataCollector {
    private static final Logger LOGGER = LoggerFactory.getLogger(AwsMetadataCollector.class);

    @Inject
    private AwsClient awsClient;

    @Inject
    private CloudFormationStackUtil cloudFormationStackUtil;

    @Inject
    private AwsLifeCycleMapper awsLifeCycleMapper;

    @Inject
    private LoadBalancerTypeConverter loadBalancerTypeConverter;

    @Override
    public List<CloudVmMetaDataStatus> collect(AuthenticatedContext ac, List<CloudResource> resources, List<CloudInstance> vms,
            List<CloudInstance> allInstances) {
        LOGGER.debug("Collect AWS instance metadata, for cluster {} resources: {}, vms: {}, allInstances: {}",
                ac.getCloudContext().getName(), resources.size(), vms.size(), allInstances.size());
        try {
            List<String> knownInstanceIdList = allInstances.stream()
                    .filter(cloudInstance -> cloudInstance.getInstanceId() != null)
                    .map(CloudInstance::getInstanceId)
                    .collect(Collectors.toList());

            return collectCloudVmMetaDataStatuses(ac, vms, knownInstanceIdList);
        } catch (RuntimeException e) {
            throw new CloudConnectorException(e.getMessage(), e);
        }
    }

    private List<CloudVmMetaDataStatus> collectCloudVmMetaDataStatuses(AuthenticatedContext ac, List<CloudInstance> vms,
            List<String> knownInstanceIdList) {
        LOGGER.debug("Collect Cloud VM metadata statuses");

        List<CloudVmMetaDataStatus> collectedCloudVmMetaDataStatuses = new ArrayList<>();

        String region = ac.getCloudContext().getLocation().getRegion().value();
        AmazonCloudFormationClient amazonCFClient = awsClient.createCloudFormationClient(new AwsCredentialView(ac.getCloudCredential()), region);
        AmazonAutoScalingClient amazonASClient = awsClient.createAutoScalingClient(new AwsCredentialView(ac.getCloudCredential()), region);
        AmazonEc2Client amazonEC2Client = new AuthenticatedContextView(ac).getAmazonEC2Client();

        Multimap<String, CloudInstance> instanceGroupMap = getInstanceGroupMap(vms);

        Multimap<String, Instance> instancesOnAWSForGroup = ArrayListMultimap.create();
        for (String group : instanceGroupMap.keySet()) {
            List<Instance> instancesForGroup = collectInstancesForGroup(ac, amazonASClient, amazonEC2Client, amazonCFClient, group);
            instancesOnAWSForGroup.putAll(group, instancesForGroup);
        }

        Multimap<String, Instance> unknownInstancesForGroup = getUnkownInstancesForGroup(knownInstanceIdList, instancesOnAWSForGroup);
        for (CloudInstance vm : vms) {
            if (vm.getInstanceId() == null) {
                addFromUnknownMap(vm, unknownInstancesForGroup, collectedCloudVmMetaDataStatuses);
            } else {
                addKnownInstance(vm, instancesOnAWSForGroup, collectedCloudVmMetaDataStatuses);
            }
        }
        return collectedCloudVmMetaDataStatuses;
    }

    private Multimap<String, Instance> getUnkownInstancesForGroup(List<String> knownInstanceIdList, Multimap<String, Instance> instancesOnAWSForGroup) {
        LOGGER.debug("Collect unknown cloud metadata statuses, knownInstanceIdList: {}, instancesOnAWSForGroup: {}", knownInstanceIdList,
                instancesOnAWSForGroup.keySet());
        Multimap<String, Instance> unknownInstancesForGroup = ArrayListMultimap.create();
        for (String group : instancesOnAWSForGroup.keySet()) {
            Collection<Instance> instances = instancesOnAWSForGroup.get(group);
            List<Instance> unkownInstances = instances.stream().filter(instance ->
                    !knownInstanceIdList.contains(instance.getInstanceId()))
                    .collect(Collectors.toList());
            unknownInstancesForGroup.putAll(group, unkownInstances);
        }
        return unknownInstancesForGroup;
    }

    private void addFromUnknownMap(CloudInstance cloudInstance, Multimap<String, Instance> unknownMap,
            List<CloudVmMetaDataStatus> collectedCloudVmMetaDataStatuses) {
        LOGGER.debug("Collect from unknown map, cloudInstance: {}, unknownMap: {}", cloudInstance.getInstanceId(), unknownMap.keySet());
        String groupName = cloudInstance.getTemplate().getGroupName();
        Collection<Instance> unknownInstancesForGroup = unknownMap.get(groupName);
        if (!unknownInstancesForGroup.isEmpty()) {
            Optional<Instance> found = unknownInstancesForGroup.stream().findFirst();
            Instance foundInstance = found.get();
            CloudInstance newCloudInstance = new CloudInstance(foundInstance.getInstanceId(), cloudInstance.getTemplate(),
                    cloudInstance.getAuthentication(), cloudInstance.getParameters());
            CloudInstanceMetaData cloudInstanceMetaData = new CloudInstanceMetaData(
                    foundInstance.getPrivateIpAddress(),
                    foundInstance.getPublicIpAddress(),
                    awsLifeCycleMapper.getLifeCycle(foundInstance));
            CloudVmInstanceStatus cloudVmInstanceStatus = new CloudVmInstanceStatus(newCloudInstance, InstanceStatus.CREATED);
            CloudVmMetaDataStatus newMetadataStatus = new CloudVmMetaDataStatus(cloudVmInstanceStatus, cloudInstanceMetaData);
            collectedCloudVmMetaDataStatuses.add(newMetadataStatus);
            unknownMap.remove(groupName, found.get());
        }
    }

    private void addKnownInstance(CloudInstance cloudInstance, Multimap<String, Instance> instancesOnAWSForGroup,
            List<CloudVmMetaDataStatus> collectedCloudVmMetaDataStatuses) {
        LOGGER.debug("Add known instance, cloudInstance: {}, instancesOnAWSForGroup: {}", cloudInstance.getInstanceId(), instancesOnAWSForGroup.keySet());
        List<Instance> instanceList = instancesOnAWSForGroup.entries().stream().map(Entry::getValue).collect(Collectors.toList());
        instanceList.stream()
                .filter(instance ->
                        cloudInstance.getInstanceId().equals(instance.getInstanceId()))
                .findAny()
                .ifPresent(instance -> {
                    CloudInstanceMetaData cloudInstanceMetaData = new CloudInstanceMetaData(
                            instance.getPrivateIpAddress(),
                            instance.getPublicIpAddress(),
                            awsLifeCycleMapper.getLifeCycle(instance));
                    CloudVmInstanceStatus cloudVmInstanceStatus = new CloudVmInstanceStatus(cloudInstance, cloudInstance.getTemplate().getStatus());
                    CloudVmMetaDataStatus newMetadataStatus = new CloudVmMetaDataStatus(cloudVmInstanceStatus, cloudInstanceMetaData);
                    collectedCloudVmMetaDataStatuses.add(newMetadataStatus);
                });
    }

    private Multimap<String, CloudInstance> getInstanceGroupMap(List<CloudInstance> vms) {
        Multimap<String, CloudInstance> instanceGroupMap = ArrayListMultimap.create();
        for (CloudInstance vm : vms) {
            instanceGroupMap.put(vm.getTemplate().getGroupName(), vm);
        }
        return instanceGroupMap;
    }

    private List<Instance> collectInstancesForGroup(AuthenticatedContext ac, AmazonAutoScalingClient amazonASClient,
            AmazonEc2Client amazonEC2Client, AmazonCloudFormationClient amazonCFClient, String group) {

        LOGGER.debug("Collect aws instances for group: {}", group);

        String asGroupName = cloudFormationStackUtil.getAutoscalingGroupName(ac, amazonCFClient, group);
        List<String> instanceIds = cloudFormationStackUtil.getInstanceIds(amazonASClient, asGroupName);

        DescribeInstancesRequest instancesRequest = cloudFormationStackUtil.createDescribeInstancesRequest(instanceIds);
        DescribeInstancesResult instancesResult = amazonEC2Client.describeInstances(instancesRequest);

        return instancesResult.getReservations().stream()
                .flatMap(reservation -> reservation.getInstances().stream())
                .collect(Collectors.toList());
    }

    @Override
    public List<CloudLoadBalancerMetadata> collectLoadBalancer(AuthenticatedContext ac, List<LoadBalancerType> loadBalancerTypes,
            List<CloudResource> resources) {
        LOGGER.debug("Collect AWS load balancer metadata, for cluster {}", ac.getCloudContext().getName());

        List<CloudLoadBalancerMetadata> cloudLoadBalancerMetadata = new ArrayList<>();
        for (LoadBalancerType type : loadBalancerTypes) {
            String loadBalancerName = AwsLoadBalancer.getLoadBalancerName(loadBalancerTypeConverter.convert(type));
            LOGGER.debug("Attempting to collect metadata for load balancer {}, type {}", loadBalancerName, type);
            try {
                LoadBalancer loadBalancer = cloudFormationStackUtil.getLoadBalancerByLogicalId(ac, loadBalancerName);
                CloudLoadBalancerMetadata loadBalancerMetadata = new CloudLoadBalancerMetadata.Builder()
                    .withType(type)
                    .withCloudDns(loadBalancer.getDNSName())
                    .withHostedZoneId(loadBalancer.getCanonicalHostedZoneId())
                    .withName(loadBalancerName)
                    .build();
                cloudLoadBalancerMetadata.add(loadBalancerMetadata);
                LOGGER.debug("Saved metadata for load balancer {}: DNS {}, zone id {}", loadBalancerName, loadBalancer.getDNSName(),
                    loadBalancer.getCanonicalHostedZoneId());
            } catch (RuntimeException e) {
                LOGGER.debug("Unable to find metadata for load balancer " + loadBalancerName, e);
            }
        }
        return cloudLoadBalancerMetadata;
    }
}
