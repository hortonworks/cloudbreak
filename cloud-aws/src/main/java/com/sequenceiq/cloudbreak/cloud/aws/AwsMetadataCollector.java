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

import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.sequenceiq.cloudbreak.cloud.MetadataCollector;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonAutoScalingRetryClient;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonCloudFormationRetryClient;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstanceMetaData;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;

@Service
public class AwsMetadataCollector implements MetadataCollector {
    private static final Logger LOGGER = LoggerFactory.getLogger(AwsMetadataCollector.class);

    @Inject
    private AwsClient awsClient;

    @Inject
    private CloudFormationStackUtil cloudFormationStackUtil;

    @Override
    public List<CloudVmMetaDataStatus> collect(AuthenticatedContext ac, List<CloudResource> resources, List<CloudInstance> vms,
            List<CloudInstance> allInstances) {
        LOGGER.debug("Collect AWS metadata, resources: {}, vms: {}, allInstances: {}", resources, vms, allInstances);
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
        LOGGER.debug("Collect Cloud VM metadata statuses, vms: {}, knownInstanceIdList: {}", vms, knownInstanceIdList);

        List<CloudVmMetaDataStatus> collectedCloudVmMetaDataStatuses = new ArrayList<>();

        String region = ac.getCloudContext().getLocation().getRegion().value();
        AmazonCloudFormationRetryClient amazonCFClient = awsClient.createCloudFormationRetryClient(new AwsCredentialView(ac.getCloudCredential()), region);
        AmazonAutoScalingRetryClient amazonASClient = awsClient.createAutoScalingRetryClient(new AwsCredentialView(ac.getCloudCredential()), region);
        AmazonEC2Client amazonEC2Client = awsClient.createAccess(new AwsCredentialView(ac.getCloudCredential()), region);

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
        LOGGER.debug("Collect unknown cloud metadata statuses, knownInstanceIdList: {}, instancesOnAWSForGroup: {}",
                knownInstanceIdList, instancesOnAWSForGroup);
        Multimap<String, Instance> unkownInstancesForGroup = ArrayListMultimap.create();
        for (String group : instancesOnAWSForGroup.keySet()) {
            Collection<Instance> instances = instancesOnAWSForGroup.get(group);
            List<Instance> unkownInstances = instances.stream().filter(instance ->
                    !knownInstanceIdList.contains(instance.getInstanceId()))
                    .collect(Collectors.toList());
            unkownInstancesForGroup.putAll(group, unkownInstances);
        }
        return unkownInstancesForGroup;
    }

    private void addFromUnknownMap(CloudInstance cloudInstance, Multimap<String, Instance> unknownMap,
            List<CloudVmMetaDataStatus> collectedCloudVmMetaDataStatuses) {
        LOGGER.debug("Collect from unknown map, cloudInstance: {}, unknownMap: {}, collectedCloudVmMetaDataStatuses: {}",
                cloudInstance, unknownMap, collectedCloudVmMetaDataStatuses);
        String groupName = cloudInstance.getTemplate().getGroupName();
        Collection<Instance> unknownInstancesForGroup = unknownMap.get(groupName);
        if (!unknownInstancesForGroup.isEmpty()) {
            Optional<Instance> found = unknownInstancesForGroup.stream().findFirst();
            Instance foundInstance = found.get();
            CloudInstance newCloudInstance = new CloudInstance(foundInstance.getInstanceId(), cloudInstance.getTemplate(),
                    cloudInstance.getAuthentication(), cloudInstance.getParameters());
            CloudInstanceMetaData cloudInstanceMetaData =
                    new CloudInstanceMetaData(foundInstance.getPrivateIpAddress(), foundInstance.getPublicIpAddress());
            CloudVmInstanceStatus cloudVmInstanceStatus = new CloudVmInstanceStatus(newCloudInstance, InstanceStatus.CREATED);
            CloudVmMetaDataStatus newMetadataStatus = new CloudVmMetaDataStatus(cloudVmInstanceStatus, cloudInstanceMetaData);
            collectedCloudVmMetaDataStatuses.add(newMetadataStatus);
            unknownMap.remove(groupName, found.get());
        }
    }

    private void addKnownInstance(CloudInstance cloudInstance, Multimap<String, Instance> instancesOnAWSForGroup,
            List<CloudVmMetaDataStatus> collectedCloudVmMetaDataStatuses) {
        LOGGER.debug("Add known intance, cloudInstance: {}, instancesOnAWSForGroup: {}, collectedCloudVmMetaDataStatuses: {}",
                cloudInstance, instancesOnAWSForGroup, collectedCloudVmMetaDataStatuses);
        List<Instance> instanceList = instancesOnAWSForGroup.entries().stream().map(Entry::getValue).collect(Collectors.toList());
        instanceList.stream()
                .filter(instance ->
                        cloudInstance.getInstanceId().equals(instance.getInstanceId()))
                .findAny()
                .ifPresent(instance -> {
                    CloudInstanceMetaData cloudInstanceMetaData =
                            new CloudInstanceMetaData(instance.getPrivateIpAddress(), instance.getPublicIpAddress());
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

    private List<Instance> collectInstancesForGroup(AuthenticatedContext ac, AmazonAutoScalingRetryClient amazonASClient,
            AmazonEC2Client amazonEC2Client, AmazonCloudFormationRetryClient amazonCFClient, String group) {

        LOGGER.debug("Collect aws instances for group: {}", group);

        String asGroupName = cloudFormationStackUtil.getAutoscalingGroupName(ac, amazonCFClient, group);
        List<String> instanceIds = cloudFormationStackUtil.getInstanceIds(amazonASClient, asGroupName);

        DescribeInstancesRequest instancesRequest = cloudFormationStackUtil.createDescribeInstancesRequest(instanceIds);
        DescribeInstancesResult instancesResult = amazonEC2Client.describeInstances(instancesRequest);

        return instancesResult.getReservations().stream()
                .flatMap(reservation -> reservation.getInstances().stream())
                .collect(Collectors.toList());
    }
}
