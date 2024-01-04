package com.sequenceiq.cloudbreak.cloud.aws.common.connector.resource;

import java.util.Optional;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.aws.common.CommonAwsClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;

import software.amazon.awssdk.services.ebs.model.EbsException;
import software.amazon.awssdk.services.ec2.model.DescribeVolumesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeVolumesResponse;
import software.amazon.awssdk.services.ec2.model.Volume;

@Service
public class AwsEbsCommonService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsEbsCommonService.class);

    @Inject
    private CommonAwsClient awsClient;

    public Optional<DescribeVolumesResponse> getEbsSize(CloudCredential cloudCredential, String region, String ebsId) {
        try {
            Optional<DescribeVolumesResponse> result = Optional.empty();
            AwsCredentialView credentialView = new AwsCredentialView(cloudCredential);
            AmazonEc2Client ec2Client = awsClient.createAccessWithMinimalRetries(credentialView, region);
            DescribeVolumesRequest describeVolumesRequest = DescribeVolumesRequest.builder().volumeIds(ebsId).build();
            DescribeVolumesResponse describeVolumesResult = ec2Client.describeVolumes(describeVolumesRequest);
            Optional<Volume> volumeOptional = describeVolumesResult.volumes().stream().findFirst();
            if (volumeOptional.isPresent()) {
                LOGGER.info("Successfully queried EBS for {}. Returned number of volumes: {}",
                        ebsId, volumeOptional.get().size());
                result = Optional.of(describeVolumesResult);
            }
            return result;
        } catch (EbsException e) {
            String message = String.format("Cannot get size for EBS %s. Reason: %s",
                    ebsId, e.getMessage());
            LOGGER.error(message, e);
            throw new CloudConnectorException(message, e);
        }
    }
}
