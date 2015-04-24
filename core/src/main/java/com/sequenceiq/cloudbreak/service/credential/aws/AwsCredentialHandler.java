package com.sequenceiq.cloudbreak.service.credential.aws;

import java.util.Random;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DeleteKeyPairRequest;
import com.amazonaws.services.ec2.model.ImportKeyPairRequest;
import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.domain.AwsCredential;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.repository.CredentialRepository;
import com.sequenceiq.cloudbreak.service.credential.CredentialHandler;
import com.sequenceiq.cloudbreak.service.credential.RsaPublicKeyValidator;
import com.sequenceiq.cloudbreak.service.stack.connector.aws.AwsStackUtil;

@Component
public class AwsCredentialHandler implements CredentialHandler<AwsCredential> {

    public static final String CLOUDBREAK_KEY_NAME = "cloudbreak-key";
    private static final int SUFFIX_RND = 999999;
    private static final Logger LOGGER = LoggerFactory.getLogger(AwsCredentialHandler.class);

    @Autowired
    private RsaPublicKeyValidator rsaPublicKeyValidator;

    @Autowired
    private CrossAccountCredentialsProvider crossAccountCredentialsProvider;

    @Autowired
    private AwsStackUtil awsStackUtil;

    @Autowired
    private CredentialRepository credentialRepository;

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.AWS;
    }

    @Override
    public AwsCredential init(AwsCredential awsCredential) {
        rsaPublicKeyValidator.validate(awsCredential);
        validateIamRoleUniqueness(awsCredential);
        validateIamRoleIsAssumable(awsCredential);
        return importKeyPairs(awsCredential);
    }

    @Override
    public boolean delete(AwsCredential awsCredential) {
        boolean result = true;
        String keyPairName = awsCredential.getKeyPairName();
        for (Regions regions : Regions.values()) {
            if (!Regions.CN_NORTH_1.equals(regions) && !Regions.GovCloud.equals(regions)) {
                try {
                    AmazonEC2Client client = awsStackUtil.createEC2Client(regions, awsCredential);
                    DeleteKeyPairRequest deleteKeyPairRequest = new DeleteKeyPairRequest(keyPairName);
                    client.deleteKeyPair(deleteKeyPairRequest);
                } catch (Exception e) {
                    String errorMessage = String.format("Failed to delete public key [roleArn:'%s', region: '%s'], detailed message: %s",
                            awsCredential.getRoleArn(), regions, e.getMessage());
                    LOGGER.error(errorMessage, e);
                    result = false;
                }
            }
        }
        return result;
    }

    @Override
    public AwsCredential update(AwsCredential credential) throws Exception {
        return credential;
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
            String errorMessage = String.format("Failed to import public key [roleArn:'%s'], detailed message: %s", awsCredential.getRoleArn(), e.getMessage());
            LOGGER.error(errorMessage, e);
            throw new BadRequestException(errorMessage, e);
        }
        return awsCredential;
    }


    private void validateIamRoleUniqueness(AwsCredential credential) {
        Set<Credential> credentials = credentialRepository.findByRoleArn(credential.getRoleArn());
        for (Credential c : credentials) {
            if (!c.getAccount().equals(credential.getAccount())) {
                throw new BadRequestException(String.format("IAM role '%s' is already in use by a different account.", credential.getRoleArn()));
            }
        }
    }

    private void validateIamRoleIsAssumable(AwsCredential awsCredential) {
        try {
            crossAccountCredentialsProvider.retrieveSessionCredentials(CrossAccountCredentialsProvider.DEFAULT_SESSION_CREDENTIALS_DURATION,
                    crossAccountCredentialsProvider.getExternalId(), awsCredential);
        } catch (Exception e) {
            String errorMessage = String.format("Could not assume role '%s': check if the role exists and if it's created with the correct external ID: '%s' ",
                    awsCredential.getRoleArn(), crossAccountCredentialsProvider.getExternalId());
            LOGGER.error(errorMessage, e);
            throw new BadRequestException(errorMessage, e);
        }
    }
}
