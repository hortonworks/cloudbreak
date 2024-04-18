package com.sequenceiq.cloudbreak.cloud.aws.common;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;

import software.amazon.awssdk.services.ec2.model.CreateTagsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesResponse;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.InstanceBlockDeviceMapping;
import software.amazon.awssdk.services.ec2.model.ResourceType;
import software.amazon.awssdk.services.ec2.model.Tag;
import software.amazon.awssdk.services.ec2.model.TagSpecification;

@Service
public class AwsTaggingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsTaggingService.class);

    private static final int MAX_RESOURCE_PER_REQUEST = 1000;

    public Collection<software.amazon.awssdk.services.cloudformation.model.Tag> prepareCloudformationTags(AuthenticatedContext ac,
            Map<String, String> userDefinedTags) {
        return userDefinedTags.entrySet().stream()
                .map(entry -> prepareCloudformationTag(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    public Collection<software.amazon.awssdk.services.ec2.model.Tag> prepareEc2Tags(Map<String, String> userDefinedTags) {
        return userDefinedTags.entrySet().stream()
                .map(entry -> prepareEc2Tag(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    public Collection<software.amazon.awssdk.services.kms.model.Tag> prepareKmsTags(Map<String, String> userDefinedTags) {
        return userDefinedTags.entrySet().stream()
                .map(entry -> prepareKmsTag(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    public Collection<software.amazon.awssdk.services.cloudwatch.model.Tag> prepareCloudWatchTags(Map<String, String> userDefinedTags) {
        return userDefinedTags.entrySet().stream()
                .map(entry -> prepareCloudWatchTag(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    public Collection<software.amazon.awssdk.services.elasticloadbalancingv2.model.Tag> prepareElasticLoadBalancingTags(Map<String, String> userDefinedTags) {
        return userDefinedTags.entrySet().stream()
                .map(entry -> prepareElasticLoadBalancingTag(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    public Collection<software.amazon.awssdk.services.secretsmanager.model.Tag> prepareSecretsManagerTags(Map<String, String> userDefinedTags) {
        return userDefinedTags.entrySet().stream()
                .map(entry -> prepareSecretsManagerTag(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    public Collection<software.amazon.awssdk.services.efs.model.Tag> prepareEfsTags(Map<String, String> userDefinedTags) {
        return userDefinedTags.entrySet().stream()
                .map(entry -> prepareEfsTag(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    public Map<String, String> convertAwsEfsTags(Collection<software.amazon.awssdk.services.efs.model.Tag> awsTags) {
        return awsTags.stream()
                .collect(Collectors.toMap(software.amazon.awssdk.services.efs.model.Tag::key, software.amazon.awssdk.services.efs.model.Tag::value));
    }

    public void tagRootVolumes(AuthenticatedContext ac, AmazonEc2Client ec2Client, List<CloudResource> instanceResources,
            Map<String, String> userDefinedTags) {
        String stackName = ac.getCloudContext().getName();
        LOGGER.debug("Fetch AWS instances to collect all root volume ids for stack: {}", stackName);
        List<String> instanceIds = instanceResources.stream().map(CloudResource::getInstanceId).collect(Collectors.toList());
        DescribeInstancesResponse describeInstancesResponse = ec2Client.describeInstances(DescribeInstancesRequest.builder().instanceIds(instanceIds).build());

        List<Instance> instances = describeInstancesResponse.reservations().stream().flatMap(res -> res.instances().stream()).collect(Collectors.toList());
        List<String> rootVolumeIds = instances.stream()
                .map(this::getRootVolumeId)
                .filter(Optional::isPresent)
                .map(blockDeviceMapping -> blockDeviceMapping.get().ebs().volumeId())
                .collect(Collectors.toList());

        int instanceCount = instances.size();
        int volumeCount = rootVolumeIds.size();
        if (instanceCount != volumeCount) {
            LOGGER.debug("Did not find all root volumes, instanceResources: {}, found root volumes: {} for stack: {}", instanceCount, volumeCount, stackName);
        } else {
            LOGGER.debug("Found all ({}) root volumes for stack: {}", volumeCount, stackName);
        }

        AtomicInteger counter = new AtomicInteger();
        Collection<List<String>> volumeIdChunks = rootVolumeIds.stream()
                .collect(Collectors.groupingBy(it -> counter.getAndIncrement() / MAX_RESOURCE_PER_REQUEST)).values();

        Collection<Tag> tags = prepareEc2Tags(userDefinedTags);
        for (List<String> volumeIds : volumeIdChunks) {
            LOGGER.debug("Tag {} root volumes for stack: {}", volumeIds.size(), stackName);
            ec2Client.createTags(CreateTagsRequest.builder().resources(volumeIds).tags(tags).build());
        }
    }

    public TagSpecification prepareEc2TagSpecification(Map<String, String> userDefinedTags, ResourceType resourceType) {
        return TagSpecification.builder().tags(prepareEc2Tags(userDefinedTags)).resourceType(resourceType).build();
    }

    private software.amazon.awssdk.services.cloudformation.model.Tag prepareCloudformationTag(String key, String value) {
        return software.amazon.awssdk.services.cloudformation.model.Tag.builder().key(key).value(value).build();
    }

    private software.amazon.awssdk.services.ec2.model.Tag prepareEc2Tag(String key, String value) {
        return software.amazon.awssdk.services.ec2.model.Tag.builder().key(key).value(value).build();
    }

    private software.amazon.awssdk.services.kms.model.Tag prepareKmsTag(String key, String value) {
        return software.amazon.awssdk.services.kms.model.Tag.builder()
                .tagKey(key)
                .tagValue(value)
                .build();
    }

    private software.amazon.awssdk.services.cloudwatch.model.Tag prepareCloudWatchTag(String key, String value) {
        return software.amazon.awssdk.services.cloudwatch.model.Tag.builder().key(key).value(value).build();
    }

    private software.amazon.awssdk.services.efs.model.Tag prepareEfsTag(String key, String value) {
        return software.amazon.awssdk.services.efs.model.Tag.builder().key(key).value(value).build();
    }

    private Optional<InstanceBlockDeviceMapping> getRootVolumeId(software.amazon.awssdk.services.ec2.model.Instance instance) {
        return instance.blockDeviceMappings().stream().filter(mapping -> mapping.deviceName().equals(instance.rootDeviceName())).findFirst();
    }

    private software.amazon.awssdk.services.elasticloadbalancingv2.model.Tag prepareElasticLoadBalancingTag(String key, String value) {
        return software.amazon.awssdk.services.elasticloadbalancingv2.model.Tag.builder().key(key).value(value).build();
    }

    private software.amazon.awssdk.services.secretsmanager.model.Tag prepareSecretsManagerTag(String key, String value) {
        return software.amazon.awssdk.services.secretsmanager.model.Tag.builder().key(key).value(value).build();
    }
}
