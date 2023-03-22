package com.sequenceiq.cloudbreak.cloud.aws;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;

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

    public List<LaunchTemplateBlockDeviceMappingRequest> createResizedRootBlockDeviceMapping(AmazonEc2Client ec2Client,
            Map<LaunchTemplateField, String> updatableFields, LaunchTemplateSpecification launchTemplateSpecification, CloudStack cloudStack) {
        if (updatableFields.containsKey(LaunchTemplateField.ROOT_DISK)) {
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
            LOGGER.info("No {} found in updatableFields, skipping resizing root disk", LaunchTemplateField.ROOT_DISK);
            return List.of();
        }
    }

    private List<LaunchTemplateBlockDeviceMappingRequest> createBlockDeviceMappingBasedOnDefaultLaunchTemplate(AmazonEc2Client ec2Client,
            Map<LaunchTemplateField, String> updatableFields, CloudStack cloudStack, LaunchTemplateVersion defaultLaunchTemplate) {
        String rootDeviceName = volumeBuilderUtil.getRootDeviceName(cloudStack, ec2Client);
        List<LaunchTemplateBlockDeviceMapping> blockDeviceMappings = defaultLaunchTemplate.launchTemplateData().hasBlockDeviceMappings()
                ? new ArrayList<>(defaultLaunchTemplate.launchTemplateData().blockDeviceMappings())
                : new ArrayList<>();
        Optional<LaunchTemplateBlockDeviceMapping> rootBlockDeviceMapping = blockDeviceMappings.stream()
                .filter(blockDeviceMapping -> blockDeviceMapping.deviceName().equals(rootDeviceName))
                .findFirst();
        LOGGER.info("rootBlockDeviceMapping {} for rootDeviceName [{}} in LaunchTemplateVersion {}.",
                rootBlockDeviceMapping, rootDeviceName, defaultLaunchTemplate);
        if (rootBlockDeviceMapping.isPresent()
                && !Integer.valueOf(updatableFields.get(LaunchTemplateField.ROOT_DISK)).equals(rootBlockDeviceMapping.get().ebs().volumeSize())) {
            return constructBlockDeviceMappingRequestWithResizedRootDisk(updatableFields, blockDeviceMappings, rootBlockDeviceMapping.get());
        } else {
            LOGGER.info("Root disk resize not required, as either root disk not found, or the size is the same. {} and required new size [{}]",
                    rootBlockDeviceMapping, updatableFields.get(LaunchTemplateField.ROOT_DISK));
            return List.of();
        }
    }

    private List<LaunchTemplateBlockDeviceMappingRequest> constructBlockDeviceMappingRequestWithResizedRootDisk(
            Map<LaunchTemplateField, String> updatableFields, List<LaunchTemplateBlockDeviceMapping> blockDeviceMappings,
            LaunchTemplateBlockDeviceMapping originalRootDeviceMapping) {
        LOGGER.debug("Creating LaunchTemplateBlockDeviceMapping with new root disk size: [{}]",
                updatableFields.get(LaunchTemplateField.ROOT_DISK));
        LaunchTemplateEbsBlockDevice rootEbsBlockDevice = originalRootDeviceMapping.ebs().toBuilder()
                .volumeSize(Integer.valueOf(updatableFields.get(LaunchTemplateField.ROOT_DISK)))
                .build();
        LaunchTemplateBlockDeviceMapping updatedRootBlockDeviceMapping = originalRootDeviceMapping.toBuilder()
                .ebs(rootEbsBlockDevice)
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
        if (oldLaunchConfiguration.hasBlockDeviceMappings() && updatableFields.containsKey(LaunchTemplateField.ROOT_DISK)) {
            List<BlockDeviceMapping> blockDeviceMappings = new ArrayList<>(oldLaunchConfiguration.blockDeviceMappings());
            LOGGER.debug("Original block device mapping: {}", blockDeviceMappings);
            String rootDeviceName = volumeBuilderUtil.getRootDeviceName(ac, cloudStack);
            Optional<BlockDeviceMapping> rootDeviceMapping = blockDeviceMappings.stream()
                    .filter(blockDeviceMapping -> blockDeviceMapping.deviceName().equals(rootDeviceName))
                    .findFirst();
            LOGGER.debug("Root device mapping for [{}]: {}", rootDeviceName, rootDeviceMapping);
            if (rootDeviceMapping.isPresent()
                    && !Integer.valueOf(updatableFields.get(LaunchTemplateField.ROOT_DISK)).equals(rootDeviceMapping.get().ebs().volumeSize())) {
                return constructBlockDeviceMappingRequestWithResizedRootDisk(updatableFields, blockDeviceMappings, rootDeviceMapping.get());
            } else {
                LOGGER.debug("Root device mapping is missing, or the requested size [{}] is the same as the current",
                        updatableFields.get(LaunchTemplateField.ROOT_DISK));
                return Optional.empty();
            }
        } else {
            return Optional.empty();
        }
    }

    private Optional<List<BlockDeviceMapping>> constructBlockDeviceMappingRequestWithResizedRootDisk(Map<LaunchTemplateField, String> updatableFields,
            List<BlockDeviceMapping> blockDeviceMappings, BlockDeviceMapping sourceDeviceMapping) {
        LOGGER.debug("Original size [{}] and requested [{}] is different",
                sourceDeviceMapping.ebs().volumeSize(), updatableFields.get(LaunchTemplateField.ROOT_DISK));
        BlockDeviceMapping updateRootDeviceMapping = sourceDeviceMapping.toBuilder()
                .ebs(sourceDeviceMapping.ebs().toBuilder()
                        .volumeSize(Integer.valueOf(updatableFields.get(LaunchTemplateField.ROOT_DISK)))
                        .build())
                .build();
        blockDeviceMappings.remove(sourceDeviceMapping);
        blockDeviceMappings.add(updateRootDeviceMapping);
        LOGGER.info("Using updated block device mapping: {}", blockDeviceMappings);
        return Optional.of(blockDeviceMappings);
    }
}
