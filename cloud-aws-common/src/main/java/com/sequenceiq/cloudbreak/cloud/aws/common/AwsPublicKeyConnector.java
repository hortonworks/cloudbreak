package com.sequenceiq.cloudbreak.cloud.aws.common;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.PublicKeyConnector;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.cloud.model.publickey.PublicKeyDescribeRequest;
import com.sequenceiq.cloudbreak.cloud.model.publickey.PublicKeyRegisterRequest;
import com.sequenceiq.cloudbreak.cloud.model.publickey.PublicKeyUnregisterRequest;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.ec2.model.DeleteKeyPairRequest;
import software.amazon.awssdk.services.ec2.model.DescribeKeyPairsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeKeyPairsResponse;
import software.amazon.awssdk.services.ec2.model.ImportKeyPairRequest;
import software.amazon.awssdk.services.ec2.model.KeyPairInfo;

@Service
public class AwsPublicKeyConnector implements PublicKeyConnector {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsPublicKeyConnector.class);

    private final CommonAwsClient awsClient;

    public AwsPublicKeyConnector(CommonAwsClient awsClient) {
        this.awsClient = awsClient;
    }

    @Override
    public void register(PublicKeyRegisterRequest request) {
        LOGGER.debug("Importing public key {} to {} region on AWS", request.getPublicKeyId(), request.getRegion());
        AwsCredentialView awsCredential = new AwsCredentialView(request.getCredential());
        try {
            AmazonEc2Client client = awsClient.createEc2Client(awsCredential, request.getRegion());
            if (!exists(client, request.getPublicKeyId())) {
                ImportKeyPairRequest importKeyPairRequest = ImportKeyPairRequest.builder()
                        .keyName(request.getPublicKeyId())
                        .publicKeyMaterial(SdkBytes.fromUtf8String(request.getPublicKey()))
                        .build();
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
            AmazonEc2Client client = awsClient.createEc2Client(awsCredential, request.getRegion());
            DeleteKeyPairRequest deleteKeyPairRequest = DeleteKeyPairRequest.builder().keyName(request.getPublicKeyId()).build();
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
            AmazonEc2Client client = awsClient.createEc2Client(awsCredential, request.getRegion());
            return exists(client, request.getPublicKeyId());
        } catch (Exception e) {
            String errorMessage = String.format("Failed to describe public key [%s:'%s', region: '%s'], detailed message: %s",
                    getType(awsCredential), getAwsId(awsCredential), request.getRegion(), e.getMessage());
            LOGGER.error(errorMessage, e);
        }
        return false;
    }

    @Override
    public String rawPublicKey(PublicKeyDescribeRequest request) {
        LOGGER.debug("Describe public key {} in {} region on AWS", request.getPublicKeyId(), request.getRegion());
        AwsCredentialView awsCredential = new AwsCredentialView(request.getCredential());
        try {
            AmazonEc2Client client = awsClient.createEc2Client(awsCredential, request.getRegion());
            return rawPublicKey(client, request.getPublicKeyId());
        } catch (Exception e) {
            String errorMessage = String.format("Failed to describe public key [%s:'%s', region: '%s'], detailed message: %s",
                    getType(awsCredential), getAwsId(awsCredential), request.getRegion(), e.getMessage());
            LOGGER.error(errorMessage, e);
        }
        return null;
    }

    @Override
    public Platform platform() {
        return AwsConstants.AWS_PLATFORM;
    }

    @Override
    public Variant variant() {
        return AwsConstants.AWS_DEFAULT_VARIANT;
    }

    private boolean exists(AmazonEc2Client client, String publicKeyId) {
        try {
            client.describeKeyPairs(DescribeKeyPairsRequest.builder().keyNames(publicKeyId).build());
            LOGGER.debug("Key-pair already exists: {}", publicKeyId);
            return true;
        } catch (AwsServiceException e) {
            LOGGER.debug("Key-pair does not exist: {}", publicKeyId);
        }
        return false;
    }

    private String rawPublicKey(AmazonEc2Client client, String publicKeyId) {
        try {
            DescribeKeyPairsResponse describeKeyPairsResponse = client.describeKeyPairs(DescribeKeyPairsRequest.builder()
                    .keyNames(publicKeyId).includePublicKey(Boolean.TRUE).build());
            return describeKeyPairsResponse.keyPairs().stream().findFirst().map(KeyPairInfo::publicKey).orElse(null);
        } catch (AwsServiceException e) {
            LOGGER.debug("Key-pair does not exist: {}", publicKeyId);
        }
        return null;
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
