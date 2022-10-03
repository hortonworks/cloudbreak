package com.sequenceiq.cloudbreak.cloud.aws.common.connector.resource;

import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.amazonaws.services.ebs.model.AmazonEBSException;
import com.amazonaws.services.ec2.model.DescribeVolumesRequest;
import com.amazonaws.services.ec2.model.DescribeVolumesResult;
import com.amazonaws.services.ec2.model.Volume;
import com.sequenceiq.cloudbreak.cloud.aws.common.CommonAwsClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;

@Service
public class AwsEbsCommonService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsEbsCommonService.class);

    @Inject
    private CommonAwsClient awsClient;

    public Optional<DescribeVolumesResult> getEbsSize(CloudCredential cloudCredential, String region, String ebsId) {
        try {
            Optional<DescribeVolumesResult> result = Optional.empty();
            AwsCredentialView credentialView = new AwsCredentialView(cloudCredential);
            AmazonEc2Client ec2Client = awsClient.createAccessWithMinimalRetries(credentialView, region);
            DescribeVolumesRequest describeVolumesRequest = new DescribeVolumesRequest();
            describeVolumesRequest.setVolumeIds(Set.of(ebsId));
            DescribeVolumesResult describeVolumesResult = ec2Client.describeVolumes(describeVolumesRequest);
            Optional<Volume> volumeOptional = describeVolumesResult.getVolumes().stream().findFirst();
            if (volumeOptional.isPresent()) {
                LOGGER.info("Successfully queried EBS for {}. Returned number of volumes: {}",
                        ebsId, volumeOptional.get().getSize());
                result = Optional.of(describeVolumesResult);
            }
            return result;
        } catch (AmazonEBSException e) {
            String message = String.format("Cannot get size for EBS %s. Reason: %s",
                    ebsId, e.getMessage());
            LOGGER.error(message, e);
            throw new CloudConnectorException(message, e);
        }
    }
}
