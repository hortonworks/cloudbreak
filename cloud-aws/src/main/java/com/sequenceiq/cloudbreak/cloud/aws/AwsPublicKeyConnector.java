package com.sequenceiq.cloudbreak.cloud.aws;

import org.apache.commons.lang3.StringUtils;
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
import com.sequenceiq.cloudbreak.cloud.model.publickey.PublicKeyDescribeRequest;
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
            if (!exists(client, request.getPublicKeyId())) {
                ImportKeyPairRequest importKeyPairRequest = new ImportKeyPairRequest(request.getPublicKeyId(), request.getPublicKey());
                client.importKeyPair(importKeyPairRequest);
            }
        } catch (Exception e) {
            String errorMessage = String.format("Failed to import public key [%s:'%s', region: '%s'], detailed message: %s",
                    getType(awsCredential), getAwsId(awsCredential), request.getRegion(), e.getMessage());
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
            String errorMessage = String.format("Failed to delete public key [%s: '%s', region: '%s'], detailed message: %s",
                    getType(awsCredential), getAwsId(awsCredential), request.getRegion(), e.getMessage());
            LOGGER.error(errorMessage, e);
        }
    }

    @Override
    public boolean exists(PublicKeyDescribeRequest request) {
        LOGGER.debug("Describe public key {} in {} region on AWS", request.getPublicKeyId(), request.getRegion());
        AwsCredentialView awsCredential = new AwsCredentialView(request.getCredential());
        try {
            AmazonEC2Client client = awsClient.createAccess(awsCredential, request.getRegion());
            return exists(client, request.getPublicKeyId());
        } catch (Exception e) {
            String errorMessage = String.format("Failed to describe public key [%s:'%s', region: '%s'], detailed message: %s",
                    getType(awsCredential), getAwsId(awsCredential), request.getRegion(), e.getMessage());
            LOGGER.error(errorMessage, e);
        }
        return false;
    }

    @Override
    public Platform platform() {
        return AwsConstants.AWS_PLATFORM;
    }

    @Override
    public Variant variant() {
        return AwsConstants.AWS_VARIANT;
    }

    private boolean exists(AmazonEC2Client client, String publicKeyId) {
        try {
            client.describeKeyPairs(new DescribeKeyPairsRequest().withKeyNames(publicKeyId));
            LOGGER.debug("Key-pair already exists: {}", publicKeyId);
            return true;
        } catch (AmazonServiceException e) {
            LOGGER.debug("Key-pair does not exist: {}", publicKeyId);
        }
        return false;
    }

    private String getAwsId(AwsCredentialView awsCredential) {
        if (StringUtils.isNotEmpty(awsCredential.getRoleArn())) {
            return awsCredential.getRoleArn();
        } else {
            return awsCredential.getAccessKey();
        }
    }

    private String getType(AwsCredentialView awsCredential) {
        if (StringUtils.isNotEmpty(awsCredential.getRoleArn())) {
            return "roleArn";
        } else {
            return "accessKey";
        }
    }
}
