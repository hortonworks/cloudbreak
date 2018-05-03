package com.sequenceiq.cloudbreak.cloud.aws;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.amazonaws.services.autoscaling.AmazonAutoScalingClient;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.sequenceiq.cloudbreak.cloud.MetadataCollector;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstanceMetaData;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceAuthentication;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;

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
        LOGGER.info("Collect AWS metadata, resources: {}, vms: {}, allInstances: {}", resources, vms, allInstances);
        List<CloudVmMetaDataStatus> collectedCloudVmMetaDataStatuses = new ArrayList<>();
        try {
            List<String> knownInstanceIdList = allInstances.stream()
                    .filter(cloudInstance -> cloudInstance.getInstanceId() != null)
                    .map(CloudInstance::getInstanceId)
                    .collect(Collectors.toList());

            collectCloudVmMetaDataStatuses(ac, vms, collectedCloudVmMetaDataStatuses, knownInstanceIdList);
            return collectedCloudVmMetaDataStatuses;
        } catch (RuntimeException e) {
            throw new CloudConnectorException(e.getMessage(), e);
        }
    }

    private void collectCloudVmMetaDataStatuses(AuthenticatedContext ac, List<CloudInstance> vms, List<CloudVmMetaDataStatus> collectedCloudVmMetaDataStatuses,
            List<String> knownInstanceIdList) {
        LOGGER.info("Collect Cloud VM metadata statuses, vms: {}, collectedCloudVmMetaDataStatuses: {}, knownInstanceIdList: {}",
                vms, collectedCloudVmMetaDataStatuses, knownInstanceIdList);
        String region = ac.getCloudContext().getLocation().getRegion().value();
        AmazonCloudFormationClient amazonCFClient = awsClient.createCloudFormationClient(new AwsCredentialView(ac.getCloudCredential()), region);
        AmazonAutoScalingClient amazonASClient = awsClient.createAutoScalingClient(new AwsCredentialView(ac.getCloudCredential()), region);
        AmazonEC2Client amazonEC2Client = awsClient.createAccess(new AwsCredentialView(ac.getCloudCredential()), region);

        Multimap<InstanceTemplate, CloudInstance> instanceTemplateMap = ArrayListMultimap.create();
        Map<InstanceTemplate, InstanceAuthentication> templateInstanceAuthenticationMap = new HashMap<>();
        fillInstanceTemplateMaps(vms, instanceTemplateMap, templateInstanceAuthenticationMap);

        for (InstanceTemplate instanceTemplate : instanceTemplateMap.keySet()) {
            List<CloudVmMetaDataStatus> cloudVmMetaDataStatusesForGroup = collectGroupMetaData(ac, amazonASClient, amazonEC2Client, amazonCFClient,
                    instanceTemplate, templateInstanceAuthenticationMap.get(instanceTemplate));
            List<CloudVmMetaDataStatus> unknownCloudMetadataStatuses = getUnkownCloudMetadataStatuses(knownInstanceIdList, cloudVmMetaDataStatusesForGroup);
            LOGGER.info("Unknown cloud metadata statuses: {}", unknownCloudMetadataStatuses);
            LOGGER.info("Cloud vm metadata statuses for group: {}", cloudVmMetaDataStatusesForGroup);
            for (CloudInstance vm : instanceTemplateMap.get(instanceTemplate)) {
                if (vm.getInstanceId() == null) {
                    addFromUnknownList(collectedCloudVmMetaDataStatuses, unknownCloudMetadataStatuses);
                } else {
                    addInstancesWithInstanceIdToGroup(collectedCloudVmMetaDataStatuses, cloudVmMetaDataStatusesForGroup, vm);
                }
            }
        }
    }

    private List<CloudVmMetaDataStatus> getUnkownCloudMetadataStatuses(List<String> knownInstanceIdList,
            List<CloudVmMetaDataStatus> cloudVmMetaDataStatusesForGroup) {
        LOGGER.info("Collect unknown cloud metadata statuses, knownInstanceIdList: {}, cloudVmMetaDataStatusesForGroup: {}",
                knownInstanceIdList, cloudVmMetaDataStatusesForGroup);
        return cloudVmMetaDataStatusesForGroup.stream().filter(cloudVmMetaDataStatus ->
                        !knownInstanceIdList.contains(cloudVmMetaDataStatus.getCloudVmInstanceStatus().getCloudInstance().getInstanceId()))
                        .collect(Collectors.toList());
    }

    private void addFromUnknownList(List<CloudVmMetaDataStatus> collectedCloudVmMetaDataStatuses, List<CloudVmMetaDataStatus> unknownCloudMetadataStatuses) {
        if (unknownCloudMetadataStatuses.size() > 0) {
            CloudVmMetaDataStatus newMetadataStatus = unknownCloudMetadataStatuses.remove(0);
            collectedCloudVmMetaDataStatuses.add(newMetadataStatus);
        }
    }

    private void addInstancesWithInstanceIdToGroup(List<CloudVmMetaDataStatus> collectedCloudVmMetaDataStatuses,
            List<CloudVmMetaDataStatus> cloudVmMetaDataStatusesForGroup, CloudInstance vm) {
        cloudVmMetaDataStatusesForGroup.stream()
                .filter(cloudVmMetaDataStatus ->
                        vm.getInstanceId().equals(cloudVmMetaDataStatus.getCloudVmInstanceStatus().getCloudInstance().getInstanceId()))
                .findAny()
                .ifPresent(collectedCloudVmMetaDataStatuses::add);
    }

    private void fillInstanceTemplateMaps(List<CloudInstance> vms, Multimap<InstanceTemplate, CloudInstance> instanceTemplateMap,
            Map<InstanceTemplate, InstanceAuthentication> templateInstanceAuthenticationMap) {
        for (CloudInstance vm : vms) {
            templateInstanceAuthenticationMap.put(vm.getTemplate(), vm.getAuthentication());
            instanceTemplateMap.put(vm.getTemplate(), vm);
        }
    }

    private List<CloudVmMetaDataStatus> collectGroupMetaData(AuthenticatedContext ac, AmazonAutoScalingClient amazonASClient,
            AmazonEC2Client amazonEC2Client, AmazonCloudFormationClient amazonCFClient, InstanceTemplate instanceTemplate,
            InstanceAuthentication instanceAuthentication) {

        LOGGER.info("Collect group metadata, instanceTemplate: {}, instanceAuthentication: {}", instanceTemplate, instanceAuthentication);

        String asGroupName = cloudFormationStackUtil.getAutoscalingGroupName(ac, amazonCFClient, instanceTemplate.getGroupName());
        List<String> instanceIds = cloudFormationStackUtil.getInstanceIds(amazonASClient, asGroupName);

        DescribeInstancesRequest instancesRequest = cloudFormationStackUtil.createDescribeInstancesRequest(instanceIds);
        DescribeInstancesResult instancesResult = amazonEC2Client.describeInstances(instancesRequest);

        return instancesResult.getReservations().stream()
                .flatMap(reservation -> reservation.getInstances().stream())
                .map(instance -> {
                    CloudInstanceMetaData md = new CloudInstanceMetaData(instance.getPrivateIpAddress(), instance.getPublicIpAddress());
                    CloudInstance cloudInstance = new CloudInstance(instance.getInstanceId(), instanceTemplate, instanceAuthentication);
                    CloudVmInstanceStatus cloudVmInstanceStatus = new CloudVmInstanceStatus(cloudInstance, InstanceStatus.CREATED);
                    return new CloudVmMetaDataStatus(cloudVmInstanceStatus, md);
                })
                .collect(Collectors.toList());
    }
}
