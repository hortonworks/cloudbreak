package com.sequenceiq.cloudbreak.cloud.aws.metadata;

import static com.sequenceiq.cloudbreak.cloud.aws.common.AwsSdkErrorCodes.INSTANCE_NOT_FOUND;
import static com.sequenceiq.cloudbreak.cloud.aws.common.AwsSdkErrorCodes.LOAD_BALANCER_NOT_FOUND;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.MetadataCollector;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsPlatformResources;
import com.sequenceiq.cloudbreak.cloud.aws.common.CommonAwsClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonElasticLoadBalancingClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.connector.resource.AwsInstanceCommonService;
import com.sequenceiq.cloudbreak.cloud.aws.common.loadbalancer.LoadBalancerTypeConverter;
import com.sequenceiq.cloudbreak.cloud.aws.common.util.AwsLifeCycleMapper;
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
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.common.api.type.LoadBalancerType;
import com.sequenceiq.common.api.type.LoadBalancerTypeAttribute;
import com.sequenceiq.common.api.type.ResourceType;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesResponse;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.DescribeLoadBalancersRequest;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.DescribeLoadBalancersResponse;

@Service
public class AwsNativeMetadataCollector implements MetadataCollector {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsNativeMetadataCollector.class);

    @Inject
    private AwsLifeCycleMapper awsLifeCycleMapper;

    @Inject
    private LoadBalancerTypeConverter loadBalancerTypeConverter;

    @Inject
    private CommonAwsClient awsClient;

    @Inject
    private AwsPlatformResources awsPlatformResources;

    @Inject
    private AwsNativeLbMetadataCollector awsNativeLbMetadataCollector;

    @Inject
    private EntitlementService entitlementService;

    @Inject
    private AwsInstanceCommonService awsInstanceCommonService;

    @Inject
    private AwsNativeLoadBalancerIpCollector awsNativeLoadBalancerIpCollector;

    @Value("${cb.aws.native.instance.fetch.max.item:100}")
    private int instanceFetchMaxBatchSize;

    @Override
    public List<CloudVmMetaDataStatus> collect(AuthenticatedContext ac, List<CloudResource> resources, List<CloudInstance> vms,
            List<CloudInstance> allInstances) {
        LOGGER.debug("Collect AWS instance metadata, for cluster {} resources: {}, VMs: {}, allInstances: {}",
                ac.getCloudContext().getName(), resources.size(), vms.size(), allInstances.size());
        try {
            String region = ac.getCloudContext().getLocation().getRegion().value();
            AmazonEc2Client ec2Client = awsClient.createEc2Client(new AwsCredentialView(ac.getCloudCredential()), region);
            return collectInstances(vms, resources, ec2Client);
        } catch (RuntimeException e) {
            LOGGER.warn("Collection of instance metadata failed", e);
            throw new CloudConnectorException(e.getMessage(), e);
        }
    }

    @Override
    public List<CloudLoadBalancerMetadata> collectLoadBalancer(AuthenticatedContext ac, List<LoadBalancerType> lbTypes, List<CloudResource> resources) {
        List<CloudLoadBalancerMetadata> response = new ArrayList<>();
        String region = ac.getCloudContext().getLocation().getRegion().value();
        AwsCredentialView awsCredential = new AwsCredentialView(ac.getCloudCredential());
        LOGGER.debug("Collect AWS load balancer metadata, for cluster {}", ac.getCloudContext().getName());

        Set<CloudResource> loadBalancers = resources.stream()
                .filter(resource -> ResourceType.ELASTIC_LOAD_BALANCER.equals(resource.getType()))
                .collect(Collectors.toSet());
        Set<String> loadBalancerArns = loadBalancers.stream()
                .map(CloudResource::getReference)
                .collect(Collectors.toSet());
        AmazonElasticLoadBalancingClient loadBalancingClient = awsClient.createElasticLoadBalancingClient(awsCredential, region);
        AmazonEc2Client ec2Client = awsClient.createEc2Client(awsCredential, region);
        LOGGER.info("Collect AWS load balancer metadata, in region '{}' with ARNs: '{}'", region, String.join(", ", loadBalancerArns));

        for (CloudResource loadBalancer : loadBalancers) {
            Optional<CloudLoadBalancerMetadata> collectedLoadBalancer = collectLoadBalancerMetadata(loadBalancingClient, ec2Client, loadBalancer, resources,
                    ac.getCloudContext().getCrn());
            collectedLoadBalancer.ifPresent(response::add);
        }
        return response;
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

    private List<CloudVmMetaDataStatus> collectInstances(List<CloudInstance> vms, List<CloudResource> resources, AmazonEc2Client ec2Client) {
        List<CloudVmMetaDataStatus> response = new ArrayList<>();
        Set<String> preferredInstanceIds = resources.stream()
                .filter(resource -> ResourceType.AWS_INSTANCE.equals(resource.getType()))
                .filter(resource -> vms.stream().anyMatch(vm -> String.valueOf(vm.getTemplate().getPrivateId()).equals(resource.getReference())))
                .map(CloudResource::getInstanceId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<String, CloudResource> resourcesByInstanceId = resources.stream()
                .filter(resource -> ResourceType.AWS_INSTANCE.equals(resource.getType()))
                .filter(cloudResource -> cloudResource.getInstanceId() != null)
                .collect(Collectors.toMap(CloudResource::getInstanceId, Function.identity()));
        AtomicInteger counter = new AtomicInteger(0);
        Map<Integer, List<String>> instanceIdBatches = preferredInstanceIds.stream()
                .collect(Collectors.groupingBy(s -> counter.getAndIncrement() / instanceFetchMaxBatchSize, LinkedHashMap::new, Collectors.toList()));

        for (Map.Entry<Integer, List<String>> instanceIdBatchEntry : instanceIdBatches.entrySet()) {
            List<String> instanceIdBatch = instanceIdBatchEntry.getValue();
            LOGGER.info("Collecting metadata for instances iteration {}/{}", instanceIdBatchEntry.getKey() + 1, instanceIdBatches.size());
            try {
                response.addAll(describeInstances(vms, instanceIdBatch, resourcesByInstanceId, ec2Client));
            } catch (AwsServiceException serviceException) {
                if (StringUtils.isNotEmpty(serviceException.awsErrorDetails().errorCode()) &&
                        INSTANCE_NOT_FOUND.equals(serviceException.awsErrorDetails().errorCode())) {
                    response.addAll(handleNotFoundInstancesAndDescribeOthers(vms, ec2Client, instanceIdBatch, serviceException, resourcesByInstanceId));
                } else {
                    LOGGER.warn("Collection of instance metadata failed", serviceException);
                    throw serviceException;
                }
            }
        }
        return response;
    }

    private CloudResource findCloudResourceByInstanceId(Map<String, CloudResource> resourcesByInstanceId, String instanceId) {
        CloudResource connectedResource = resourcesByInstanceId.get(instanceId);
        if (connectedResource == null) {
            throw new NotFoundException(String.format("Instance with ID '%s' could not be found as resource on our side", instanceId));
        }
        return connectedResource;
    }

    private CloudInstance findCloudInstanceByPrivateId(List<CloudInstance> vms, CloudResource connectedResource) {
        return vms.stream()
                .filter(ci -> connectedResource.getReference().equals(String.valueOf(ci.getTemplate().getPrivateId())))
                .findFirst()
                .orElseThrow();
    }

    private List<CloudVmMetaDataStatus> handleNotFoundInstancesAndDescribeOthers(List<CloudInstance> vms, AmazonEc2Client ec2Client,
            List<String> instanceIdList, AwsServiceException serviceException, Map<String, CloudResource> resourcesByInstanceId) {
        LOGGER.info("One or more instances could not be found, collecting metadata for existing ones");
        int instanceIdNum = instanceIdList.size();
        List<CloudVmMetaDataStatus> response = new ArrayList<>(instanceIdNum);
        List<String> existingInstanceIds = new ArrayList<>(instanceIdNum);
        instanceIdList
                .forEach(instanceId -> {
                    if (!serviceException.getMessage().contains(instanceId)) {
                        existingInstanceIds.add(instanceId);
                    } else {
                        LOGGER.info("Marking instance '{}' as terminated because it could not be found on the provider side", instanceId);
                        CloudResource connectedResource = findCloudResourceByInstanceId(resourcesByInstanceId, instanceId);
                        CloudInstance matchedInstance = findCloudInstanceByPrivateId(vms, connectedResource);
                        CloudInstance updatedInstance = new CloudInstance(connectedResource.getReference(), matchedInstance.getTemplate(),
                                matchedInstance.getAuthentication(), matchedInstance.getSubnetId(), matchedInstance.getAvailabilityZone());
                        CloudVmInstanceStatus status = new CloudVmInstanceStatus(updatedInstance, InstanceStatus.TERMINATED);
                        response.add(new CloudVmMetaDataStatus(status, CloudInstanceMetaData.EMPTY_METADATA));
                    }
                });
        if (!existingInstanceIds.isEmpty()) {
            LOGGER.info("Existing instances on AWS to collect metadata for: {}/{}", existingInstanceIds.size(), instanceIdNum);
            response.addAll(describeInstances(vms, existingInstanceIds, resourcesByInstanceId, ec2Client));
        }
        return response;
    }

    private List<CloudVmMetaDataStatus> describeInstances(List<CloudInstance> vms, List<String> instanceIdList,
            Map<String, CloudResource> resourcesByInstanceId, AmazonEc2Client ec2Client) {
        LOGGER.info("Collecting metadata for instance IDs: '{}'", String.join(", ", instanceIdList));
        List<CloudVmMetaDataStatus> response = new ArrayList<>();
        DescribeInstancesRequest instancesRequest = DescribeInstancesRequest.builder().instanceIds(instanceIdList).build();
        DescribeInstancesResponse instancesResponse = ec2Client.describeInstances(instancesRequest);

        List<Instance> instances = instancesResponse.reservations().stream()
                .flatMap(reservation -> reservation.instances().stream())
                .collect(Collectors.toList());

        for (Instance instance : instances) {
            String instanceId = instance.instanceId();
            CloudResource connectedResource = findCloudResourceByInstanceId(resourcesByInstanceId, instanceId);
            CloudInstance matchedInstance = findCloudInstanceByPrivateId(vms, connectedResource);
            CloudInstance updatedInstance = new CloudInstance(connectedResource.getInstanceId(), matchedInstance.getTemplate(),
                    matchedInstance.getAuthentication(), matchedInstance.getSubnetId(), matchedInstance.getAvailabilityZone());
            CloudInstanceMetaData cloudInstanceMetaData = new CloudInstanceMetaData(
                    instance.privateIpAddress(),
                    instance.publicIpAddress(),
                    awsLifeCycleMapper.getLifeCycle(instance));
            CloudVmInstanceStatus cloudVmInstanceStatus = new CloudVmInstanceStatus(updatedInstance, updatedInstance.getTemplate().getStatus());
            CloudVmMetaDataStatus newMetadataStatus = new CloudVmMetaDataStatus(cloudVmInstanceStatus, cloudInstanceMetaData);
            response.add(newMetadataStatus);
        }
        return response;
    }

    private Optional<CloudLoadBalancerMetadata> collectLoadBalancerMetadata(AmazonElasticLoadBalancingClient loadBalancingClient,  AmazonEc2Client ec2Client,
            CloudResource loadBalancer, List<CloudResource> resources, String resourceCrn) {
        Optional<CloudLoadBalancerMetadata> response = Optional.empty();
        try {
            response = describeLoadBalancer(loadBalancer, loadBalancingClient, ec2Client, resources, resourceCrn);
        } catch (AwsServiceException awsException) {
            if (StringUtils.isNotEmpty(awsException.awsErrorDetails().errorCode()) &&
                    LOAD_BALANCER_NOT_FOUND.equals(awsException.awsErrorDetails().errorCode())) {
                LOGGER.info("Load balancers with ARN '{}' could not be found due to:", loadBalancer.getReference(), awsException);
            } else {
                LOGGER.warn("Metadata collection failed for load balancer '{}'", loadBalancer.getReference(), awsException);
                throw new CloudConnectorException("Metadata collection of load balancers failed", awsException);
            }
        } catch (RuntimeException e) {
            LOGGER.warn("Unable to fetch metadata for load balancer '{}'", loadBalancer.getReference(), e);
            throw new CloudConnectorException(String.format("Metadata collection of load balancers failed. Reason: %s", e.getMessage()), e);
        }
        return response;
    }

    private Optional<CloudLoadBalancerMetadata> describeLoadBalancer(CloudResource loadBalancer, AmazonElasticLoadBalancingClient loadBalancingClient,
            AmazonEc2Client ec2Client, List<CloudResource> resources, String resourceCrn) {
        DescribeLoadBalancersRequest describeLoadBalancersRequest = DescribeLoadBalancersRequest.builder().loadBalancerArns(loadBalancer.getReference()).build();
        DescribeLoadBalancersResponse describeLoadBalancersResponse = loadBalancingClient.describeLoadBalancers(describeLoadBalancersRequest);

        return describeLoadBalancersResponse.loadBalancers().stream()
                .findFirst()
                .map(awsLb -> {
                    LoadBalancerType type = loadBalancerTypeConverter.convert(awsLb.scheme());
                    Map<String, Object> parameters = awsNativeLbMetadataCollector.getParameters(loadBalancer.getReference(), resources);
                    LoadBalancerTypeAttribute resourceLbType = loadBalancer.getParameter(CloudResource.ATTRIBUTES, LoadBalancerTypeAttribute.class);
                    if (type == LoadBalancerType.PRIVATE && resourceLbType == LoadBalancerTypeAttribute.GATEWAY_PRIVATE) {
                        LOGGER.debug("GATEWAY_PRIVATE LoadBalancer selected");
                        type = LoadBalancerType.GATEWAY_PRIVATE;
                    }
                    CloudLoadBalancerMetadata.Builder loadBalancerMetadata = CloudLoadBalancerMetadata.builder()
                            .withType(type)
                            .withCloudDns(awsLb.dnsName())
                            .withHostedZoneId(awsLb.canonicalHostedZoneId())
                            .withName(awsLb.loadBalancerName())
                            .withParameters(parameters);
                    awsNativeLoadBalancerIpCollector.getLoadBalancerIp(ec2Client, loadBalancer.getName(), resourceCrn).ifPresent(loadBalancerMetadata::withIp);
                    LOGGER.info("Saved metadata for load balancer {}: DNS {}, zone ID {}", awsLb.loadBalancerName(), awsLb.dnsName(),
                            awsLb.canonicalHostedZoneId());
                    return loadBalancerMetadata.build();
                });
    }

    @Override
    public List<InstanceCheckMetadata> collectCdpInstances(AuthenticatedContext ac, String resourceCrn, CloudStack cloudStack, List<String> knownInstanceIds) {
        return awsInstanceCommonService.collectCdpInstances(ac, resourceCrn, knownInstanceIds);
    }
}
