package com.sequenceiq.cloudbreak.service.credential.aws;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.ImportKeyPairRequest;
import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.domain.AwsCredential;
import com.sequenceiq.cloudbreak.service.stack.connector.aws.AwsStackUtil;

@Component
public class AwsCredentialValidator {

    public static final String CLOUDBREAK_KEY_NAME = "cloudbreak-key";

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsCredentialValidator.class);

    @Autowired
    private CrossAccountCredentialsProvider crossAccountCredentialsProvider;

    @Autowired
    private AwsStackUtil awsStackUtil;

    public void validate(AwsCredential awsCredential) {
        try {
            crossAccountCredentialsProvider.retrieveSessionCredentials(CrossAccountCredentialsProvider.DEFAULT_SESSION_CREDENTIALS_DURATION,
                    CrossAccountCredentialsProvider.DEFAULT_EXTERNAL_ID, awsCredential);
        } catch (Exception e) {
            String errorMessage = String.format("Could not assume role [credential: '%s', roleArn: '%s'", awsCredential.getId(), awsCredential.getRoleArn());
            LOGGER.error(errorMessage, e);
            throw new BadRequestException(errorMessage, e);
        }

        try {
            for (Regions regions : Regions.values()) {
                AmazonEC2Client client = awsStackUtil.createEC2Client(regions, awsCredential);
                ImportKeyPairRequest importKeyPairRequest = new ImportKeyPairRequest(CLOUDBREAK_KEY_NAME, awsCredential.getPublicKey());
                client.importKeyPair(importKeyPairRequest);
            }
        } catch (Exception e) {
            String errorMessage = String.format("Failed to import public key [credential: '%s', roleArn: '%s']");
            LOGGER.error(errorMessage, e);
            throw new BadRequestException(errorMessage, e);
        }
    }
}
