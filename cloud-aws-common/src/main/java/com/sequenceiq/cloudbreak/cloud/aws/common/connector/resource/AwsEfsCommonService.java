package com.sequenceiq.cloudbreak.cloud.aws.common.connector.resource;

import java.util.Date;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.aws.common.CommonAwsClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEfsClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;

import software.amazon.awssdk.services.efs.model.DescribeFileSystemsRequest;
import software.amazon.awssdk.services.efs.model.DescribeFileSystemsResponse;
import software.amazon.awssdk.services.efs.model.EfsException;

@Service
public class AwsEfsCommonService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsEfsCommonService.class);

    @Inject
    private CommonAwsClient awsClient;

    public DescribeFileSystemsResponse getEfsSize(CloudCredential cloudCredential, String region, Date startTime, Date endTime, String efsId) {
        try {
            AwsCredentialView credentialView = new AwsCredentialView(cloudCredential);
            AmazonEfsClient elasticFileSystemClient = awsClient.createElasticFileSystemClient(credentialView, region);
            DescribeFileSystemsRequest describeFileSystemsRequest = DescribeFileSystemsRequest.builder().fileSystemId(efsId).build();
            DescribeFileSystemsResponse describeFileSystemsResponse = elasticFileSystemClient.describeFileSystems(describeFileSystemsRequest);
            LOGGER.info("Successfully queried efs for {} and timeframe from {} to {}. Returned number of datapoints: {}",
                    efsId, startTime, endTime, describeFileSystemsResponse.fileSystems().stream().findFirst().get().sizeInBytes());
            return describeFileSystemsResponse;
        } catch (EfsException e) {
            String message = String.format("Cannot get size for efs %s and timeframe from %s to %s. Reason: %s",
                    efsId, startTime, endTime, e.getMessage());
            LOGGER.error(message, e);
            throw new CloudConnectorException(message, e);
        }
    }
}
