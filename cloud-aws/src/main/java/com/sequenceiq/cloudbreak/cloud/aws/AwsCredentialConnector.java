package com.sequenceiq.cloudbreak.cloud.aws;


import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DeleteKeyPairRequest;
import com.amazonaws.services.ec2.model.ImportKeyPairRequest;
import com.sequenceiq.cloudbreak.cloud.CredentialConnector;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.event.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredentialStatus;
import com.sequenceiq.cloudbreak.cloud.model.CredentialStatus;

@Service
public class AwsCredentialConnector implements CredentialConnector {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsCredentialConnector.class);

    @Inject
    private AwsClient awsClient;

    @Inject
    private AwsPlatformParameters platformParameters;

    @Override
    public CloudCredentialStatus create(AuthenticatedContext authenticatedContext) {
        LOGGER.info("Create credential: {}", authenticatedContext.getCloudCredential());
        AwsCredentialView awsCredential = new AwsCredentialView(authenticatedContext.getCloudCredential());

        /*
        validateIamRoleUniqueness(awsCredential);
        validateIamRoleIsAssumable(awsCredential);
        */
        importKeyPairs(awsCredential);
        return new CloudCredentialStatus(authenticatedContext.getCloudCredential(), CredentialStatus.CREATED);
    }

    @Override
    public CloudCredentialStatus delete(AuthenticatedContext authenticatedContext) {
        AwsCredentialView awsCredential = new AwsCredentialView(authenticatedContext.getCloudCredential());
        String keyPairName = awsCredential.getKeyPairName();

        Set<String> regions = platformParameters.regions().keySet();

        for (String region : regions) {
            if (!"cn-north-1".equalsIgnoreCase(region) && !"us-gov-west-1".equalsIgnoreCase(region)) {
                try {
                    AmazonEC2Client client = awsClient.createAccess(awsCredential, region);
                    DeleteKeyPairRequest deleteKeyPairRequest = new DeleteKeyPairRequest(keyPairName);
                    client.deleteKeyPair(deleteKeyPairRequest);
                } catch (Exception e) {
                    String errorMessage = String.format("Failed to delete public key [roleArn:'%s', region: '%s'], detailed message: %s",
                            awsCredential.getRoleArn(), regions, e.getMessage());
                    LOGGER.error(errorMessage, e);
                }
            }
        }
        return new CloudCredentialStatus(authenticatedContext.getCloudCredential(), CredentialStatus.DELETED);
    }

    /*private void validateIamRoleUniqueness(AwsCredential credential) {
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
        } catch (AmazonClientException ae) {
            if (ae.getMessage().contains("Unable to load AWS credentials")) {

                String errorMessage =
                        String.format("Unable to load AWS credentials: please make sure the deployer defined AWS_ACCESS_KEY_ID and AWS_SECRET_ACCESS_KEY");
                LOGGER.error(errorMessage, ae);
                throw new BadRequestException(errorMessage, ae);
            }
        } catch (Exception e) {
            String errorMessage = String.format("Could not assume role '%s': check if the role exists and if it's created with the correct external ID: '%s' ",
                    awsCredential.getRoleArn(), crossAccountCredentialsProvider.getExternalId());
            LOGGER.error(errorMessage, e);
            throw new BadRequestException(errorMessage, e);
        }
    }
*/

    private void importKeyPairs(AwsCredentialView awsCredential) {
        try {
            Set<String> regions = platformParameters.regions().keySet();
            for (String region : regions) {
                if (!"cn-north-1".equalsIgnoreCase(region) && !"us-gov-west-1".equalsIgnoreCase(region)) {
                    AmazonEC2Client client = awsClient.createAccess(awsCredential, region);
                    ImportKeyPairRequest importKeyPairRequest = new ImportKeyPairRequest(awsCredential.getKeyPairName(), awsCredential.getPublicKey());
                    client.importKeyPair(importKeyPairRequest);
                }
            }
        } catch (Exception e) {
            String errorMessage = String.format("Failed to import public key [roleArn:'%s'], detailed message: %s", awsCredential.getRoleArn(), e.getMessage());
            LOGGER.error(errorMessage, e);
            throw new RuntimeException(errorMessage, e);
        }
    }
}
