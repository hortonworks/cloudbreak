package com.sequenceiq.cloudbreak.cloud.aws.common.resource;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.amazonaws.services.ec2.model.BlockDeviceMapping;
import com.amazonaws.services.ec2.model.DescribeImagesRequest;
import com.amazonaws.services.ec2.model.DescribeImagesResult;
import com.amazonaws.services.ec2.model.EbsBlockDevice;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AuthenticatedContextView;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsInstanceView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.Volume;

@Component
public class VolumeBuilderUtil {

    public BlockDeviceMapping getEphemeral(AwsInstanceView awsInstanceView) {
        Map<String, Long> volumes = awsInstanceView.getVolumes().stream().collect(Collectors.groupingBy(Volume::getType, Collectors.counting()));
        Long ephemeralCount = volumes.getOrDefault("ephemeral", 0L);
        BlockDeviceMapping ephemeral = null;
        if (ephemeralCount != 0) {
            List<String> seq = List.of("b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x",
                    "y", "z");
            int xIndex = 0;
            while (xIndex < seq.size() && ephemeral == null) {
                // we need to decrease the ephemeralCount, otherwise the 0 index will be never selected
                if (xIndex == ephemeralCount - 1) {
                    String x = seq.get(xIndex);
                    ephemeral = new BlockDeviceMapping()
                            .withDeviceName("/dev/xvd" + x)
                            .withVirtualName("ephemeral" + xIndex);
                }
                xIndex++;
            }
        }
        return ephemeral;
    }

    public BlockDeviceMapping getRootVolume(AwsInstanceView awsInstanceView, Group group, CloudStack cloudStack, AuthenticatedContext ac) {
        return new BlockDeviceMapping()
                .withDeviceName(getRootDeviceName(ac, cloudStack))
                .withEbs(getEbs(awsInstanceView, group));
    }

    public EbsBlockDevice getEbs(AwsInstanceView awsInstanceView, Group group) {
        EbsBlockDevice ebsBlockDevice = new EbsBlockDevice()
                .withDeleteOnTermination(true)
                .withVolumeType("gp2")
                .withVolumeSize(group.getRootVolumeSize());

        if (awsInstanceView.isEncryptedVolumes()) {
            ebsBlockDevice.withEncrypted(true);
        }

        if (awsInstanceView.isKmsCustom()) {
            ebsBlockDevice.withKmsKeyId(awsInstanceView.getKmsKey());
        }
        return ebsBlockDevice;
    }

    public String getRootDeviceName(AuthenticatedContext ac, CloudStack cloudStack) {
        AmazonEc2Client ec2Client = new AuthenticatedContextView(ac).getAmazonEC2Client();
        DescribeImagesResult images = ec2Client.describeImages(new DescribeImagesRequest().withImageIds(cloudStack.getImage().getImageName()));
        if (images.getImages().isEmpty()) {
            throw new CloudConnectorException(String.format("AMI is not available: '%s'.", cloudStack.getImage().getImageName()));
        }
        com.amazonaws.services.ec2.model.Image image = images.getImages().get(0);
        if (image == null) {
            throw new CloudConnectorException(String.format("Couldn't describe AMI '%s'.", cloudStack.getImage().getImageName()));
        }
        return image.getRootDeviceName();
    }
}
