package com.sequenceiq.cloudbreak.cloud.aws.common.connector.resource;

import java.util.Date;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.amazonaws.services.ebs.model.AmazonEBSException;
import com.amazonaws.services.ec2.model.DescribeVolumesRequest;
import com.amazonaws.services.ec2.model.DescribeVolumesResult;

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

    public DescribeVolumesResult getEbsSize(CloudCredential cloudCredential, String region, Date startTime, Date endTime, String ebsId) {
        try {
            AwsCredentialView credentialView = new AwsCredentialView(cloudCredential);
            AmazonEc2Client ec2Client = awsClient.createAccessWithMinimalRetries(credentialView, region);
            DescribeVolumesRequest describeVolumesRequest = new DescribeVolumesRequest();
            describeVolumesRequest.setVolumeIds(Set.of(ebsId));
            DescribeVolumesResult describeVolumesResult = ec2Client.describeVolumes(describeVolumesRequest);
            LOGGER.info("Successfully queried efs for {} and timeframe from {} to {}. Returned number of datapoints: {}",
                    ebsId, startTime, endTime, describeVolumesResult.getVolumes().stream().findFirst().get().getSize());
            return describeVolumesResult;
        } catch (AmazonEBSException e) {
            String message = String.format("Cannot get size for efs %s and timeframe from %s to %s. Reason: %s",
                    ebsId, startTime, endTime, e.getMessage());
            LOGGER.error(message, e);
            throw new CloudConnectorException(message, e);
        }
    }
}
