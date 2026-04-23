package com.sequenceiq.cloudbreak.cloud.aws.common.connector.resource.tag;

import static com.sequenceiq.common.api.type.ResourceType.AWS_ENCRYPTED_AMI;
import static com.sequenceiq.common.api.type.ResourceType.AWS_ENCRYPTED_VOLUME;
import static com.sequenceiq.common.api.type.ResourceType.AWS_INSTANCE;
import static com.sequenceiq.common.api.type.ResourceType.AWS_RESERVED_IP;
import static com.sequenceiq.common.api.type.ResourceType.AWS_ROOT_DISK;
import static com.sequenceiq.common.api.type.ResourceType.AWS_ROOT_DISK_TAGGING;
import static com.sequenceiq.common.api.type.ResourceType.AWS_SECURITY_GROUP;
import static com.sequenceiq.common.api.type.ResourceType.AWS_SNAPSHOT;
import static com.sequenceiq.common.api.type.ResourceType.AWS_SSH_KEY;
import static com.sequenceiq.common.api.type.ResourceType.AWS_VOLUMESET;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.TagUpdateStrategy;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsTaggingService;
import com.sequenceiq.cloudbreak.cloud.aws.common.CommonAwsClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.common.api.type.ResourceType;

import software.amazon.awssdk.services.ec2.model.CreateTagsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeTagsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeTagsResponse;
import software.amazon.awssdk.services.ec2.model.DescribeVolumesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeVolumesResponse;
import software.amazon.awssdk.services.ec2.model.Filter;
import software.amazon.awssdk.services.ec2.model.Tag;
import software.amazon.awssdk.services.ec2.model.TagDescription;
import software.amazon.awssdk.services.ec2.model.Volume;

@Service
public class Ec2TagUpdateStrategy implements TagUpdateStrategy {

    private static final Logger LOGGER = LoggerFactory.getLogger(Ec2TagUpdateStrategy.class);

    @Inject
    private CommonAwsClient commonAwsClient;

    @Inject
    private AwsTaggingService awsTaggingService;

    @Override
    public Set<ResourceType> supportedTypes() {
        return Set.of(AWS_INSTANCE, AWS_SECURITY_GROUP, AWS_ROOT_DISK, AWS_ROOT_DISK_TAGGING,
                AWS_ENCRYPTED_VOLUME, AWS_VOLUMESET, AWS_SNAPSHOT, AWS_ENCRYPTED_AMI, AWS_RESERVED_IP, AWS_SSH_KEY);
    }

    @Override
    public void updateTags(AuthenticatedContext authenticatedContext, CloudResource cloudResource, Map<String, String> tags) {
        AmazonEc2Client ec2Client = commonAwsClient.createEc2Client(authenticatedContext);

        List<String> resourcesToUpdate = switch (cloudResource.getType()) {
            case AWS_ROOT_DISK, AWS_VOLUMESET -> resolveVolumeIdsToUpdate(ec2Client, cloudResource.getInstanceId(), tags);
            case AWS_INSTANCE                 -> filterResourcesToUpdate(ec2Client, List.of(cloudResource.getInstanceId()), tags);
            case AWS_SECURITY_GROUP           -> filterResourcesToUpdate(ec2Client, List.of(cloudResource.getReference()), tags);
            default                           -> filterResourcesToUpdate(ec2Client, List.of(cloudResource.getReference()), tags);
        };

        if (resourcesToUpdate.isEmpty()) {
            LOGGER.info("Tags for resource {} of type {} are already up to date, skipping update.",
                    cloudResource.getName(), cloudResource.getType());
            return;
        }

        Collection<Tag> ec2Tags = awsTaggingService.prepareEc2Tags(tags);

        ec2Client.createTags(CreateTagsRequest.builder()
                .resources(resourcesToUpdate)
                .tags(ec2Tags)
                .build());
    }

    private List<String> resolveVolumeIdsToUpdate(AmazonEc2Client ec2Client,
            String instanceId, Map<String, String> newTags) {
        DescribeVolumesResponse response = ec2Client.describeVolumes(
                DescribeVolumesRequest.builder()
                        .filters(Filter.builder()
                                .name("attachment.instance-id")
                                .values(instanceId)
                                .build())
                        .build());

        return response.volumes().stream()
                .filter(volume -> !tagsAlreadyUpToDate(toTagMap(volume.tags()), newTags))
                .map(Volume::volumeId)
                .toList();
    }

    private List<String> filterResourcesToUpdate(AmazonEc2Client ec2Client,
            List<String> resourceIds, Map<String, String> newTags) {
        DescribeTagsResponse response = ec2Client.describeTags(
                DescribeTagsRequest.builder()
                        .filters(Filter.builder()
                                .name("resource-id")
                                .values(resourceIds)
                                .build())
                        .build());

        Map<String, Map<String, String>> existingTagsByResource = response.tags().stream()
                .collect(Collectors.groupingBy(
                        TagDescription::resourceId,
                        Collectors.toMap(TagDescription::key, TagDescription::value)
                ));

        return resourceIds.stream()
                .filter(resourceId -> {
                    Map<String, String> existingTags = existingTagsByResource
                            .getOrDefault(resourceId, Map.of());
                    return !tagsAlreadyUpToDate(existingTags, newTags);
                })
                .toList();
    }

    private Map<String, String> toTagMap(List<Tag> tags) {
        return tags.stream().collect(Collectors.toMap(Tag::key, Tag::value));
    }
}