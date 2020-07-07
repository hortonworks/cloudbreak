package com.sequenceiq.cloudbreak.cloud.aws;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.CreateTagsRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceBlockDeviceMapping;
import com.amazonaws.services.ec2.model.Tag;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.common.api.tag.model.Tags;

@Service
public class AwsTaggingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsTaggingService.class);

    private static final int MAX_RESOURCE_PER_REQUEST = 1000;

    public Collection<com.amazonaws.services.cloudformation.model.Tag> prepareCloudformationTags(AuthenticatedContext ac, Tags userDefinedTags) {
        return userDefinedTags.getAll().entrySet().stream()
                .map(entry -> prepareCloudformationTag(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    public Collection<com.amazonaws.services.ec2.model.Tag> prepareEc2Tags(AuthenticatedContext ac, Tags userDefinedTags) {
        return userDefinedTags.getAll().entrySet().stream()
                .map(entry -> prepareEc2Tag(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    public void tagRootVolumes(AuthenticatedContext ac, AmazonEC2Client ec2Client, List<CloudResource> instanceResources, Tags userDefinedTags) {
        String stackName = ac.getCloudContext().getName();
        LOGGER.debug("Fetch AWS instances to collect all root volume ids for stack: {}", stackName);
        List<String> instanceIds = instanceResources.stream().map(CloudResource::getInstanceId).collect(Collectors.toList());
        DescribeInstancesResult describeInstancesResult = ec2Client.describeInstances(new DescribeInstancesRequest().withInstanceIds(instanceIds));

        List<Instance> instances = describeInstancesResult.getReservations().stream().flatMap(res -> res.getInstances().stream()).collect(Collectors.toList());
        List<String> rootVolumeIds = instances.stream()
                .map(this::getRootVolumeId)
                .filter(Optional::isPresent)
                .map(blockDeviceMapping -> blockDeviceMapping.get().getEbs().getVolumeId())
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

        Collection<Tag> tags = prepareEc2Tags(ac, userDefinedTags);
        for (List<String> volumeIds : volumeIdChunks) {
            LOGGER.debug("Tag {} root volumes for stack: {}", volumeIds.size(), stackName);
            ec2Client.createTags(new CreateTagsRequest().withResources(volumeIds).withTags(tags));
        }
    }

    private com.amazonaws.services.cloudformation.model.Tag prepareCloudformationTag(String key, String value) {
        return new com.amazonaws.services.cloudformation.model.Tag().withKey(key).withValue(value);
    }

    private com.amazonaws.services.ec2.model.Tag prepareEc2Tag(String key, String value) {
        return new com.amazonaws.services.ec2.model.Tag().withKey(key).withValue(value);
    }

    private Optional<InstanceBlockDeviceMapping> getRootVolumeId(com.amazonaws.services.ec2.model.Instance instance) {
        return instance.getBlockDeviceMappings().stream().filter(mapping -> mapping.getDeviceName().equals(instance.getRootDeviceName())).findFirst();
    }
}
