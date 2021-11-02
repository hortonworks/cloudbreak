package com.sequenceiq.cloudbreak.cloud.aws.metadata;

import static com.sequenceiq.cloudbreak.cloud.aws.common.AwsInstanceConnector.INSTANCE_NOT_FOUND_ERROR_CODE;

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

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.elasticloadbalancingv2.model.DescribeLoadBalancersRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.DescribeLoadBalancersResult;
import com.sequenceiq.cloudbreak.cloud.MetadataCollector;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsPlatformResources;
import com.sequenceiq.cloudbreak.cloud.aws.common.CommonAwsClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonElasticLoadBalancingClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.loadbalancer.LoadBalancerTypeConverter;
import com.sequenceiq.cloudbreak.cloud.aws.common.util.AwsLifeCycleMapper;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstanceMetaData;
import com.sequenceiq.cloudbreak.cloud.model.CloudLoadBalancerMetadata;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStoreMetadata;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.common.api.type.LoadBalancerType;
import com.sequenceiq.common.api.type.ResourceType;

@Service
public class AwsNativeMetadataCollector implements MetadataCollector {

    public static final String LOAD_BALANCER_NOT_FOUND_ERROR_CODE = "LoadBalancerNotFound";

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
        List<CloudLoadBalancerMetadata> result = new ArrayList<>();
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
        LOGGER.info("Collect AWS load balancer metadata, in region '{}' with ARNs: '{}'", region, String.join(", ", loadBalancerArns));

