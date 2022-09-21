package com.sequenceiq.cloudbreak.cloud.aws.common.connector.resource;

import java.util.Date;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.amazonaws.services.elasticfilesystem.model.AmazonElasticFileSystemException;
import com.amazonaws.services.elasticfilesystem.model.DescribeFileSystemsRequest;
import com.amazonaws.services.elasticfilesystem.model.DescribeFileSystemsResult;
import com.sequenceiq.cloudbreak.cloud.aws.common.CommonAwsClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEfsClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;

@Service
public class AwsEfsCommonService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsEfsCommonService.class);

    @Inject
    private CommonAwsClient awsClient;

    public DescribeFileSystemsResult getEfsSize(CloudCredential cloudCredential, String region, Date startTime, Date endTime, String efsId) {
        try {
            AwsCredentialView credentialView = new AwsCredentialView(cloudCredential);
            AmazonEfsClient elasticFileSystemClient = awsClient.createElasticFileSystemClient(credentialView, region);
            DescribeFileSystemsRequest describeFileSystemsRequest = new DescribeFileSystemsRequest();
            describeFileSystemsRequest.setFileSystemId(efsId);
            DescribeFileSystemsResult describeFileSystemsResult = elasticFileSystemClient.describeFileSystems(describeFileSystemsRequest);
            LOGGER.info("Successfully queried efs for {} and timeframe from {} to {}. Returned number of datapoints: {}",
                    efsId, startTime, endTime, describeFileSystemsResult.getFileSystems().stream().findFirst().get().getSizeInBytes());
            return describeFileSystemsResult;
        } catch (AmazonElasticFileSystemException e) {
            String message = String.format("Cannot get size for efs %s and timeframe from %s to %s. Reason: %s",
                    efsId, startTime, endTime, e.getMessage());
            LOGGER.error(message, e);
            throw new CloudConnectorException(message, e);
        }
    }
}
