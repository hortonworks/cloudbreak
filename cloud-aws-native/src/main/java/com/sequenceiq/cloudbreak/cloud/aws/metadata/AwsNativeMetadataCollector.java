package com.sequenceiq.cloudbreak.cloud.aws.metadata;

import static com.sequenceiq.cloudbreak.cloud.aws.common.AwsInstanceConnector.INSTANCE_NOT_FOUND_ERROR_CODE;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
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
import com.amazonaws.services.elasticloadbalancingv2.model.LoadBalancer;
import com.sequenceiq.cloudbreak.cloud.MetadataCollector;
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
import com.sequenceiq.cloudbreak.cloud.model.InstanceStoreMetadata;
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

    @Value("${cb.aws.native.instance.fetch.max.item:100}")
    private int instanceFetchMaxBatchSize;

    @Override
    public List<CloudVmMetaDataStatus> collect(AuthenticatedContext ac, List<CloudResource> resources, List<CloudInstance> vms,
            List<CloudInstance> allInstances) {
        LOGGER.debug("Collect AWS instance metadata, for cluster {} resources: {}, vms: {}, allInstances: {}",
                ac.getCloudContext().getName(), resources.size(), vms.size(), allInstances.size());
        try {
            String region = ac.getCloudContext().getLocation().getRegion().value();
            List<String> instanceIds = resources.stream()
                    .filter(resource -> ResourceType.AWS_INSTANCE.equals(resource.getType()))
                    .map(CloudResource::getInstanceId)
                    .collect(Collectors.toList());

            AmazonEc2Client ec2Client = awsClient.createEc2Client(new AwsCredentialView(ac.getCloudCredential()), region);

            return collectInstances(vms, instanceIds, ec2Client);
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
            Optional<CloudLoadBalancerMetadata> collectedLoadBalancer = collectLoadBalancerMetadata(loadBalancingClient, loadBalancerArn);
            collectedLoadBalancer.ifPresent(result::add);
        }
        return result;
    }

    @Override
    public InstanceStoreMetadata collectInstanceStorageCount(AuthenticatedContext ac, List<String> instanceTypes) {
        return null;
    }

    private List<CloudVmMetaDataStatus> collectInstances(List<CloudInstance> vms, List<String> knownInstanceIdList, AmazonEc2Client ec2Client) {
        List<CloudVmMetaDataStatus> result = new ArrayList<>();
        final AtomicInteger counter = new AtomicInteger(0);
        Map<Integer, List<String>> instanceIdBatches = knownInstanceIdList.stream()
                .collect(Collectors.groupingBy(s -> counter.getAndIncrement() / instanceFetchMaxBatchSize));
        for (Map.Entry<Integer, List<String>> instanceIdBatchEntry : instanceIdBatches.entrySet()) {
            List<String> instanceIdBatch = instanceIdBatchEntry.getValue();
            LOGGER.info("Collecting metadata for instances iteration {}/{}", instanceIdBatchEntry.getKey(), instanceIdBatches.size());
            try {
                List<CloudVmMetaDataStatus> cloudVmMetaDataStatuses = describeInstances(vms, instanceIdBatch, ec2Client);
                result.addAll(cloudVmMetaDataStatuses);
            } catch (AmazonServiceException serviceException) {
                if (StringUtils.isNotEmpty(serviceException.getErrorCode()) && INSTANCE_NOT_FOUND_ERROR_CODE.equals(serviceException.getErrorCode())) {
                    LOGGER.info("One or more load balancers could not be found, collecting metadata for existing ones");
                    List<String> existingInstances = instanceIdBatch.stream()
                            .filter(instanceId -> !serviceException.getMessage().contains(instanceId))
                            .collect(Collectors.toList());
                    if (!existingInstances.isEmpty()) {
                        LOGGER.info("Existing instances on AWS to collect metadata: {}/{}", existingInstances, instanceFetchMaxBatchSize);
                        result.addAll(describeInstances(vms, existingInstances, ec2Client));
                    }
                } else {
                    LOGGER.warn("Collection of instance metadata failed", serviceException);
                    throw serviceException;
                }
            }
        }
        return result;
    }

    private List<CloudVmMetaDataStatus> describeInstances(List<CloudInstance> vms, List<String> knownInstanceIdList, AmazonEc2Client ec2Client) {
        LOGGER.info("Collecting metadata for instance ids: '{}'", String.join(", ", knownInstanceIdList));
        List<CloudVmMetaDataStatus> result = new ArrayList<>();
        DescribeInstancesRequest instancesRequest = new DescribeInstancesRequest().withInstanceIds(knownInstanceIdList);
        DescribeInstancesResult instancesResult = ec2Client.describeInstances(instancesRequest);

        List<Instance> instances = instancesResult.getReservations().stream()
                .flatMap(reservation -> reservation.getInstances().stream())
                .collect(Collectors.toList());

        for (Instance instance : instances) {
            CloudInstance cloudInstance = vms.stream()
                    .filter(ci -> instance.getInstanceId().equals(ci.getInstanceId()))
                    .findFirst()
                    .orElseThrow();
            CloudInstanceMetaData cloudInstanceMetaData = new CloudInstanceMetaData(
                    instance.getPrivateIpAddress(),
                    instance.getPublicIpAddress(),
                    awsLifeCycleMapper.getLifeCycle(instance));
            CloudVmInstanceStatus cloudVmInstanceStatus = new CloudVmInstanceStatus(cloudInstance, cloudInstance.getTemplate().getStatus());
            CloudVmMetaDataStatus newMetadataStatus = new CloudVmMetaDataStatus(cloudVmInstanceStatus, cloudInstanceMetaData);
            result.add(newMetadataStatus);
        }
        return result;
    }

    private Optional<CloudLoadBalancerMetadata> collectLoadBalancerMetadata(AmazonElasticLoadBalancingClient loadBalancingClient, String loadBalancerArn) {
        Optional<CloudLoadBalancerMetadata> result = Optional.empty();
        try {
            result = Optional.of(describeLoadBalancers(loadBalancerArn, loadBalancingClient));
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

    private CloudLoadBalancerMetadata describeLoadBalancers(String loadBalancerArn, AmazonElasticLoadBalancingClient loadBalancingClient) {
        CloudLoadBalancerMetadata result = null;
        DescribeLoadBalancersRequest describeLoadBalancersRequest = new DescribeLoadBalancersRequest()
                .withLoadBalancerArns(loadBalancerArn);
        DescribeLoadBalancersResult describeLoadBalancersResult = loadBalancingClient.describeLoadBalancers(describeLoadBalancersRequest);

        for (LoadBalancer loadBalancer : describeLoadBalancersResult.getLoadBalancers()) {
            LoadBalancerType type = loadBalancerTypeConverter.convert(loadBalancer.getScheme());
            CloudLoadBalancerMetadata loadBalancerMetadata = new CloudLoadBalancerMetadata.Builder()
                    .withType(type)
                    .withCloudDns(loadBalancer.getDNSName())
                    .withHostedZoneId(loadBalancer.getCanonicalHostedZoneId())
                    .withName(loadBalancer.getLoadBalancerName())
                    .build();
            result = loadBalancerMetadata;
            LOGGER.info("Saved metadata for load balancer {}: DNS {}, zone id {}", loadBalancer.getLoadBalancerName(), loadBalancer.getDNSName(),
                    loadBalancer.getCanonicalHostedZoneId());
        }
        return result;
    }
}
