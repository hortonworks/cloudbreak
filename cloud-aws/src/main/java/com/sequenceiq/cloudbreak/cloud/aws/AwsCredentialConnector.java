package com.sequenceiq.cloudbreak.cloud.aws;

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNoneEmpty;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DeleteKeyPairRequest;
import com.amazonaws.services.ec2.model.ImportKeyPairRequest;
import com.sequenceiq.cloudbreak.cloud.CredentialConnector;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredentialStatus;
import com.sequenceiq.cloudbreak.cloud.model.CredentialStatus;

@Service
public class AwsCredentialConnector implements CredentialConnector {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsCredentialConnector.class);

    @Inject
    private AwsSessionCredentialClient credentialClient;
    @Inject
    private AwsClient awsClient;
    @Inject
    private AwsSmartSenseIdGenerator smartSenseIdGenerator;

    @Override
    public CloudCredentialStatus verify(AuthenticatedContext authenticatedContext) {
        CloudCredential credential = authenticatedContext.getCloudCredential();
        LOGGER.info("Create credential: {}", credential);
        AwsCredentialView awsCredential = new AwsCredentialView(credential);
        String roleArn = awsCredential.getRoleArn();
        String accessKey = awsCredential.getAccessKey();
        String secretKey = awsCredential.getSecretKey();
        String smartSenseId = smartSenseIdGenerator.getSmartSenseId(awsCredential);
        if (StringUtils.isNoneEmpty(smartSenseId)) {
            credential.putParameter("smartSenseId", smartSenseId);
        }
        if (isNoneEmpty(roleArn) && isNoneEmpty(accessKey) && isNoneEmpty(secretKey)) {
            String message = "Please only provide the 'role arn' or the 'access' and 'secret key'";
            return new CloudCredentialStatus(credential, CredentialStatus.FAILED, new Exception(message), message);
        }
        if (isNoneEmpty(roleArn)) {
            return verifyIamRoleIsAssumable(credential);
        }
        if (isEmpty(accessKey) || isEmpty(secretKey)) {
            String message = "Please provide both the 'access' and 'secret key'";
            return new CloudCredentialStatus(credential, CredentialStatus.FAILED, new Exception(message), message);
        }
        return new CloudCredentialStatus(credential, CredentialStatus.VERIFIED);
    }

    @Override
    public CloudCredentialStatus create(AuthenticatedContext auth) {
        AwsCredentialView awsCredential = new AwsCredentialView(auth.getCloudCredential());
        String region = auth.getCloudContext().getLocation().getRegion().value();
        if (!awsClient.existingKeyPairNameSpecified(auth)) {
            try {
                LOGGER.info(String.format("Importing public key to %s region on AWS", region));
                AmazonEC2Client client = awsClient.createAccess(awsCredential, region);
                ImportKeyPairRequest importKeyPairRequest = new ImportKeyPairRequest(awsClient.getKeyPairName(auth), awsCredential.getPublicKey());
                client.importKeyPair(importKeyPairRequest);
            } catch (Exception e) {
                String errorMessage = String.format("Failed to import public key [roleArn:'%s'], detailed message: %s", awsCredential.getRoleArn(),
                        e.getMessage());
                LOGGER.error(errorMessage, e);
                return new CloudCredentialStatus(auth.getCloudCredential(), CredentialStatus.FAILED, e, errorMessage);
            }
        }
        return new CloudCredentialStatus(auth.getCloudCredential(), CredentialStatus.CREATED);
    }

    @Override
    public CloudCredentialStatus delete(AuthenticatedContext auth) {
        AwsCredentialView awsCredential = new AwsCredentialView(auth.getCloudCredential());
        String region = auth.getCloudContext().getLocation().getRegion().value();
        if (!awsClient.existingKeyPairNameSpecified(auth)) {
            try {
                AmazonEC2Client client = awsClient.createAccess(awsCredential, region);
                DeleteKeyPairRequest deleteKeyPairRequest = new DeleteKeyPairRequest(awsClient.getKeyPairName(auth));
                client.deleteKeyPair(deleteKeyPairRequest);
            } catch (Exception e) {
                String errorMessage = String.format("Failed to delete public key [roleArn:'%s', region: '%s'], detailed message: %s",
                        awsCredential.getRoleArn(), region, e.getMessage());
                LOGGER.error(errorMessage, e);
            }
        }
        return new CloudCredentialStatus(auth.getCloudCredential(), CredentialStatus.DELETED);
    }

    private CloudCredentialStatus verifyIamRoleIsAssumable(CloudCredential cloudCredential) {
        AwsCredentialView awsCredential = new AwsCredentialView(cloudCredential);
        try {
            credentialClient.retrieveSessionCredentials(awsCredential);
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
}