        for (String loadBalancerArn : loadBalancerArns) {
            Optional<CloudLoadBalancerMetadata> collectedLoadBalancer = collectLoadBalancerMetadata(loadBalancingClient, loadBalancerArn, resources);
            collectedLoadBalancer.ifPresent(result::add);
        }
        return result;
    }

    @Override
    public InstanceStoreMetadata collectInstanceStorageCount(AuthenticatedContext ac, List<String> instanceTypes) {
        return awsPlatformResources.collectInstanceStorageCount(ac, instanceTypes);
    }

    private List<CloudVmMetaDataStatus> collectInstances(List<CloudInstance> vms, List<CloudResource> resources, AmazonEc2Client ec2Client) {
        List<CloudVmMetaDataStatus> result = new ArrayList<>();
        Set<String> preferredInstanceIds = resources.stream()
                .filter(resource -> ResourceType.AWS_INSTANCE.equals(resource.getType()))
                .filter(resource -> vms.stream().anyMatch(vm -> String.valueOf(vm.getTemplate().getPrivateId()).equals(resource.getReference())))
                .map(CloudResource::getInstanceId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<String, CloudResource> resourcesByInstanceId = resources.stream()
                .filter(resource -> ResourceType.AWS_INSTANCE.equals(resource.getType()))
                .filter(cloudResource -> cloudResource.getInstanceId() != null)
                .collect(Collectors.toMap(CloudResource::getInstanceId, Function.identity()));
        final AtomicInteger counter = new AtomicInteger(0);
        Map<Integer, List<String>> instanceIdBatches = preferredInstanceIds.stream()
                .collect(Collectors.groupingBy(s -> counter.getAndIncrement() / instanceFetchMaxBatchSize, LinkedHashMap::new, Collectors.toList()));

        for (Map.Entry<Integer, List<String>> instanceIdBatchEntry : instanceIdBatches.entrySet()) {
            List<String> instanceIdBatch = instanceIdBatchEntry.getValue();
            LOGGER.info("Collecting metadata for instances iteration {}/{}", instanceIdBatchEntry.getKey() + 1, instanceIdBatches.size());
            try {
                result.addAll(describeInstances(vms, instanceIdBatch, resourcesByInstanceId, ec2Client));
            } catch (AmazonServiceException serviceException) {
                if (StringUtils.isNotEmpty(serviceException.getErrorCode()) && INSTANCE_NOT_FOUND_ERROR_CODE.equals(serviceException.getErrorCode())) {
                    result.addAll(handleNotFoundInstancesAndDescribeOthers(vms, ec2Client, instanceIdBatch, serviceException, resourcesByInstanceId));
                } else {
                    LOGGER.warn("Collection of instance metadata failed", serviceException);
                    throw serviceException;
                }
            }
        }
        return result;
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
            List<String> instanceIdList, AmazonServiceException serviceException, Map<String, CloudResource> resourcesByInstanceId) {
        LOGGER.info("One or more instances could not be found, collecting metadata for existing ones");
        int instanceIdNum = instanceIdList.size();
        List<CloudVmMetaDataStatus> result = new ArrayList<>(instanceIdNum);
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
                        result.add(new CloudVmMetaDataStatus(status, CloudInstanceMetaData.EMPTY_METADATA));
                    }
                });
        if (!existingInstanceIds.isEmpty()) {
            LOGGER.info("Existing instances on AWS to collect metadata for: {}/{}", existingInstanceIds.size(), instanceIdNum);
            result.addAll(describeInstances(vms, existingInstanceIds, resourcesByInstanceId, ec2Client));
        }
        return result;
    }

    private List<CloudVmMetaDataStatus> describeInstances(List<CloudInstance> vms, List<String> instanceIdList,
            Map<String, CloudResource> resourcesByInstanceId, AmazonEc2Client ec2Client) {
        LOGGER.info("Collecting metadata for instance IDs: '{}'", String.join(", ", instanceIdList));
        List<CloudVmMetaDataStatus> result = new ArrayList<>();
        DescribeInstancesRequest instancesRequest = new DescribeInstancesRequest().withInstanceIds(instanceIdList);
        DescribeInstancesResult instancesResult = ec2Client.describeInstances(instancesRequest);

        List<Instance> instances = instancesResult.getReservations().stream()
                .flatMap(reservation -> reservation.getInstances().stream())
                .collect(Collectors.toList());

        for (Instance instance : instances) {
            String instanceId = instance.getInstanceId();
            CloudResource connectedResource = findCloudResourceByInstanceId(resourcesByInstanceId, instanceId);
            CloudInstance matchedInstance = findCloudInstanceByPrivateId(vms, connectedResource);
            CloudInstance updatedInstance = new CloudInstance(connectedResource.getInstanceId(), matchedInstance.getTemplate(),
                    matchedInstance.getAuthentication(), matchedInstance.getSubnetId(), matchedInstance.getAvailabilityZone());
            CloudInstanceMetaData cloudInstanceMetaData = new CloudInstanceMetaData(
                    instance.getPrivateIpAddress(),
                    instance.getPublicIpAddress(),
                    awsLifeCycleMapper.getLifeCycle(instance));
            CloudVmInstanceStatus cloudVmInstanceStatus = new CloudVmInstanceStatus(updatedInstance, updatedInstance.getTemplate().getStatus());
            CloudVmMetaDataStatus newMetadataStatus = new CloudVmMetaDataStatus(cloudVmInstanceStatus, cloudInstanceMetaData);
            result.add(newMetadataStatus);
        }
        return result;
    }

    private Optional<CloudLoadBalancerMetadata> collectLoadBalancerMetadata(AmazonElasticLoadBalancingClient loadBalancingClient,
            String loadBalancerArn, List<CloudResource> resources) {
        Optional<CloudLoadBalancerMetadata> result = Optional.empty();
        try {
            result = describeLoadBalancer(loadBalancerArn, loadBalancingClient, resources);
        } catch (AmazonServiceException awsException) {
            if (StringUtils.isNotEmpty(awsException.getErrorCode()) && LOAD_BALANCER_NOT_FOUND_ERROR_CODE.equals(awsException.getErrorCode())) {
                LOGGER.info("Load balancers with ARN '{}' could not be found due to:", loadBalancerArn, awsException);
            } else {
                LOGGER.warn("Metadata collection failed for load balancer '{}'", loadBalancerArn, awsException);
                throw new CloudConnectorException("Metadata collection of load balancers failed", awsException);
            }
        } catch (RuntimeException e) {
            LOGGER.warn("Unable to fetch metadata for load balancer '{}'", loadBalancerArn, e);
            throw new CloudConnectorException("Metadata collection of load balancers failed", e);
        }
        return result;
    }

    private Optional<CloudLoadBalancerMetadata> describeLoadBalancer(String loadBalancerArn, AmazonElasticLoadBalancingClient loadBalancingClient,
            List<CloudResource> resources) {
        DescribeLoadBalancersRequest describeLoadBalancersRequest = new DescribeLoadBalancersRequest()
                .withLoadBalancerArns(loadBalancerArn);
        DescribeLoadBalancersResult describeLoadBalancersResult = loadBalancingClient.describeLoadBalancers(describeLoadBalancersRequest);

        return describeLoadBalancersResult.getLoadBalancers().stream()
                .findFirst()
                .map(loadBalancer -> {
                    LoadBalancerType type = loadBalancerTypeConverter.convert(loadBalancer.getScheme());
                    Map<String, Object> parameters = awsNativeLbMetadataCollector.getParameters(loadBalancerArn, resources);
                    CloudLoadBalancerMetadata loadBalancerMetadata = new CloudLoadBalancerMetadata.Builder()
                            .withType(type)
                            .withCloudDns(loadBalancer.getDNSName())
                            .withHostedZoneId(loadBalancer.getCanonicalHostedZoneId())
                            .withName(loadBalancer.getLoadBalancerName())
                            .withParameters(parameters)
                            .build();
                    LOGGER.info("Saved metadata for load balancer {}: DNS {}, zone ID {}", loadBalancer.getLoadBalancerName(), loadBalancer.getDNSName(),
                            loadBalancer.getCanonicalHostedZoneId());
                    return loadBalancerMetadata;
                });
    }

}
