package com.sequenceiq.cloudbreak.cloud.aws.common.resource;

import static java.lang.String.format;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.aws.common.CommonAwsClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.aws.common.resource.volume.AwsVolumeIopsCalculator;
import com.sequenceiq.cloudbreak.cloud.aws.common.resource.volume.AwsVolumeThroughputCalculator;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AuthenticatedContextView;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsInstanceView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.CloudVolumeUsageType;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.Volume;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.common.model.AwsDiskType;

import software.amazon.awssdk.services.ec2.model.BlockDeviceMapping;
import software.amazon.awssdk.services.ec2.model.DescribeImagesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeImagesResponse;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesResponse;
import software.amazon.awssdk.services.ec2.model.DescribeVolumesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeVolumesResponse;
import software.amazon.awssdk.services.ec2.model.EbsBlockDevice;
import software.amazon.awssdk.services.ec2.model.Image;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.InstanceBlockDeviceMapping;

@Component
public class VolumeBuilderUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(VolumeBuilderUtil.class);

    @Inject
    private AwsVolumeIopsCalculator awsVolumeIopsCalculator;

    @Inject
    private AwsVolumeThroughputCalculator awsVolumeThroughputCalculator;

    @Inject
    private CommonAwsClient awsClient;

    public List<BlockDeviceMapping> getEphemeral(AwsInstanceView awsInstanceView) {
        Long ephemeralCount = getEphemeralCount(awsInstanceView);
        List<BlockDeviceMapping> ephemeralBlockDeviceMappings = new ArrayList<>();
        if (ephemeralCount != 0) {
            List<String> seq = List.of("b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x",
                    "y", "z");
            int xIndex = 0;
            while (xIndex < seq.size() && ephemeralBlockDeviceMappings.size() < ephemeralCount) {
                String blockDeviceNameIndex = seq.get(xIndex);
                ephemeralBlockDeviceMappings.add(BlockDeviceMapping.builder()
                        .deviceName("/dev/xvd" + blockDeviceNameIndex)
                        .virtualName("ephemeral" + xIndex)
                        .build());
                xIndex++;
            }
        }
        return ephemeralBlockDeviceMappings;
    }

    private Long getEphemeralCount(AwsInstanceView awsInstanceView) {
        Map<String, Long> volumes = awsInstanceView.getVolumes().stream().collect(Collectors.groupingBy(Volume::getType, Collectors.counting()));
        Long ephemeralCount = volumes.getOrDefault("ephemeral", 0L);
        if (ephemeralCount.equals(0L)) {
            return Optional.ofNullable(awsInstanceView.getTemporaryStorageCount()).orElse(0L);
        } else {
            return ephemeralCount;
        }
    }

    public BlockDeviceMapping getRootVolume(AwsInstanceView awsInstanceView, Group group, CloudStack cloudStack, AuthenticatedContext ac) {
        return BlockDeviceMapping.builder()
                .deviceName(getRootDeviceName(ac, cloudStack))
                .ebs(getRootEbs(awsInstanceView, group))
                .build();
    }

    public EbsBlockDevice getRootEbs(AwsInstanceView awsInstanceView, Group group) {
        String volumeType = group.getRootVolumeType() != null ? group.getRootVolumeType().toLowerCase(Locale.ROOT) : AwsDiskType.Gp3.value();
        int rootVolumeSize = group.getRootVolumeSize();

        LOGGER.debug("AwsInstanceView: {},  root volume type: {}, size: {}", awsInstanceView, volumeType, rootVolumeSize);

        EbsBlockDevice.Builder ebsBlockDeviceBuilder = EbsBlockDevice.builder()
                .deleteOnTermination(true)
                .volumeType(volumeType)
                .iops(awsVolumeIopsCalculator.getIops(volumeType, rootVolumeSize))
                .throughput(awsVolumeThroughputCalculator.getThroughput(volumeType, rootVolumeSize))
                .volumeSize(rootVolumeSize);

        if (awsInstanceView.isEncryptedVolumes()) {
            ebsBlockDeviceBuilder.encrypted(true);
        }

        if (awsInstanceView.isKmsCustom()) {
            ebsBlockDeviceBuilder.kmsKeyId(awsInstanceView.getKmsKey());
        }
        return ebsBlockDeviceBuilder.build();
    }

    public String getRootDeviceName(AuthenticatedContext ac, CloudStack cloudStack) {
        AmazonEc2Client ec2Client = new AuthenticatedContextView(ac).getAmazonEC2Client();
        return getRootDeviceName(cloudStack.getImage().getImageName(), ec2Client);
    }

    public String getRootDeviceName(String imageName, AmazonEc2Client ec2Client) {
        DescribeImagesResponse images = ec2Client.describeImages(DescribeImagesRequest.builder().imageIds(imageName).build());
        if (images.images().isEmpty()) {
            throw new CloudConnectorException(String.format("AMI is not available: '%s'.", imageName));
        }
        Image image = images.images().get(0);
        if (image == null) {
            throw new CloudConnectorException(String.format("Couldn't describe AMI '%s'.", imageName));
        }
        return image.rootDeviceName();
    }

    public CloudResource createRootVolumeResource(String resourceName, String groupName, ResourceType resourceType, String availabilityZone) {
        CloudResource rootDiskResource = CloudResource.builder()
                .withGroup(groupName)
                .withName(resourceName)
                .withType(resourceType)
                .withStatus(CommonStatus.REQUESTED)
                .withAvailabilityZone(availabilityZone)
                .withPersistent(true)
                .withParameters(new HashMap<>())
                .build();
        return rootDiskResource;
    }

    public List<Instance> describeInstancesByInstanceIds(List<String> instanceIds, AuthenticatedContext auth) {
        AmazonEc2Client ec2Client = awsClient.createEc2Client(auth);
        DescribeInstancesResponse describeInstancesResponse = ec2Client.describeInstances(DescribeInstancesRequest.builder().instanceIds(instanceIds).build());
        return describeInstancesResponse.reservations().stream().flatMap(res -> res.instances().stream()).collect(Collectors.toList());
    }

    public List<String> getRootVolumeIdsFromInstances(List<Instance> instances) {
        return instances.stream()
                .map(this::getRootVolumeId)
                .filter(Optional::isPresent)
                .map(blockDeviceMapping -> blockDeviceMapping.get().ebs().volumeId())
                .collect(Collectors.toList());
    }

    private Optional<InstanceBlockDeviceMapping> getRootVolumeId(software.amazon.awssdk.services.ec2.model.Instance instance) {
        return instance.blockDeviceMappings().stream().filter(mapping -> mapping.deviceName().equals(instance.rootDeviceName())).findFirst();
    }

    public List<CloudResource> updateRootVolumeResource(List<CloudResource> resources, List<String> rootVolumeIds, AuthenticatedContext auth) {
        AmazonEc2Client ec2Client = awsClient.createEc2Client(auth);
        DescribeVolumesRequest describeVolumesRequest = DescribeVolumesRequest.builder().volumeIds(rootVolumeIds).build();
        try {
            DescribeVolumesResponse volumesResponse = ec2Client.describeVolumes(describeVolumesRequest);
            if (volumesResponse.hasVolumes()) {
                return volumesResponse.volumes().stream().map(
                        (vol) -> {
                            CloudResource rootDiskResource = resources.getFirst();
                            String instanceId = vol.attachments().getFirst().instanceId();
                            String deviceName = vol.attachments().getFirst().device();
                            rootDiskResource.setStatus(CommonStatus.CREATED);
                            rootDiskResource.setInstanceId(instanceId);
                            VolumeSetAttributes attributes = new VolumeSetAttributes.Builder()
                                    .withAvailabilityZone(vol.availabilityZone())
                                    .withVolumes(List.of(new VolumeSetAttributes.Volume(vol.volumeId(), deviceName, vol.size(),
                                            vol.volumeTypeAsString(), CloudVolumeUsageType.GENERAL)))
                                    .withDeleteOnTermination(Boolean.TRUE)
                                    .build();
                            rootDiskResource.putParameter(CloudResource.ATTRIBUTES, attributes);
                            return rootDiskResource;
                        }).toList();
            }
        } catch (Exception ex) {
            String exceptionMessage = format("Exception while querying describe root volumes for volume - %s. " +
                    "returning empty list. Exception - %s", rootVolumeIds, ex.getMessage());
            LOGGER.warn("{} This should not prevent instance creation.", exceptionMessage);
        }
        return new ArrayList<>();
    }
}
