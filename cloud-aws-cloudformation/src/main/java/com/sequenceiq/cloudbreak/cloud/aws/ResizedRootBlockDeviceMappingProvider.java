package com.sequenceiq.cloudbreak.cloud.aws;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.aws.common.resource.VolumeBuilderUtil;
import com.sequenceiq.cloudbreak.cloud.aws.mapper.LaunchTemplateBlockDeviceMappingToRequestConverter;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;

import software.amazon.awssdk.services.autoscaling.model.BlockDeviceMapping;
import software.amazon.awssdk.services.autoscaling.model.LaunchConfiguration;
import software.amazon.awssdk.services.autoscaling.model.LaunchTemplateSpecification;
import software.amazon.awssdk.services.ec2.model.DescribeLaunchTemplateVersionsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeLaunchTemplateVersionsResponse;
import software.amazon.awssdk.services.ec2.model.LaunchTemplateBlockDeviceMapping;
import software.amazon.awssdk.services.ec2.model.LaunchTemplateBlockDeviceMappingRequest;
import software.amazon.awssdk.services.ec2.model.LaunchTemplateEbsBlockDevice;
import software.amazon.awssdk.services.ec2.model.LaunchTemplateVersion;

@Component
public class ResizedRootBlockDeviceMappingProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResizedRootBlockDeviceMappingProvider.class);

    @Inject
    private VolumeBuilderUtil volumeBuilderUtil;

    @Inject
    private LaunchTemplateBlockDeviceMappingToRequestConverter blockDeviceMappingConverter;

    public List<LaunchTemplateBlockDeviceMappingRequest> createUpdatedRootBlockDeviceMapping(AmazonEc2Client ec2Client,
            Map<LaunchTemplateField, String> updatableFields, LaunchTemplateSpecification launchTemplateSpecification, CloudStack cloudStack) {
        if (updatableFields.containsKey(LaunchTemplateField.ROOT_DISK_SIZE) || updatableFields.containsKey(LaunchTemplateField.ROOT_DISK_PATH)
            || updatableFields.containsKey(LaunchTemplateField.ROOT_VOLUME_TYPE)) {
            try {
                DescribeLaunchTemplateVersionsResponse describeLaunchTemplateVersionsResponse =
                        ec2Client.describeLaunchTemplateVersions(DescribeLaunchTemplateVersionsRequest.builder()
                                .launchTemplateId(launchTemplateSpecification.launchTemplateId()).build());
                if (describeLaunchTemplateVersionsResponse.hasLaunchTemplateVersions()) {
                    Optional<LaunchTemplateVersion> defaultLaunchTemplate = describeLaunchTemplateVersionsResponse.launchTemplateVersions().stream()
                            .filter(LaunchTemplateVersion::defaultVersion)
                            .findFirst();
                    if (defaultLaunchTemplate.isPresent()) {
                        return createBlockDeviceMappingBasedOnDefaultLaunchTemplate(ec2Client, updatableFields, cloudStack, defaultLaunchTemplate.get());
                    } else {
                        LOGGER.warn("'describeLaunchTemplateVersions' doesn't have any default template: {}", describeLaunchTemplateVersionsResponse);
                        return List.of();
                    }
                } else {
                    LOGGER.warn("'describeLaunchTemplateVersions' returned with empty result, skipping resizing root disk. Response: {}",
                            describeLaunchTemplateVersionsResponse);
                    return List.of();
                }
            } catch (Exception e) {
                LOGGER.error("Couldn't fetch LaunchTemplateVersions, skipping updating root disk size", e);
                return List.of();
            }
        } else {
            LOGGER.info("No {} found in updatableFields, skipping resizing root disk", LaunchTemplateField.ROOT_DISK_SIZE);
            return List.of();
        }
    }

    private List<LaunchTemplateBlockDeviceMappingRequest> createBlockDeviceMappingBasedOnDefaultLaunchTemplate(AmazonEc2Client ec2Client,
            Map<LaunchTemplateField, String> updatableFields, CloudStack cloudStack, LaunchTemplateVersion defaultLaunchTemplate) {
        String newRootDeviceName = volumeBuilderUtil.getRootDeviceName(cloudStack.getImage().getImageName(), ec2Client);
        String originalRootDeviceName = getOriginalRootDeviceNameFromAmi(ec2Client, defaultLaunchTemplate);
        if (updatableFields.containsKey(LaunchTemplateField.ROOT_DISK_PATH) && !newRootDeviceName.equals(originalRootDeviceName)) {
            LOGGER.info("RootDevice name difference detected. originalRootDeviceName: [{}] newRootDeviceName: [{}]. " +
                    "rootBlockDeviceMapping needs to be updated to [{}]", originalRootDeviceName, newRootDeviceName, newRootDeviceName);
            updatableFields.put(LaunchTemplateField.ROOT_DISK_PATH, newRootDeviceName);
        }
        List<LaunchTemplateBlockDeviceMapping> blockDeviceMappings = defaultLaunchTemplate.launchTemplateData().hasBlockDeviceMappings()
                ? new ArrayList<>(defaultLaunchTemplate.launchTemplateData().blockDeviceMappings())
                : new ArrayList<>();
        Optional<LaunchTemplateBlockDeviceMapping> rootBlockDeviceMapping = blockDeviceMappings.stream()
                .filter(blockDeviceMapping -> blockDeviceMapping.deviceName().equals(newRootDeviceName)
                        || blockDeviceMapping.deviceName().equals(originalRootDeviceName))
                .findFirst();
        LOGGER.info("rootBlockDeviceMapping {} for rootDeviceName [{}} in LaunchTemplateVersion {}.",
                rootBlockDeviceMapping, newRootDeviceName, defaultLaunchTemplate);
        if (rootBlockDeviceMapping.isPresent() && isUpdateRequired(updatableFields, rootBlockDeviceMapping.get())) {
            return constructBlockDeviceMappingRequestWithResizedRootDisk(updatableFields, blockDeviceMappings, rootBlockDeviceMapping.get());
        } else {
            LOGGER.info("Root disk update not required, as either root disk not found, or the size and path is the same. {}",
                    rootBlockDeviceMapping);
            return List.of();
        }
    }

    private boolean isUpdateRequired(Map<LaunchTemplateField, String> updatableFields, LaunchTemplateBlockDeviceMapping originalRootDeviceMapping) {
        boolean rootDiskSizeDifferent = rootVolumeSizeUpdateRequired(updatableFields, originalRootDeviceMapping);
        boolean rootDiskMountPointChanged = isNotBlank(updatableFields.get(LaunchTemplateField.ROOT_DISK_PATH));
        boolean rootVolumeTypeDifferent = rootVolumeTypeUpdateRequired(updatableFields, originalRootDeviceMapping);
        return rootDiskSizeDifferent || rootDiskMountPointChanged || rootVolumeTypeDifferent;
    }

    private String getOriginalRootDeviceNameFromAmi(AmazonEc2Client ec2Client, LaunchTemplateVersion launchTemplate) {
        return volumeBuilderUtil.getRootDeviceName(launchTemplate.launchTemplateData().imageId(), ec2Client);
    }

    private List<LaunchTemplateBlockDeviceMappingRequest> constructBlockDeviceMappingRequestWithResizedRootDisk(
            Map<LaunchTemplateField, String> updatableFields, List<LaunchTemplateBlockDeviceMapping> blockDeviceMappings,
            LaunchTemplateBlockDeviceMapping originalRootDeviceMapping) {

        LaunchTemplateEbsBlockDevice.Builder rootEbsBlockDeviceBuilder = originalRootDeviceMapping.ebs().toBuilder();
        if (rootVolumeSizeUpdateRequired(updatableFields, originalRootDeviceMapping)) {
            LOGGER.debug("Creating LaunchTemplateBlockDeviceMapping with new root disk size: [{}]",
                    updatableFields.get(LaunchTemplateField.ROOT_DISK_SIZE));
            rootEbsBlockDeviceBuilder.volumeSize(Integer.valueOf(updatableFields.get(LaunchTemplateField.ROOT_DISK_SIZE)));
        }

        if (rootVolumeTypeUpdateRequired(updatableFields, originalRootDeviceMapping)) {
            LOGGER.debug("Creating LaunchTemplateBlockDeviceMapping with new root disk volume type: [{}]",
                    updatableFields.get(LaunchTemplateField.ROOT_VOLUME_TYPE));
            rootEbsBlockDeviceBuilder.volumeType(updatableFields.get(LaunchTemplateField.ROOT_VOLUME_TYPE));
        }

        LaunchTemplateBlockDeviceMapping.Builder updatedRootBlockDeviceMappingBuilder = originalRootDeviceMapping.toBuilder();
        if (updatableFields.containsKey(LaunchTemplateField.ROOT_DISK_PATH)
                && isNotBlank(updatableFields.get(LaunchTemplateField.ROOT_DISK_PATH))) {
            LOGGER.debug("Updating LaunchTemplateBlockDeviceMapping with image matching rootDeviceName: [{}]",
                    updatableFields.get(LaunchTemplateField.ROOT_DISK_PATH));
            updatedRootBlockDeviceMappingBuilder.deviceName(updatableFields.get(LaunchTemplateField.ROOT_DISK_PATH));
        }

        LaunchTemplateBlockDeviceMapping updatedRootBlockDeviceMapping = updatedRootBlockDeviceMappingBuilder
                .ebs(rootEbsBlockDeviceBuilder.build())
                .build();
        LOGGER.debug("Replacing old {} with new {} device mapping", originalRootDeviceMapping, updatedRootBlockDeviceMapping);
        blockDeviceMappings.remove(originalRootDeviceMapping);
        blockDeviceMappings.add(updatedRootBlockDeviceMapping);
        LOGGER.info("New block device mappings: {}", blockDeviceMappings);
        return blockDeviceMappings.stream()
                .map(blockDeviceMappingConverter::convert)
                .collect(Collectors.toList());
    }

    public Optional<List<BlockDeviceMapping>> createBlockDeviceMappingIfRootDiskResizeRequired(AuthenticatedContext ac, CloudStack cloudStack,
            Map<LaunchTemplateField, String> updatableFields, LaunchConfiguration oldLaunchConfiguration) {
        if (oldLaunchConfiguration.hasBlockDeviceMappings() && updatableFields.containsKey(LaunchTemplateField.ROOT_DISK_SIZE)) {
            List<BlockDeviceMapping> blockDeviceMappings = new ArrayList<>(oldLaunchConfiguration.blockDeviceMappings());
            LOGGER.debug("Original block device mapping: {}", blockDeviceMappings);
            String rootDeviceName = volumeBuilderUtil.getRootDeviceName(ac, cloudStack);
            Optional<BlockDeviceMapping> rootDeviceMapping = blockDeviceMappings.stream()
                    .filter(blockDeviceMapping -> blockDeviceMapping.deviceName().equals(rootDeviceName))
                    .findFirst();
            LOGGER.debug("Root device mapping for [{}]: {}", rootDeviceName, rootDeviceMapping);
            if (rootDeviceMapping.isPresent()
                    && !Integer.valueOf(updatableFields.get(LaunchTemplateField.ROOT_DISK_SIZE)).equals(rootDeviceMapping.get().ebs().volumeSize())) {
                return constructBlockDeviceMappingRequestWithResizedRootDisk(updatableFields, blockDeviceMappings, rootDeviceMapping.get());
            } else {
                LOGGER.debug("Root device mapping is missing, or the requested size [{}] is the same as the current",
                        updatableFields.get(LaunchTemplateField.ROOT_DISK_SIZE));
                return Optional.empty();
            }
        } else {
            return Optional.empty();
        }
    }

    private Optional<List<BlockDeviceMapping>> constructBlockDeviceMappingRequestWithResizedRootDisk(Map<LaunchTemplateField, String> updatableFields,
            List<BlockDeviceMapping> blockDeviceMappings, BlockDeviceMapping sourceDeviceMapping) {
        LOGGER.debug("Original size [{}] and requested [{}] is different",
                sourceDeviceMapping.ebs().volumeSize(), updatableFields.get(LaunchTemplateField.ROOT_DISK_SIZE));
        BlockDeviceMapping updateRootDeviceMapping = sourceDeviceMapping.toBuilder()
                .ebs(sourceDeviceMapping.ebs().toBuilder()
                        .volumeSize(Integer.valueOf(updatableFields.get(LaunchTemplateField.ROOT_DISK_SIZE)))
                        .build())
                .build();
        blockDeviceMappings.remove(sourceDeviceMapping);
        blockDeviceMappings.add(updateRootDeviceMapping);
        LOGGER.info("Using updated block device mapping: {}", blockDeviceMappings);
        return Optional.of(blockDeviceMappings);
    }

    private boolean rootVolumeTypeUpdateRequired(Map<LaunchTemplateField, String> updatableFields,
            LaunchTemplateBlockDeviceMapping originalRootDeviceMapping) {
        return updatableFields.containsKey(LaunchTemplateField.ROOT_VOLUME_TYPE)
                && !updatableFields.get(LaunchTemplateField.ROOT_VOLUME_TYPE).equals(originalRootDeviceMapping.ebs().volumeType());
    }

    private boolean rootVolumeSizeUpdateRequired(Map<LaunchTemplateField, String> updatableFields,
            LaunchTemplateBlockDeviceMapping originalRootDeviceMapping) {
        return updatableFields.containsKey(LaunchTemplateField.ROOT_DISK_SIZE)
                && !Integer.valueOf(updatableFields.get(LaunchTemplateField.ROOT_DISK_SIZE)).equals(originalRootDeviceMapping.ebs().volumeSize());
    }
}
