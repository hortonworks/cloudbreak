package com.sequenceiq.cloudbreak.cloud.aws;


import java.util.Collection;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DeleteKeyPairRequest;
import com.amazonaws.services.ec2.model.ImportKeyPairRequest;
import com.sequenceiq.cloudbreak.cloud.CredentialConnector;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.event.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
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
        CloudCredentialStatus cloudCredentialStatus = validateIamRoleIsAssumable(authenticatedContext.getCloudCredential());
        if (cloudCredentialStatus.getStatus().equals(CredentialStatus.FAILED)) {
            return cloudCredentialStatus;
        }
        return importKeyPairs(authenticatedContext.getCloudCredential());
    }

    @Override
    public CloudCredentialStatus delete(AuthenticatedContext authenticatedContext) {
        AwsCredentialView awsCredential = new AwsCredentialView(authenticatedContext.getCloudCredential());
        String keyPairName = awsCredential.getKeyPairName();

        Collection<String> regions = platformParameters.regions().keySet();

        for (String region : regions) {
            if (!"CN_NORTH_1".equalsIgnoreCase(region) && !"GovCloud".equalsIgnoreCase(region)) {
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

    private CloudCredentialStatus validateIamRoleIsAssumable(CloudCredential cloudCredential) {
        AwsCredentialView awsCredential = new AwsCredentialView(cloudCredential);
        try {
            awsClient.retrieveCachedSessionCredentials(awsCredential);
        } catch (AmazonClientException ae) {
            if (ae.getMessage().contains("Unable to load AWS credentials")) {
                String errorMessage =
                        String.format("Unable to load AWS credentials: please make sure the deployer defined AWS_ACCESS_KEY_ID and AWS_SECRET_ACCESS_KEY");
                LOGGER.error(errorMessage, ae);
                return new CloudCredentialStatus(cloudCredential, CredentialStatus.FAILED, ae, errorMessage);
            }
        } catch (Exception e) {
            String errorMessage = String.format("Could not assume role '%s': check if the role exists and if it's created with the correct external ID",
                    awsCredential.getRoleArn());
            LOGGER.error(errorMessage, e);
            return new CloudCredentialStatus(cloudCredential, CredentialStatus.FAILED, e, errorMessage);
        }
        return new CloudCredentialStatus(cloudCredential, CredentialStatus.CREATED);
    }


    private CloudCredentialStatus importKeyPairs(CloudCredential cloudCredential) {
        AwsCredentialView awsCredential = new AwsCredentialView(cloudCredential);
        try {
            Collection<String> regions = platformParameters.regions().keySet();
            for (String region : regions) {
                if (!"CN_NORTH_1".equalsIgnoreCase(region) && !"GovCloud".equalsIgnoreCase(region)) {
                    LOGGER.info(String.format("Importing publickey to %s region on AWS", region));
                    AmazonEC2Client client = awsClient.createAccess(awsCredential, region);
                    ImportKeyPairRequest importKeyPairRequest = new ImportKeyPairRequest(awsCredential.getKeyPairName(), awsCredential.getPublicKey());
                    client.importKeyPair(importKeyPairRequest);
                }
            }
        } catch (Exception e) {
            String errorMessage = String.format("Failed to import public key [roleArn:'%s'], detailed message: %s", awsCredential.getRoleArn(), e.getMessage());
            LOGGER.error(errorMessage, e);
            return new CloudCredentialStatus(cloudCredential, CredentialStatus.FAILED, e, errorMessage);
        }
        return new CloudCredentialStatus(cloudCredential, CredentialStatus.CREATED);
    }
}
