package com.sequenceiq.cloudbreak.cloud.aws.common.service;

import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.google.api.client.util.Lists;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsTaggingService;
import com.sequenceiq.cloudbreak.cloud.aws.common.resource.volume.AwsVolumeIopsCalculator;
import com.sequenceiq.cloudbreak.cloud.aws.common.resource.volume.AwsVolumeThroughputCalculator;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsInstanceView;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;

import software.amazon.awssdk.services.ec2.model.CreateVolumeRequest;
import software.amazon.awssdk.services.ec2.model.Tag;
import software.amazon.awssdk.services.ec2.model.TagSpecification;

@Component
public class AwsCommonDiskUtilService {

    @Inject
    private AwsTaggingService awsTaggingService;

    @Inject
    private AwsVolumeIopsCalculator awsVolumeIopsCalculator;

    @Inject
    private AwsVolumeThroughputCalculator awsVolumeThroughputCalculator;

    public boolean isEncryptedVolumeRequested(Group group) {
        return new AwsInstanceView(group.getReferenceInstanceTemplate()).isEncryptedVolumes();
    }

    public String getVolumeEncryptionKey(Group group, boolean encryptedVolume) {
        AwsInstanceView awsInstanceView = new AwsInstanceView(group.getReferenceInstanceTemplate());
        return encryptedVolume && awsInstanceView.isKmsCustom() ? awsInstanceView.getKmsKey() : null;
    }

    public TagSpecification getTagSpecification(CloudStack cloudStack) {
        return TagSpecification.builder()
                .resourceType(software.amazon.awssdk.services.ec2.model.ResourceType.VOLUME)
                .tags(awsTaggingService.prepareEc2Tags(cloudStack.getTags()))
                .build();
    }

    public TagSpecification addAdditionalTags(Map<String, String> tags, TagSpecification tagSpecification) {
        List<Tag> currentTags = Lists.newArrayList();
        currentTags.addAll(tagSpecification.tags());
        currentTags.addAll(awsTaggingService.prepareEc2Tags(tags));
        return TagSpecification.builder()
                .resourceType(software.amazon.awssdk.services.ec2.model.ResourceType.VOLUME)
                .tags(currentTags)
                .build();
    }

    public CreateVolumeRequest createVolumeRequest(VolumeSetAttributes.Volume volume, TagSpecification tagSpecification, String volumeEncryptionKey,
            boolean encryptedVolume, String availabilityZone) {
        return CreateVolumeRequest.builder()
            .availabilityZone(availabilityZone)
            .size(volume.getSize())
            .snapshotId(null)
            .tagSpecifications(tagSpecification)
            .volumeType(volume.getType())
            .iops(awsVolumeIopsCalculator.getIops(volume.getType(), volume.getSize()))
            .throughput(awsVolumeThroughputCalculator.getThroughput(volume.getType(), volume.getSize()))
            .encrypted(encryptedVolume)
            .kmsKeyId(volumeEncryptionKey)
            .build();
    }
}