package com.sequenceiq.cloudbreak.cloud.aws;

import static com.sequenceiq.cloudbreak.cloud.model.CloudInstance.FQDN;
import static com.sequenceiq.cloudbreak.common.network.NetworkConstants.SUBNET_ID;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.MetadataCollector;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonAutoScalingClient;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonCloudFormationClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsPlatformResources;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.aws.common.connector.resource.AwsInstanceCommonService;
import com.sequenceiq.cloudbreak.cloud.aws.common.loadbalancer.AwsLoadBalancer;
import com.sequenceiq.cloudbreak.cloud.aws.common.loadbalancer.AwsLoadBalancerScheme;
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
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceCheckMetadata;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStoreMetadata;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTypeMetadata;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.common.api.type.LoadBalancerType;
import com.sequenceiq.common.api.type.ResourceType;

import software.amazon.awssdk.services.ec2.model.DescribeInstancesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesResponse;
import software.amazon.awssdk.services.ec2.model.DescribeSubnetsRequest;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.Subnet;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.LoadBalancer;

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

    @Inject
    private AwsLoadBalancerMetadataCollector awsLoadBalancerMetadataCollector;

    @Inject
    private EntitlementService entitlementService;

    @Inject
    private AwsInstanceCommonService awsInstanceCommonService;

    @Override
    public List<CloudVmMetaDataStatus> collect(AuthenticatedContext ac, List<CloudResource> resources, List<CloudInstance> vms,
            List<CloudInstance> allInstances) {
        LOGGER.debug("Collect AWS instance metadata, for cluster {} resources: {}, vms: {}, allInstances: {}",
                ac.getCloudContext().getName(), resources.size(), vms.size(), allInstances.size());
        try {
            List<String> knownInstanceIdList = allInstances.stream()
                    .map(CloudInstance::getInstanceId)
                    .filter(Objects::nonNull)
                    .collect(toList());

            return collectCloudVmMetaDataStatuses(ac, vms, resources, knownInstanceIdList);
        } catch (RuntimeException e) {
            throw new CloudConnectorException(e.getMessage(), e);
        }
    }

    private List<CloudVmMetaDataStatus> collectCloudVmMetaDataStatuses(AuthenticatedContext ac, List<CloudInstance> vms,
            List<CloudResource> resources, List<String> knownInstanceIdList) {
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
            LOGGER.info("Collected instances for group of {}: {}", group, instancesForGroup.stream().map(Instance::instanceId).collect(joining(",")));
            instancesOnAWSForGroup.putAll(group, instancesForGroup);
            subnetIds.addAll(getSubnetIdsForInstances(instancesForGroup));
        }
        LOGGER.info("Collected subnet IDs: {}", subnetIds);
        Map<String, String> subnetIdToAvailabilityZoneMap = buildSubnetIdToAvailabilityZoneMap(subnetIds, amazonEC2Client);
        LOGGER.info("Subnet id to availability zone map: {}", subnetIdToAvailabilityZoneMap);
        Multimap<String, Instance> unknownInstancesForGroup = getUnknownInstancesForGroup(knownInstanceIdList, instancesOnAWSForGroup);
        for (CloudInstance vm : vms) {
            if (vm.getInstanceId() == null) {
                addFromUnknownMap(vm, resources, unknownInstancesForGroup, collectedCloudVmMetaDataStatuses, subnetIdToAvailabilityZoneMap);
            } else {
                addKnownInstance(vm, instancesOnAWSForGroup, collectedCloudVmMetaDataStatuses, subnetIdToAvailabilityZoneMap);
            }
        }
        LOGGER.info("Collected cloud VM metadata and statuses: {}", collectedCloudVmMetaDataStatuses);
        return collectedCloudVmMetaDataStatuses;
    }

    private Collection<String> getSubnetIdsForInstances(List<Instance> instances) {
        return instances.stream()
                .map(Instance::subnetId)
                .collect(toSet());
    }

    private Map<String, String> buildSubnetIdToAvailabilityZoneMap(Set<String> subnetIds, AmazonEc2Client amazonEC2Client) {
        List<Subnet> subnets = amazonEC2Client.describeSubnets(DescribeSubnetsRequest.builder().subnetIds(subnetIds).build()).subnets();
        return subnets.stream()
                .collect(toMap(Subnet::subnetId, Subnet::availabilityZone));
    }

    private Multimap<String, Instance> getUnknownInstancesForGroup(List<String> knownInstanceIdList, Multimap<String, Instance> instancesOnAWSForGroup) {
        LOGGER.debug("Collect unknown cloud metadata statuses, knownInstanceIdList: {}, instancesOnAWSForGroup: {}", knownInstanceIdList,
                instancesOnAWSForGroup.keySet());
        Multimap<String, Instance> unknownInstancesForGroup = ArrayListMultimap.create();
        for (String group : instancesOnAWSForGroup.keySet()) {
            Collection<Instance> instances = instancesOnAWSForGroup.get(group);
            List<Instance> unknownInstances = instances.stream().filter(instance ->
                            !knownInstanceIdList.contains(instance.instanceId()))
                    .collect(toList());
            unknownInstancesForGroup.putAll(group, unknownInstances);
        }
        LOGGER.info("Collected unknown instances from AWS: {}",
                unknownInstancesForGroup.values().stream().map(Instance::instanceId).collect(joining(",")));
        return unknownInstancesForGroup;
    }

    private void addFromUnknownMap(CloudInstance cloudInstance, List<CloudResource> resources, Multimap<String, Instance> unknownMap,
            List<CloudVmMetaDataStatus> collectedCloudVmMetaDataStatuses, Map<String, String> subnetIdToAvailabilityZoneMap) {
        String groupName = cloudInstance.getTemplate().getGroupName();
        LOGGER.info("CloudInstance group: {}", groupName);
        Collection<Instance> unknownInstancesForGroup = unknownMap.get(groupName);
        if (!unknownInstancesForGroup.isEmpty()) {
            LOGGER.info("Unknown instances from AWS for group {}: {}", groupName,
                    unknownInstancesForGroup.stream().map(Instance::instanceId).collect(joining(",")));
            Instance selectedInstance = findInstanceByFQDNIfFQDNDefinedInCloudInstance(cloudInstance, groupName, resources, unknownInstancesForGroup);
            CloudInstance newCloudInstance = new CloudInstance(selectedInstance.instanceId(),
                    cloudInstance.getTemplate(),
                    cloudInstance.getAuthentication(),
                    cloudInstance.getSubnetId(),
                    cloudInstance.getAvailabilityZone(),
                    cloudInstance.getParameters());
            addCloudInstanceNetworkParameters(newCloudInstance, selectedInstance, subnetIdToAvailabilityZoneMap);
            CloudInstanceMetaData cloudInstanceMetaData = new CloudInstanceMetaData(
                    selectedInstance.privateIpAddress(),
                    selectedInstance.publicIpAddress(),
                    awsLifeCycleMapper.getLifeCycle(selectedInstance));
            LOGGER.info("New CloudInstance: {}", newCloudInstance);
            LOGGER.info("Cloud instance metadata: {}", cloudInstanceMetaData);
            CloudVmInstanceStatus cloudVmInstanceStatus = new CloudVmInstanceStatus(newCloudInstance, InstanceStatus.CREATED);
            CloudVmMetaDataStatus newMetadataStatus = new CloudVmMetaDataStatus(cloudVmInstanceStatus, cloudInstanceMetaData);
            collectedCloudVmMetaDataStatuses.add(newMetadataStatus);
            unknownMap.remove(groupName, selectedInstance);
        }
    }

    private Instance findInstanceByFQDNIfFQDNDefinedInCloudInstance(CloudInstance cloudInstance, String groupName, List<CloudResource> resources,
            Collection<Instance> unknownInstancesForGroup) {
        List<CloudResource> volumeResources =
                resources.stream().filter(cloudResource -> ResourceType.AWS_VOLUMESET.equals(cloudResource.getType())).collect(toList());
        List<String> allKnownVolumes = listVolumes(volumeResources);
        LOGGER.info("All known volumes: {}", allKnownVolumes);
        String instanceFQDN = cloudInstance.getStringParameter(FQDN);
        Long privateId = cloudInstance.getTemplate().getPrivateId();
        List<String> volumeCandidates;
        if (instanceFQDN == null) {
            LOGGER.info("Instance (private id: {}) does not have FQDN parameter, lets find an instance from AWS which has an attached volume, but the volume " +
                    "does not have FQDN", privateId);
            volumeCandidates = listVolumesWithoutFQDN(volumeResources);
        } else {
            LOGGER.info("Instance (private id: {}) has FQDN ({}), lets find an instance from AWS which has attached volume with the same FQDN",
                    privateId, instanceFQDN);
            List<String> volumesForFqdn = listVolumesForFQDN(volumeResources, instanceFQDN);
            LOGGER.info("Volumes for FQDN ({}), {}", instanceFQDN, volumesForFqdn);
            if (!volumesForFqdn.isEmpty()) {
                LOGGER.info("We found volume with the given FQDN, so lets find the instance with this volume");
                volumeCandidates = volumesForFqdn;
            } else {
                LOGGER.info("We can't found any volume with the given FQDN, this means disk was deleted, we can chose any machine with FQDN less volumes");
                volumeCandidates = listVolumesWithoutFQDN(volumeResources);
            }
        }
        return findInstanceByVolumes(unknownInstancesForGroup, volumeCandidates)
                .or(() -> getInstancesWithoutKnownVolumes(unknownInstancesForGroup, allKnownVolumes))
                .orElseThrow(() -> {
                    if (instanceFQDN == null) {
                        return new IllegalStateException(String.format(
                                "Error occured while tried to identify new instance in %s group from AWS. New instances on AWS are %s.",
                                groupName,
                                unknownInstancesForGroup.stream().map(Instance::instanceId).collect(joining(", "))));
                    } else {
                        return new IllegalStateException(String.format(
                                "Error occured while tried to identify instance from AWS for %s host. "
                                        + "Couldn't find the instance based on existing volumes or couldn't pick a new one based on unknown volumes. "
                                        + "New instances on AWS are %s.",
                                instanceFQDN,
                                unknownInstancesForGroup.stream().map(Instance::instanceId).collect(joining(", "))
                        ));
                    }
                });
    }

    private Optional<Instance> findInstanceByVolumes(Collection<Instance> unknownInstancesForGroup, List<String> volumes) {
        return unknownInstancesForGroup.stream().filter(instance -> instance.blockDeviceMappings().stream()
                        .anyMatch(instanceBlockDeviceMapping -> volumes.contains(instanceBlockDeviceMapping.ebs().volumeId())))
                .findFirst();
    }

    private Optional<Instance> getInstancesWithoutKnownVolumes(Collection<Instance> unknownInstancesForGroup, List<String> allKnownVolumes) {
        return unknownInstancesForGroup.stream().filter(instance -> instance.blockDeviceMappings().stream()
                        .noneMatch(instanceBlockDeviceMapping -> allKnownVolumes.contains(instanceBlockDeviceMapping.ebs().volumeId())))
                .findFirst();
    }

    private List<String> listVolumes(List<CloudResource> volumeResources) {
        return volumeResources.stream().map(cloudResource -> cloudResource.getParameter(CloudResource.ATTRIBUTES, VolumeSetAttributes.class))
                .map(VolumeSetAttributes::getVolumes)
                .flatMap(Collection::stream)
                .map(VolumeSetAttributes.Volume::getId)
                .collect(toList());
    }

    private List<String> listVolumesWithoutFQDN(List<CloudResource> volumeResources) {
        List<String> volumesWithoutFQDN = volumeResources.stream()
                .map(cloudResource -> cloudResource.getParameter(CloudResource.ATTRIBUTES, VolumeSetAttributes.class))
                .filter(volumeSetAttributes -> volumeSetAttributes.getDiscoveryFQDN() == null)
                .map(VolumeSetAttributes::getVolumes)
                .flatMap(Collection::stream)
                .map(VolumeSetAttributes.Volume::getId)
                .collect(toList());
        LOGGER.info("Volumes without FQDN: {}", volumesWithoutFQDN);
        return volumesWithoutFQDN;
    }

    private List<String> listVolumesForFQDN(List<CloudResource> volumeResources, String fqdn) {
        return volumeResources.stream().map(cloudResource -> cloudResource.getParameter(CloudResource.ATTRIBUTES, VolumeSetAttributes.class))
                .filter(volumeSetAttributes -> volumeSetAttributes.getDiscoveryFQDN() != null)
                .filter(volumeSetAttributes -> volumeSetAttributes.getDiscoveryFQDN().equals(fqdn))
                .map(VolumeSetAttributes::getVolumes)
                .flatMap(Collection::stream)
                .map(VolumeSetAttributes.Volume::getId)
                .collect(toList());
    }

    private void addCloudInstanceNetworkParameters(CloudInstance cloudInstance, Instance instance, Map<String, String> subnetIdToAvailabilityZoneMap) {
        String subnetId = instance.subnetId();
        cloudInstance.putParameter(SUBNET_ID, subnetId);
        cloudInstance.setSubnetId(subnetId);
        String availabilityZone = subnetIdToAvailabilityZoneMap.get(subnetId);
        cloudInstance.setAvailabilityZone(availabilityZone);
    }

    private void addKnownInstance(CloudInstance cloudInstance, Multimap<String, Instance> instancesOnAWSForGroup,
            List<CloudVmMetaDataStatus> collectedCloudVmMetaDataStatuses, Map<String, String> subnetIdToAvailabilityZoneMap) {
        LOGGER.debug("Add known instance, cloudInstance: {}, instancesOnAWSForGroup: {}", cloudInstance.getInstanceId(), instancesOnAWSForGroup.keySet());
        List<Instance> instanceList = instancesOnAWSForGroup.entries().stream().map(Entry::getValue).collect(toList());
        instanceList.stream()
                .filter(instance ->
                        cloudInstance.getInstanceId().equals(instance.instanceId()))
                .findAny()
                .ifPresent(instance -> {
                    addCloudInstanceNetworkParameters(cloudInstance, instance, subnetIdToAvailabilityZoneMap);
                    CloudInstanceMetaData cloudInstanceMetaData = new CloudInstanceMetaData(
                            instance.privateIpAddress(),
                            instance.publicIpAddress(),
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
        List<Instance> describedInstances = new ArrayList<>();
        LOGGER.debug("Collect aws instances for group: {}", group);

        String asGroupName = cloudFormationStackUtil.getAutoscalingGroupName(ac, amazonCFClient, group);
        List<String> instanceIds = cloudFormationStackUtil.getInstanceIds(amazonASClient, asGroupName);

        if (instanceIds.isEmpty()) {
            LOGGER.warn("No instanceID was collected for group '{}', within auto scaling group: '{}', but metadata collection has been triggered on it", group,
                    asGroupName);
        } else {
            LOGGER.debug("Collected instanceID(s) for group ({}): {}", String.join(",", instanceIds), group);
            DescribeInstancesRequest instancesRequest = cloudFormationStackUtil.createDescribeInstancesRequest(instanceIds);
            DescribeInstancesResponse instancesResponse = amazonEC2Client.retryableDescribeInstances(instancesRequest);

            describedInstances.addAll(instancesResponse.reservations().stream()
                    .flatMap(reservation -> reservation.instances().stream())
                    .collect(toList()));
        }
        return describedInstances;
    }

    @Override
    public List<CloudLoadBalancerMetadata> collectLoadBalancer(AuthenticatedContext ac, List<LoadBalancerType> loadBalancerTypes,
            List<CloudResource> resources) {
        LOGGER.debug("Collect AWS load balancer metadata, for cluster {}", ac.getCloudContext().getName());

        List<CloudLoadBalancerMetadata> cloudLoadBalancerMetadata = new ArrayList<>();
        for (LoadBalancerType type : loadBalancerTypes) {
            AwsLoadBalancerScheme scheme = loadBalancerTypeConverter.convert(type);
            String loadBalancerName = AwsLoadBalancer.getLoadBalancerName(scheme);
            LOGGER.debug("Attempting to collect metadata for load balancer {}, type {}", loadBalancerName, type);
            try {
                LoadBalancer loadBalancer = cloudFormationStackUtil.getLoadBalancerByLogicalId(ac, loadBalancerName);
                LOGGER.debug("Parsing all listener and target group information for load balancer {}", loadBalancerName);
                Map<String, Object> parameters = awsLoadBalancerMetadataCollector.getParameters(ac, loadBalancer, scheme);

                CloudLoadBalancerMetadata loadBalancerMetadata = CloudLoadBalancerMetadata.builder()
                        .withType(type)
                        .withCloudDns(loadBalancer.dnsName())
                        .withHostedZoneId(loadBalancer.canonicalHostedZoneId())
                        .withName(loadBalancerName)
                        .withParameters(parameters)
                        .build();
                cloudLoadBalancerMetadata.add(loadBalancerMetadata);
                LOGGER.debug("Saved metadata for load balancer {}: DNS {}, zone id {}", loadBalancerName, loadBalancer.dnsName(),
                        loadBalancer.canonicalHostedZoneId());
            } catch (RuntimeException e) {
                LOGGER.debug("Unable to find metadata for load balancer " + loadBalancerName, e);
            }
        }
        return cloudLoadBalancerMetadata;
    }

    @Override
    public InstanceStoreMetadata collectInstanceStorageCount(AuthenticatedContext ac, List<String> instanceTypes) {
        return awsPlatformResources.collectInstanceStorageCount(ac, instanceTypes,
                entitlementService.getEntitlements(ac.getCloudCredential().getAccountId()));
    }

    @Override
    public InstanceTypeMetadata collectInstanceTypes(AuthenticatedContext ac, List<String> instanceIds) {
        return awsInstanceCommonService.collectInstanceTypes(ac, instanceIds);
    }

    @Override
    public List<InstanceCheckMetadata> collectCdpInstances(AuthenticatedContext ac, String resourceCrn, CloudStack cloudStack, List<String> knownInstanceIds) {
        return awsInstanceCommonService.collectCdpInstances(ac, resourceCrn, knownInstanceIds);
    }
}
