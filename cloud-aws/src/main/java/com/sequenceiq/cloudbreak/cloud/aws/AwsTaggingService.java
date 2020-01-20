package com.sequenceiq.cloudbreak.cloud.aws;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.CreateTagsRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceBlockDeviceMapping;
import com.amazonaws.services.ec2.model.Tag;
import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;

@Service
public class AwsTaggingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsTaggingService.class);

    private static final String CLOUDBREAK_ID = "CloudbreakId";

    private static final String CLOUDBREAK_CLUSTER_TAG = "CloudbreakClusterName";

    private static final int MAX_RESOURCE_PER_REQUEST = 1000;

    @Value("${cb.aws.default.cf.tag:}")
    private String defaultCloudformationTag;

    @Value("#{'${cb.aws.custom.cf.tags:}'.split(',')}")
    private List<String> customCloudformationTags;

    private Map<String, String> customTags = new HashMap<>();

    @PostConstruct
    public void init() {
        customTags = new HashMap<>();
        if (customCloudformationTags != null && !customCloudformationTags.isEmpty()) {
            customCloudformationTags.stream().filter(field -> !field.isEmpty()).forEach(field -> {
                String[] splittedField = field.split(":");
                customTags.put(splittedField[0], splittedField[1]);
            });
        }
    }

    public Collection<com.amazonaws.services.cloudformation.model.Tag> prepareCloudformationTags(AuthenticatedContext ac, Map<String, String> userDefinedTags) {
        Collection<com.amazonaws.services.cloudformation.model.Tag> tags = new ArrayList<>();
        Optional.ofNullable(ac)
                .map(AuthenticatedContext::getCloudContext)
                .map(CloudContext::getName)
                .ifPresent(clusterName -> tags.add(prepareCloudformationTag(CLOUDBREAK_CLUSTER_TAG, clusterName)));
        if (!Strings.isNullOrEmpty(defaultCloudformationTag)) {
            tags.add(prepareCloudformationTag(CLOUDBREAK_ID, defaultCloudformationTag));
        }
        tags.addAll(Stream.concat(customTags.entrySet().stream(), userDefinedTags.entrySet().stream())
                .map(entry -> prepareCloudformationTag(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList()));
        return tags;
    }

    public Collection<com.amazonaws.services.ec2.model.Tag> prepareEc2Tags(AuthenticatedContext ac, Map<String, String> userDefinedTags) {
        Collection<com.amazonaws.services.ec2.model.Tag> tags = new ArrayList<>();
        tags.add(prepareEc2Tag(CLOUDBREAK_CLUSTER_TAG, ac.getCloudContext().getName()));
        if (!Strings.isNullOrEmpty(defaultCloudformationTag)) {
            tags.add(prepareEc2Tag(CLOUDBREAK_ID, defaultCloudformationTag));
        }
        tags.addAll(Stream.concat(customTags.entrySet().stream(), userDefinedTags.entrySet().stream())
                .map(entry -> prepareEc2Tag(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList()));
        return tags;
    }

    public void tagRootVolumes(AuthenticatedContext ac, AmazonEC2Client ec2Client, List<CloudResource> instanceResources, Map<String, String> userDefinedTags) {
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
