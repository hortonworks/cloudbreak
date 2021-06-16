package com.sequenceiq.cloudbreak.cloud.aws;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.DescribeSubnetsRequest;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Subnet;
import com.amazonaws.services.elasticloadbalancingv2.model.LoadBalancer;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.sequenceiq.cloudbreak.cloud.MetadataCollector;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonAutoScalingClient;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonCloudFormationClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsPlatformResources;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.aws.common.loadbalancer.AwsLoadBalancer;
import com.sequenceiq.cloudbreak.cloud.aws.common.loadbalancer.LoadBalancerTypeConverter;
import com.sequenceiq.cloudbreak.cloud.aws.common.util.AwsLifeCycleMapper;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AuthenticatedContextView;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstanceMetaData;
import com.sequenceiq.cloudbreak.cloud.model.CloudLoadBalancerMetadata;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmTypes;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStoreMetadata;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.VmType;
import com.sequenceiq.cloudbreak.cloud.model.VolumeParameterConfig;
import com.sequenceiq.common.api.type.LoadBalancerType;

@Service
public class AwsMetadataCollector implements MetadataCollector {
    private static final Logger LOGGER = LoggerFactory.getLogger(AwsMetadataCollector.class);

    @Inject
    private AwsCloudFormationClient awsClient;

    @Inject
    private CloudFormationStackUtil cloudFormationStackUtil;

    @Inject
    private AwsLifeCycleMapper awsLifeCycleMapper;

    @Inject
    private LoadBalancerTypeConverter loadBalancerTypeConverter;

    @Inject
    private AwsPlatformResources awsPlatformResources;

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

        Set<String> subnetIds = new HashSet<>();
        Multimap<String, Instance> instancesOnAWSForGroup = ArrayListMultimap.create();
        for (String group : instanceGroupMap.keySet()) {
            List<Instance> instancesForGroup = collectInstancesForGroup(ac, amazonASClient, amazonEC2Client, amazonCFClient, group);
            instancesOnAWSForGroup.putAll(group, instancesForGroup);
            subnetIds.addAll(getSubnetIdsForInstances(instancesForGroup));
        }
        Map<String, String> subnetIdToAvailabilityZoneMap = buildSubnetIdToAvailabilityZoneMap(subnetIds, amazonEC2Client);

