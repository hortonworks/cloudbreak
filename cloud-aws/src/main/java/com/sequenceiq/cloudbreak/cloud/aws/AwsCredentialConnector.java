package com.sequenceiq.cloudbreak.cloud.aws;

import static com.sequenceiq.cloudbreak.cloud.model.CloudCredential.SMART_SENSE_ID;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNoneEmpty;

import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.amazonaws.AmazonClientException;
import com.sequenceiq.cloudbreak.cloud.CredentialConnector;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.credential.CredentialNotifier;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredentialStatus;
import com.sequenceiq.cloudbreak.cloud.model.CredentialStatus;
import com.sequenceiq.cloudbreak.cloud.model.ExtendedCloudCredential;

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
            credential.putParameter(SMART_SENSE_ID, smartSenseId);
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
        return new CloudCredentialStatus(auth.getCloudCredential(), CredentialStatus.CREATED);
    }

    @Override
    public Map<String, String> interactiveLogin(CloudContext cloudContext, ExtendedCloudCredential extendedCloudCredential,
            CredentialNotifier credentialNotifier) {
        throw new UnsupportedOperationException("Interactive login not supported on AWS");
    }

    @Override
    public CloudCredentialStatus delete(AuthenticatedContext auth) {
        return new CloudCredentialStatus(auth.getCloudCredential(), CredentialStatus.DELETED);
    }

    private CloudCredentialStatus verifyIamRoleIsAssumable(CloudCredential cloudCredential) {
        AwsCredentialView awsCredential = new AwsCredentialView(cloudCredential);
        try {
            credentialClient.retrieveSessionCredentials(awsCredential);
        } catch (AmazonClientException ae) {
            if (ae.getMessage().contains("Unable to load AWS credentials")) {
                String errorMessage =
                        "Unable to load AWS credentials: please make sure the deployer defined AWS_ACCESS_KEY_ID and AWS_SECRET_ACCESS_KEY";
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
