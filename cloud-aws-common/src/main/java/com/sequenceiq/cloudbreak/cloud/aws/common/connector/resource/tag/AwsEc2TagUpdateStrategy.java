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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.cloud.TagUpdateStrategy;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsTaggingService;
import com.sequenceiq.cloudbreak.cloud.aws.common.CommonAwsClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.common.api.type.ResourceType;

import software.amazon.awssdk.services.ec2.model.CreateTagsRequest;
import software.amazon.awssdk.services.ec2.model.DeleteTagsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeTagsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeTagsResponse;
import software.amazon.awssdk.services.ec2.model.DescribeVolumesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeVolumesResponse;
import software.amazon.awssdk.services.ec2.model.Filter;
import software.amazon.awssdk.services.ec2.model.Tag;
import software.amazon.awssdk.services.ec2.model.TagDescription;
import software.amazon.awssdk.services.ec2.model.Volume;

@Service
public class AwsEc2TagUpdateStrategy implements TagUpdateStrategy {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsEc2TagUpdateStrategy.class);

    private static final int TAG_UPDATE_BATCH_SIZE = 1000;

    private static final int DESCRIBE_BATCH_SIZE = 200;

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

        List<String> resourcesToUpdate = resolveResourceIds(ec2Client, List.of(cloudResource), needsUpdate(tags));

        if (resourcesToUpdate.isEmpty()) {
            LOGGER.info("Tags for resource {} of type {} are already up to date, skipping update.", cloudResource.getName(), cloudResource.getType());
            return;
        }

        Collection<Tag> ec2Tags = awsTaggingService.prepareEc2Tags(tags);

        ec2Client.createTags(CreateTagsRequest.builder()
                .resources(resourcesToUpdate)
                .tags(ec2Tags)
                .build());
    }

    @Override
    public boolean isBatchUpdateSupported() {
        return true;
    }

    @Override
    public void batchUpdateTags(AuthenticatedContext authenticatedContext, List<CloudResource> cloudResources, Map<String, String> tags) {
        AmazonEc2Client ec2Client = commonAwsClient.createEc2Client(authenticatedContext);

        List<String> resourcesToUpdate = resolveResourceIds(ec2Client, cloudResources, needsUpdate(tags));

        if (resourcesToUpdate.isEmpty()) {
            LOGGER.info("Tags for all {} EC2 resources are already up to date, skipping update.", cloudResources.size());
            return;
        }

        Collection<Tag> ec2Tags = awsTaggingService.prepareEc2Tags(tags);

        Lists.partition(resourcesToUpdate, TAG_UPDATE_BATCH_SIZE).forEach(batch ->
                ec2Client.createTags(CreateTagsRequest.builder()
                        .resources(batch)
                        .tags(ec2Tags)
                        .build())
        );
    }

    @Override
    public void deleteTags(AuthenticatedContext authenticatedContext, CloudResource cloudResource, Set<String> tagKeys) {
        AmazonEc2Client ec2Client = commonAwsClient.createEc2Client(authenticatedContext);

        List<String> resourcesToDeleteTagsFrom = resolveResourceIds(ec2Client, List.of(cloudResource), hasTagsToDelete(tagKeys));

        if (resourcesToDeleteTagsFrom.isEmpty()) {
            LOGGER.info("No tags to delete for resource {} of type {}, skipping.", cloudResource.getName(), cloudResource.getType());
            return;
        }

        logTagKeyDeletion(LOGGER, String.format("%s (%s), EC2 resources: %s",
                cloudResource.getName(), cloudResource.getType(), resourcesToDeleteTagsFrom), tagKeys);

        ec2Client.deleteTags(DeleteTagsRequest.builder()
                .resources(resourcesToDeleteTagsFrom)
                .tags(tagKeysAsTags(tagKeys))
                .build());
    }

    @Override
    public boolean isBatchDeleteSupported() {
        return true;
    }

    @Override
    public void batchDeleteTags(AuthenticatedContext authenticatedContext, List<CloudResource> cloudResources, Set<String> tagKeys) {
        AmazonEc2Client ec2Client = commonAwsClient.createEc2Client(authenticatedContext);

        List<String> resourcesToDeleteTagsFrom = resolveResourceIds(ec2Client, cloudResources, hasTagsToDelete(tagKeys));

        if (resourcesToDeleteTagsFrom.isEmpty()) {
            LOGGER.info("No tags to delete for all {} EC2 resources, skipping.", cloudResources.size());
            return;
        }

        Collection<Tag> ec2Tags = tagKeysAsTags(tagKeys);

        Lists.partition(resourcesToDeleteTagsFrom, TAG_UPDATE_BATCH_SIZE).forEach(batch ->
                ec2Client.deleteTags(DeleteTagsRequest.builder()
                        .resources(batch)
                        .tags(ec2Tags)
                        .build())
        );
    }

    private Predicate<Map<String, String>> needsUpdate(Map<String, String> newTags) {
        return existingTags -> !tagsAlreadyUpToDate(existingTags, newTags);
    }

    private Predicate<Map<String, String>> hasTagsToDelete(Set<String> tagKeys) {
        return existingTags -> hasTagKeysToDelete(existingTags, tagKeys);
    }

    private Collection<Tag> tagKeysAsTags(Set<String> tagKeys) {
        return tagKeys.stream()
                .map(key -> Tag.builder().key(key).build())
                .toList();
    }

    private List<String> resolveResourceIds(AmazonEc2Client ec2Client, List<CloudResource> cloudResources, Predicate<Map<String, String>> tagPredicate) {
        List<String> resourceIds = new ArrayList<>();

        Map<ResourceType, List<CloudResource>> cloudResourcesByType = cloudResources.stream()
                .collect(Collectors.groupingBy(CloudResource::getType));

        cloudResourcesByType.forEach((type, resources) -> {
            switch (type) {
                case AWS_ROOT_DISK, AWS_VOLUMESET -> {
                    List<String> instanceIds = resources.stream().map(CloudResource::getInstanceId).distinct().toList();
                    resourceIds.addAll(volumeIdsMatching(ec2Client, instanceIds, tagPredicate));
                }
                case AWS_INSTANCE -> {
                    List<String> instanceIds = resources.stream().map(CloudResource::getInstanceId).toList();
                    resourceIds.addAll(resourceIdsMatching(ec2Client, instanceIds, tagPredicate));
                }
                default -> {
                    List<String> refs = resources.stream().map(CloudResource::getReference).toList();
                    resourceIds.addAll(resourceIdsMatching(ec2Client, refs, tagPredicate));
                }
            }
        });

        return resourceIds;
    }

    private List<String> volumeIdsMatching(AmazonEc2Client ec2Client, List<String> instanceIds, Predicate<Map<String, String>> tagPredicate) {
        return Lists.partition(instanceIds, DESCRIBE_BATCH_SIZE).stream()
                .flatMap(batch -> {
                    DescribeVolumesResponse response = ec2Client.describeVolumes(
                            DescribeVolumesRequest.builder()
                                    .filters(Filter.builder()
                                            .name("attachment.instance-id")
                                            .values(batch)
                                            .build())
                                    .build());
                    return response.volumes().stream()
                            .filter(volume -> tagPredicate.test(toTagMap(volume.tags())))
                            .map(Volume::volumeId);
                })
                .toList();
    }

    private List<String> resourceIdsMatching(AmazonEc2Client ec2Client, List<String> resourceIds, Predicate<Map<String, String>> tagPredicate) {
        return Lists.partition(resourceIds, DESCRIBE_BATCH_SIZE).stream()
                .flatMap(batch -> {
                    DescribeTagsResponse response = ec2Client.describeTags(
                            DescribeTagsRequest.builder()
                                .filters(Filter.builder()
                                    .name("resource-id")
                                    .values(batch)
                                    .build())
                                .build());

                    Map<String, Map<String, String>> existingTagsByResource = response.tags().stream()
                            .collect(Collectors.groupingBy(
                                TagDescription::resourceId,
                                Collectors.toMap(TagDescription::key, TagDescription::value)
                            ));

                    return batch.stream()
                            .filter(resourceId -> tagPredicate.test(existingTagsByResource.getOrDefault(resourceId, Map.of())));
                })
                .toList();
    }

    private Map<String, String> toTagMap(List<Tag> tags) {
        return tags.stream().collect(Collectors.toMap(Tag::key, Tag::value));
    }
}