        Multimap<String, Instance> unknownInstancesForGroup = getUnknownInstancesForGroup(knownInstanceIdList, instancesOnAWSForGroup);
        for (CloudInstance vm : vms) {
            if (vm.getInstanceId() == null) {
                addFromUnknownMap(vm, unknownInstancesForGroup, collectedCloudVmMetaDataStatuses, subnetIdToAvailabilityZoneMap);
            } else {
                addKnownInstance(vm, instancesOnAWSForGroup, collectedCloudVmMetaDataStatuses, subnetIdToAvailabilityZoneMap);
            }
        }
        return collectedCloudVmMetaDataStatuses;
    }

    private Collection<String> getSubnetIdsForInstances(List<Instance> instances) {
        return instances.stream()
                .map(Instance::getSubnetId)
                .collect(Collectors.toSet());
    }

    private Map<String, String> buildSubnetIdToAvailabilityZoneMap(Set<String> subnetIds, AmazonEc2Client amazonEC2Client) {
        List<Subnet> subnets = amazonEC2Client.describeSubnets(new DescribeSubnetsRequest().withSubnetIds(subnetIds)).getSubnets();
        return subnets.stream()
                .collect(Collectors.toMap(Subnet::getSubnetId, Subnet::getAvailabilityZone));
    }

    private Multimap<String, Instance> getUnknownInstancesForGroup(List<String> knownInstanceIdList, Multimap<String, Instance> instancesOnAWSForGroup) {
        LOGGER.debug("Collect unknown cloud metadata statuses, knownInstanceIdList: {}, instancesOnAWSForGroup: {}", knownInstanceIdList,
                instancesOnAWSForGroup.keySet());
        Multimap<String, Instance> unknownInstancesForGroup = ArrayListMultimap.create();
        for (String group : instancesOnAWSForGroup.keySet()) {
            Collection<Instance> instances = instancesOnAWSForGroup.get(group);
            List<Instance> unknownInstances = instances.stream().filter(instance ->
                    !knownInstanceIdList.contains(instance.getInstanceId()))
                    .collect(Collectors.toList());
            unknownInstancesForGroup.putAll(group, unknownInstances);
        }
        return unknownInstancesForGroup;
    }

    private void addFromUnknownMap(CloudInstance cloudInstance, Multimap<String, Instance> unknownMap,
            List<CloudVmMetaDataStatus> collectedCloudVmMetaDataStatuses, Map<String, String> subnetIdToAvailabilityZoneMap) {
        LOGGER.debug("Collect from unknown map, cloudInstance: {}, unknownMap: {}", cloudInstance.getInstanceId(), unknownMap.keySet());
        String groupName = cloudInstance.getTemplate().getGroupName();
        Collection<Instance> unknownInstancesForGroup = unknownMap.get(groupName);
        if (!unknownInstancesForGroup.isEmpty()) {
            Optional<Instance> found = unknownInstancesForGroup.stream().findFirst();
            Instance foundInstance = found.get();
            CloudInstance newCloudInstance = new CloudInstance(foundInstance.getInstanceId(), cloudInstance.getTemplate(),
                    cloudInstance.getAuthentication(), cloudInstance.getParameters());
            addCloudInstanceNetworkParameters(newCloudInstance, foundInstance, subnetIdToAvailabilityZoneMap);
            CloudInstanceMetaData cloudInstanceMetaData = new CloudInstanceMetaData(
                    foundInstance.getPrivateIpAddress(),
                    foundInstance.getPublicIpAddress(),
                    awsLifeCycleMapper.getLifeCycle(foundInstance));
            CloudVmInstanceStatus cloudVmInstanceStatus = new CloudVmInstanceStatus(newCloudInstance, InstanceStatus.CREATED);
            CloudVmMetaDataStatus newMetadataStatus = new CloudVmMetaDataStatus(cloudVmInstanceStatus, cloudInstanceMetaData);
            collectedCloudVmMetaDataStatuses.add(newMetadataStatus);
            unknownMap.remove(groupName, foundInstance);
        }
    }

    private void addCloudInstanceNetworkParameters(CloudInstance cloudInstance, Instance instance, Map<String, String> subnetIdToAvailabilityZoneMap) {
        String subnetId = instance.getSubnetId();
        cloudInstance.putParameter(CloudInstance.SUBNET_ID, subnetId);
        cloudInstance.putParameter(CloudInstance.AVAILABILITY_ZONE, subnetIdToAvailabilityZoneMap.get(subnetId));
    }

    private void addKnownInstance(CloudInstance cloudInstance, Multimap<String, Instance> instancesOnAWSForGroup,
            List<CloudVmMetaDataStatus> collectedCloudVmMetaDataStatuses, Map<String, String> subnetIdToAvailabilityZoneMap) {
        LOGGER.debug("Add known instance, cloudInstance: {}, instancesOnAWSForGroup: {}", cloudInstance.getInstanceId(), instancesOnAWSForGroup.keySet());
        List<Instance> instanceList = instancesOnAWSForGroup.entries().stream().map(Entry::getValue).collect(Collectors.toList());
        instanceList.stream()
                .filter(instance ->
                        cloudInstance.getInstanceId().equals(instance.getInstanceId()))
                .findAny()
                .ifPresent(instance -> {
                    addCloudInstanceNetworkParameters(cloudInstance, instance, subnetIdToAvailabilityZoneMap);
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

    public List<Instance> collectInstancesForGroup(AuthenticatedContext ac, AmazonAutoScalingClient amazonASClient,
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

    @Override
    public InstanceStoreMetadata collectInstanceStorageCount(AuthenticatedContext ac, List<String> instanceTypes) {
        Location location = ac.getCloudContext().getLocation();
        try {
            CloudVmTypes cloudVmTypes = awsPlatformResources.virtualMachines(ac.getCloudCredential(), location.getRegion(), Map.of());
            Map<String, Set<VmType>> cloudVmResponses = cloudVmTypes.getCloudVmResponses();
            Map<String, VolumeParameterConfig> instanceTypeToInstanceStorageMap = cloudVmResponses.getOrDefault(location.getAvailabilityZone().value(), Set.of())
                    .stream()
                    .filter(vmType -> instanceTypes.contains(vmType.value()))
                    .filter(vmType -> Objects.nonNull(vmType.getMetaData().getEphemeralConfig()))
                    .collect(Collectors.toMap(VmType::value, vmType -> vmType.getMetaData().getEphemeralConfig()));
            return new InstanceStoreMetadata(instanceTypeToInstanceStorageMap);
        } catch (Exception e) {
        LOGGER.warn("Failed to get vm type data: {}", instanceTypes, e);
        throw new CloudConnectorException(e.getMessage(), e);
    }
    }
}
