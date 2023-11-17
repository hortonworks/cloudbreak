package com.sequenceiq.cloudbreak.cloud.aws.common.resource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.aws.common.resource.volume.AwsVolumeIopsCalculator;
import com.sequenceiq.cloudbreak.cloud.aws.common.resource.volume.AwsVolumeThroughputCalculator;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AuthenticatedContextView;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsInstanceView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.Volume;
import com.sequenceiq.common.model.AwsDiskType;

import software.amazon.awssdk.services.ec2.model.BlockDeviceMapping;
import software.amazon.awssdk.services.ec2.model.DescribeImagesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeImagesResponse;
import software.amazon.awssdk.services.ec2.model.EbsBlockDevice;
import software.amazon.awssdk.services.ec2.model.Image;

@Component
public class VolumeBuilderUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(VolumeBuilderUtil.class);

    @Inject
    private AwsVolumeIopsCalculator awsVolumeIopsCalculator;

    @Inject
    private AwsVolumeThroughputCalculator awsVolumeThroughputCalculator;

    @Inject
    private EntitlementService entitlementService;

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
                .ebs(getRootEbs(awsInstanceView, group, ac.getCloudCredential().getAccountId()))
                .build();
    }

    public EbsBlockDevice getRootEbs(AwsInstanceView awsInstanceView, Group group, String accountId) {
        String volumeType = AwsDiskType.Gp3.value();
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
}
