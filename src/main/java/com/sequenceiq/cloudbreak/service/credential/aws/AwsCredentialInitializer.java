package com.sequenceiq.cloudbreak.service.credential.aws;

import java.util.Random;

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
public class AwsCredentialInitializer {

    public static final String CLOUDBREAK_KEY_NAME = "cloudbreak-key";

    private static final int SUFFIX_RND = 999999;

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsCredentialInitializer.class);

    @Autowired
    private CrossAccountCredentialsProvider crossAccountCredentialsProvider;

    @Autowired
    private AwsStackUtil awsStackUtil;

    public AwsCredential init(AwsCredential awsCredential) {
        validateIamRole(awsCredential);
        return importKeyPairs(awsCredential);
    }

    private AwsCredential importKeyPairs(AwsCredential awsCredential) {
        try {
            Random rnd = new Random();
            String keyPairName = CLOUDBREAK_KEY_NAME + "-" + rnd.nextInt(SUFFIX_RND);
            for (Regions regions : Regions.values()) {
                if (!Regions.CN_NORTH_1.equals(regions) && !Regions.GovCloud.equals(regions)) {
                    AmazonEC2Client client = awsStackUtil.createEC2Client(regions, awsCredential);
                    ImportKeyPairRequest importKeyPairRequest = new ImportKeyPairRequest(keyPairName, awsCredential.getPublicKey());
                    client.importKeyPair(importKeyPairRequest);
                }
            }
            awsCredential.setKeyPairName(keyPairName);
        } catch (Exception e) {
            String errorMessage = String.format("Failed to import public key [credential: '%s', roleArn: '%s'], detailed message: %s", awsCredential.getId(),
                    awsCredential.getRoleArn(), e.getMessage());
            LOGGER.error(errorMessage, e);
            throw new BadRequestException(errorMessage, e);
        }
        return awsCredential;
    }

    private void validateIamRole(AwsCredential awsCredential) {
        try {
            crossAccountCredentialsProvider.retrieveSessionCredentials(CrossAccountCredentialsProvider.DEFAULT_SESSION_CREDENTIALS_DURATION,
                    CrossAccountCredentialsProvider.DEFAULT_EXTERNAL_ID, awsCredential);
        } catch (Exception e) {
            String errorMessage = String.format("Could not assume role [credential: '%s', roleArn: '%s'], detailed message: %s", awsCredential.getId(),
                    awsCredential.getRoleArn(), e.getMessage());
            LOGGER.error(errorMessage, e);
            throw new BadRequestException(errorMessage, e);
        }
    }
}
