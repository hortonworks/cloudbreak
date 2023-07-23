package com.sequenceiq.cloudbreak.cloud.aws.common.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import com.google.api.client.util.Lists;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsTaggingService;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.aws.common.resource.volume.AwsVolumeIopsCalculator;
import com.sequenceiq.cloudbreak.cloud.aws.common.resource.volume.AwsVolumeThroughputCalculator;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AuthenticatedContextView;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsInstanceView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes.Volume;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.util.DeviceNameGenerator;

import software.amazon.awssdk.services.ec2.model.AttachVolumeRequest;
import software.amazon.awssdk.services.ec2.model.AttachVolumeResponse;
import software.amazon.awssdk.services.ec2.model.CreateVolumeRequest;
import software.amazon.awssdk.services.ec2.model.CreateVolumeResponse;
import software.amazon.awssdk.services.ec2.model.TagSpecification;

public class AwsCommonDiskUpdateService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsCommonDiskUpdateService.class);

    private static final String DEVICE_NAME_TEMPLATE = "/dev/xvd%s";

    @Inject
    private AwsTaggingService awsTaggingService;

    @Inject
    private AwsVolumeIopsCalculator awsVolumeIopsCalculator;

    @Inject
    private AwsVolumeThroughputCalculator awsVolumeThroughputCalculator;

    public List<CloudResource> createAndAttachVolumes(AuthenticatedContext authenticatedContext, Group group, Volume volumeRequest,
            CloudStack cloudStack, int volToAddPerInstance, List<CloudResource> cloudResources) throws CloudbreakServiceException {
        LOGGER.debug("Creating additional EBS volumes with : {} for group: {}", volumeRequest, group.getName());
        boolean encryptedVolume = isEncryptedVolumeRequested(group);
        String volumeEncryptionKey = getVolumeEncryptionKey(group, encryptedVolume);
        TagSpecification tagSpecification = getTagSpecification(cloudStack);
        AmazonEc2Client client = getEc2Client(authenticatedContext);
        int attachedVolumesCount = group.getReferenceInstanceTemplate().getVolumes().size();
        String availabilityZone = group.getInstances().get(0).getAvailabilityZone();
        Map<String, List<Volume>> volumeSetMap = new HashMap<>();
        for(CloudResource resource: cloudResources) {
            volumeSetMap.put(resource.getName(), Lists.newArrayList());
            DeviceNameGenerator generator = new DeviceNameGenerator(DEVICE_NAME_TEMPLATE, attachedVolumesCount);
            while (volToAddPerInstance-- > 0) {
                try {
                    CreateVolumeResponse createResponse = createVolume(volumeRequest, tagSpecification, volumeEncryptionKey, encryptedVolume,
                            availabilityZone, client);
                    LOGGER.debug("Response for create volume : {}", createResponse);
                    String deviceName = generator.next();
                    AttachVolumeResponse attachResponse = attachVolume(resource.getInstanceId(), createResponse.volumeId(), deviceName, client);
                    LOGGER.debug("Response for attach volume : {}", attachResponse);
                    Volume volume = new Volume(createResponse.volumeId(), deviceName, volumeRequest.getSize(), volumeRequest.getType(),
                            volumeRequest.getCloudVolumeUsageType());
                    volumeSetMap.get(resource.getName()).add(volume);
                } catch (Exception ex) {
                    LOGGER.error("Error while creating and attaching disks to the instance: {}, exception: {}",
                            resource.getInstanceId(), ex.getMessage());
                    throw new CloudbreakServiceException(ex);
                }
            }
        }
        List<CloudResource> createdResources = cloudResources.stream()
            .peek(resource -> {
                List<Volume> volumes = volumeSetMap.get(resource.getName());
                if (!CollectionUtils.isEmpty(volumes)) {
                    resource.getParameter(CloudResource.ATTRIBUTES, VolumeSetAttributes.class).getVolumes().addAll(volumes);
                }
            })
            .collect(Collectors.toList());
        LOGGER.debug("Created resources with additional volumes: {}", createdResources);
        return createdResources;
    }

    protected CreateVolumeResponse createVolume(Volume volume, TagSpecification tagSpecification, String volumeEncryptionKey,
            boolean encryptedVolume, String availabilityZone, AmazonEc2Client client) {
        CreateVolumeRequest createVolumeRequest =  CreateVolumeRequest.builder()
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
        LOGGER.debug("Sending create volume request : {}", createVolumeRequest);
        return client.createVolume(createVolumeRequest);
    }

    protected AttachVolumeResponse attachVolume(String instanceId, String volumeId, String deviceName, AmazonEc2Client client) {
        AttachVolumeRequest attachVolumeRequest = AttachVolumeRequest.builder().instanceId(instanceId)
                .volumeId(volumeId).device(deviceName).build();
        LOGGER.debug("Sending attach volume request : {}", attachVolumeRequest);
        return client.attachVolume(attachVolumeRequest);
    }

    protected boolean isEncryptedVolumeRequested(Group group) {
        return new AwsInstanceView(group.getReferenceInstanceTemplate()).isEncryptedVolumes();
    }

    protected String getVolumeEncryptionKey(Group group, boolean encryptedVolume) {
        AwsInstanceView awsInstanceView = new AwsInstanceView(group.getReferenceInstanceTemplate());
        return encryptedVolume && awsInstanceView.isKmsCustom() ? awsInstanceView.getKmsKey() : null;
    }

    protected TagSpecification getTagSpecification(CloudStack cloudStack) {
        return TagSpecification.builder()
                .resourceType(software.amazon.awssdk.services.ec2.model.ResourceType.VOLUME)
                .tags(awsTaggingService.prepareEc2Tags(cloudStack.getTags()))
                .build();
    }

    protected AmazonEc2Client getEc2Client(AuthenticatedContext authenticatedContext) {
        return new AuthenticatedContextView(authenticatedContext).getAmazonEC2Client();
    }
}
