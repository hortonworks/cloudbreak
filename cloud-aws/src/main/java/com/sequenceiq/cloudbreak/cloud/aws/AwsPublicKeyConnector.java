package com.sequenceiq.cloudbreak.cloud.aws;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DeleteKeyPairRequest;
import com.amazonaws.services.ec2.model.DescribeKeyPairsRequest;
import com.amazonaws.services.ec2.model.ImportKeyPairRequest;
import com.sequenceiq.cloudbreak.cloud.PublicKeyConnector;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.cloud.model.publickey.PublicKeyRegisterRequest;
import com.sequenceiq.cloudbreak.cloud.model.publickey.PublicKeyUnregisterRequest;

@Service
public class AwsPublicKeyConnector implements PublicKeyConnector {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsPublicKeyConnector.class);

    private final AwsClient awsClient;

    public AwsPublicKeyConnector(AwsClient awsClient) {
        this.awsClient = awsClient;
    }

    @Override
    public void register(PublicKeyRegisterRequest request) {
        LOGGER.debug("Importing public key {} to {} region on AWS", request.getPublicKeyId(), request.getRegion());
        AwsCredentialView awsCredential = new AwsCredentialView(request.getCredential());
        try {
            AmazonEC2Client client = awsClient.createAccess(awsCredential, request.getRegion());
            ImportKeyPairRequest importKeyPairRequest = new ImportKeyPairRequest(request.getPublicKeyId(), request.getPublicKey());
            try {
                client.describeKeyPairs(new DescribeKeyPairsRequest().withKeyNames(request.getPublicKeyId()));
                LOGGER.debug("Key-pair already exists: {}", request.getPublicKeyId());
            } catch (AmazonServiceException e) {
                client.importKeyPair(importKeyPairRequest);
            }
        } catch (Exception e) {
            String errorMessage = String.format("Failed to import public key [roleArn:'%s', region: '%s'], detailed message: %s",
                    awsCredential.getRoleArn(), request.getRegion(), e.getMessage());
            LOGGER.error(errorMessage, e);
            throw new CloudConnectorException(e.getMessage(), e);
        }
    }

    @Override
    public void unregister(PublicKeyUnregisterRequest request) {
        LOGGER.debug("Deleting public key {} in {} region on AWS", request.getPublicKeyId(), request.getRegion());
        AwsCredentialView awsCredential = new AwsCredentialView(request.getCredential());
        try {
            AmazonEC2Client client = awsClient.createAccess(awsCredential, request.getRegion());
            DeleteKeyPairRequest deleteKeyPairRequest = new DeleteKeyPairRequest(request.getPublicKeyId());
            client.deleteKeyPair(deleteKeyPairRequest);
        } catch (Exception e) {
            String errorMessage = String.format("Failed to delete public key [roleArn:'%s', region: '%s'], detailed message: %s",
                    awsCredential.getRoleArn(), request.getRegion(), e.getMessage());
            LOGGER.error(errorMessage, e);
        }
    }

    @Override
    public Platform platform() {
        return AwsConstants.AWS_PLATFORM;
    }

    @Override
    public Variant variant() {
        return AwsConstants.AWS_VARIANT;
    }
}
