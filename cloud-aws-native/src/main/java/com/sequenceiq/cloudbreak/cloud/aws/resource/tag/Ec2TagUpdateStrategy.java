package com.sequenceiq.cloudbreak.cloud.aws.resource.tag;

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

import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.TagUpdateStrategy;
import com.sequenceiq.cloudbreak.cloud.aws.common.CommonAwsClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.common.api.type.ResourceType;

import software.amazon.awssdk.services.ec2.model.CreateTagsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeVolumesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeVolumesResponse;
import software.amazon.awssdk.services.ec2.model.Filter;
import software.amazon.awssdk.services.ec2.model.Tag;
import software.amazon.awssdk.services.ec2.model.Volume;

@Service
public class Ec2TagUpdateStrategy implements TagUpdateStrategy {

    @Inject
    private CommonAwsClient commonAwsClient;

    @Override
    public Set<ResourceType> supportedTypes() {
        return Set.of(AWS_INSTANCE, AWS_SECURITY_GROUP, AWS_ROOT_DISK, AWS_ROOT_DISK_TAGGING, AWS_ENCRYPTED_VOLUME, AWS_VOLUMESET, AWS_SNAPSHOT,
                AWS_ENCRYPTED_AMI, AWS_RESERVED_IP, AWS_SSH_KEY);
    }

    @Override
    public void updateTags(AuthenticatedContext authenticatedContext, CloudResource cloudResource, Map<String, String> tags) {
        AmazonEc2Client ec2Client = commonAwsClient.createEc2Client(authenticatedContext);
        List<Tag> ec2Tags = tags.entrySet().stream()
                .map(e -> Tag.builder()
                        .key(e.getKey())
                        .value(e.getValue())
                        .build())
                .toList();

        List<String> references = switch (cloudResource.getType()) {
            case AWS_INSTANCE -> List.of(cloudResource.getInstanceId());
            case AWS_ROOT_DISK, AWS_VOLUMESET -> resolveVolumeIds(ec2Client, cloudResource.getInstanceId());
            case AWS_SECURITY_GROUP -> List.of(cloudResource.getReference());
            default -> List.of(cloudResource.getReference());
        };

        ec2Client.createTags(CreateTagsRequest.builder()
                .resources(references)
                .tags(ec2Tags)
                .build());
    }

    private List<String> resolveVolumeIds(AmazonEc2Client ec2Client, String instanceId) {
        DescribeVolumesResponse response = ec2Client.describeVolumes(DescribeVolumesRequest.builder()
                .filters(
                        Filter.builder()
                                .name("attachment.instance-id")
                                .values(instanceId)
                                .build()
                )
                .build());

        return response.volumes().stream()
                .map(Volume::volumeId)
                .toList();
    }
}
