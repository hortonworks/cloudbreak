package com.sequenceiq.cloudbreak.cloud.aws;

import static com.sequenceiq.cloudbreak.cloud.model.CredentialStatus.PERMISSIONS_MISSING;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNoneEmpty;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.amazonaws.AmazonClientException;
import com.sequenceiq.cloudbreak.cloud.CredentialConnector;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsCredentialViewProvider;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredentialStatus;
import com.sequenceiq.cloudbreak.cloud.model.CredentialStatus;
import com.sequenceiq.cloudbreak.cloud.model.credential.CredentialVerificationContext;
import com.sequenceiq.cloudbreak.cloud.response.AwsCredentialPrerequisites;
import com.sequenceiq.cloudbreak.cloud.response.CredentialPrerequisitesResponse;
import com.sequenceiq.common.model.CredentialType;

@Service
public class AwsCredentialConnector implements CredentialConnector {

    static final String ROLE_IS_NOT_ASSUMABLE_ERROR_MESSAGE_INDICATOR = "is not authorized to perform: sts:AssumeRole on resource";

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsCredentialConnector.class);

    @Value("${cb.aws.account.id:}")
    private String accountId;

    @Inject
    private AwsSessionCredentialClient credentialClient;

    @Inject
    private AwsCredentialVerifier awsCredentialVerifier;

    @Inject
    private AwsPlatformParameters awsPlatformParameters;

    @Inject
    private AwsCredentialViewProvider credentialViewProvider;

    @Inject
    private AwsDefaultRegionSelector defaultRegionSelector;

    @Override
    public CloudCredentialStatus verify(AuthenticatedContext authenticatedContext, CredentialVerificationContext credentialVerificationContext) {
        CloudCredential credential = authenticatedContext.getCloudCredential();
        LOGGER.debug("Create credential: {}", credential);
        AwsCredentialView awsCredential = credentialViewProvider.createAwsCredentialView(credential);
        String roleArn = awsCredential.getRoleArn();
        String accessKey = awsCredential.getAccessKey();
        String secretKey = awsCredential.getSecretKey();

        CloudCredentialStatus result;
        if (isNoneEmpty(roleArn, accessKey, secretKey)) {
            String message = "Please only provide the 'role arn' or the 'access' and 'secret key'";
            result = new CloudCredentialStatus(credential, CredentialStatus.FAILED, new Exception(message), message);
        } else if (isNotEmpty(roleArn)) {
            result = verifyIamRoleIsAssumable(credential, credentialVerificationContext);
        } else if (isEmpty(accessKey) || isEmpty(secretKey)) {
            String message = "Please provide both the 'access' and 'secret key'";
            result = new CloudCredentialStatus(credential, CredentialStatus.FAILED, new Exception(message), message);
        } else {
            result = verifyAccessKeySecretKeyIsAssumable(credential);
        }
        return result;
    }

    @Override
    public CloudCredentialStatus create(AuthenticatedContext auth) {
        return new CloudCredentialStatus(auth.getCloudCredential(), CredentialStatus.CREATED);
    }

    @Override
    public CloudCredentialStatus delete(AuthenticatedContext auth) {
        return new CloudCredentialStatus(auth.getCloudCredential(), CredentialStatus.DELETED);
    }

    @Override
    public CredentialPrerequisitesResponse getPrerequisites(CloudContext cloudContext, String externalId, String deploymentAddress, CredentialType type) {
        String policyJson;
        switch (type) {
            case ENVIRONMENT:
                policyJson = awsPlatformParameters.getCredentialPoliciesJson();
                break;
            case AUDIT:
                policyJson = awsPlatformParameters.getAuditPoliciesJson();
                break;
            default:
                policyJson = null;
        }
        AwsCredentialPrerequisites awsPrerequisites = new AwsCredentialPrerequisites(externalId, policyJson);
        awsPrerequisites.setPolicies(Map.of("Environment", awsPlatformParameters.getEnvironmentMinimalPoliciesJson()));
        return new CredentialPrerequisitesResponse(cloudContext.getPlatform().value(), accountId, awsPrerequisites);
    }

    private CloudCredentialStatus verifyIamRoleIsAssumable(CloudCredential cloudCredential, CredentialVerificationContext credentialVerificationContext) {
        AwsCredentialView awsCredential = credentialViewProvider.createAwsCredentialView(cloudCredential);
        CloudCredentialStatus credentialStatus = new CloudCredentialStatus(cloudCredential, CredentialStatus.VERIFIED);
        try {
            credentialClient.retrieveSessionCredentials(awsCredential);
            checkRoleIsAssumableWithoutExternalId(credentialVerificationContext, awsCredential);
            credentialStatus = verifyCredentialsPermission(cloudCredential, awsCredential, credentialStatus);
            credentialStatus = determineDefaultRegion(cloudCredential, credentialStatus);
        } catch (AmazonClientException ae) {
            String errorMessage = getErrorMessageForAwsClientException(awsCredential, ae);
            LOGGER.warn(errorMessage, ae);
            credentialStatus = new CloudCredentialStatus(cloudCredential, CredentialStatus.FAILED, ae, errorMessage);
        } catch (AwsConfusedDeputyException confusedDeputyEx) {
            credentialStatus = new CloudCredentialStatus(cloudCredential, CredentialStatus.FAILED, confusedDeputyEx, confusedDeputyEx.getMessage());
        } catch (RuntimeException e) {
            String errorMessage = String.format("Unable to verify credential: check if the role '%s' exists and it's created with the correct external ID. " +
                            "Cause: '%s'", awsCredential.getRoleArn(), e.getMessage());
            LOGGER.warn(errorMessage, e);
            credentialStatus = new CloudCredentialStatus(cloudCredential, CredentialStatus.FAILED, e, errorMessage);
        }
        return credentialStatus;
    }

    private CloudCredentialStatus verifyCredentialsPermission(CloudCredential cloudCredential, AwsCredentialView awsCredential,
            CloudCredentialStatus credentialStatus) {
        if (cloudCredential.isVerifyPermissions()) {
            try {
                awsCredentialVerifier.validateAws(awsCredential);
            } catch (AwsPermissionMissingException e) {
                credentialStatus = new CloudCredentialStatus(cloudCredential, PERMISSIONS_MISSING, new Exception(e.getMessage()), e.getMessage());
            }
        }
        return credentialStatus;
    }

    private CloudCredentialStatus determineDefaultRegion(CloudCredential cloudCredential, CloudCredentialStatus credentialStatus) {
        boolean defaultRegionChanged = determineDefaultRegionViaDescribingRegions(cloudCredential);
        if (defaultRegionChanged) {
            credentialStatus = new CloudCredentialStatus(credentialStatus, defaultRegionChanged);
        }
        return credentialStatus;
    }

    private String getErrorMessageForAwsClientException(AwsCredentialView awsCredential, AmazonClientException ae) {
        String errorMessage = String.format("Unable to verify AWS credential due to: '%s'", ae.getMessage());
        if (ae.getMessage().contains("Unable to load AWS credentials")) {
            errorMessage = String.format("Unable to load AWS credentials: please make sure that you configured your assumer %s and %s to deployer.",
                    awsCredential.isGovernmentCloudEnabled() ? "AWS_GOV_ACCESS_KEY_ID" : "AWS_ACCESS_KEY_ID",
                    awsCredential.isGovernmentCloudEnabled() ? "AWS_GOV_SECRET_ACCESS_KEY" : "AWS_SECRET_ACCESS_KEY");
        } else if (ae.getMessage().contains(ROLE_IS_NOT_ASSUMABLE_ERROR_MESSAGE_INDICATOR)) {
            errorMessage = String.format("CDP Control Pane is not authorized to perform sts:AssumeRole on '%s' role", awsCredential.getRoleArn());
        }
        return errorMessage;
    }

    private CloudCredentialStatus verifyAccessKeySecretKeyIsAssumable(CloudCredential cloudCredential) {
        AwsCredentialView awsCredential = new AwsCredentialView(cloudCredential);
        CloudCredentialStatus credentialStatus = new CloudCredentialStatus(cloudCredential, CredentialStatus.VERIFIED);
        try {
            boolean defaultRegionChanged = determineDefaultRegionViaDescribingRegions(cloudCredential);
            credentialStatus = verifyCredentialsPermission(cloudCredential, awsCredential, credentialStatus);
            if (defaultRegionChanged) {
                credentialStatus = new CloudCredentialStatus(credentialStatus, defaultRegionChanged);
            }
        } catch (AmazonClientException ae) {
            String errorMessage = "Unable to verify AWS credentials: "
                    + "please make sure the access key and secret key is correct. "
                    + ae.getMessage();
            LOGGER.debug(errorMessage, ae);
            credentialStatus = new CloudCredentialStatus(cloudCredential, CredentialStatus.FAILED, ae, errorMessage);
        } catch (RuntimeException e) {
            String errorMessage = String.format("Could not verify keys '%s': check if the keys exists. %s",
                    awsCredential.getAccessKey(), e.getMessage());
            LOGGER.warn(errorMessage, e);
            credentialStatus = new CloudCredentialStatus(cloudCredential, CredentialStatus.FAILED, e, errorMessage);
        }
        return credentialStatus;
    }

    private void checkRoleIsAssumableWithoutExternalId(CredentialVerificationContext credentialVerificationContext, AwsCredentialView awsCredential) {
        if (credentialVerificationContext.getCreationVerification()) {
            String roleArn = awsCredential.getRoleArn();
            try {
                credentialClient.retrieveSessionCredentialsWithoutExternalId(awsCredential);
                String message = String.format("CDP Control Pane is able to perform 'sts:AssumeRole' on '%s' role without external id. " +
                        "The role is vulnerable and could be exploited by confused deputy attacks. " +
                        "Please update the role's trust relationship to have a condition on external id " +
                        "or re-create the role with external id configured in the create role wizard.", roleArn);
                LOGGER.warn(message);
                throw new AwsConfusedDeputyException(message);
            } catch (AmazonClientException ae) {
                if (ae.getMessage().contains(ROLE_IS_NOT_ASSUMABLE_ERROR_MESSAGE_INDICATOR)) {
                    String msg = String.format("Consider the specified role as secured. " +
                            "CDP Control Pane is not authorized to perform sts:AssumeRole on '%s' role without external Id.", roleArn);
                    LOGGER.info(msg);
                } else {
                    throw ae;
                }
            }
        }
    }

    private boolean determineDefaultRegionViaDescribingRegions(CloudCredential credential) {
        boolean defaultRegionChanged = false;
        String defaultRegion = defaultRegionSelector.determineDefaultRegion(credential);
        if (StringUtils.isNoneEmpty(defaultRegion)) {
            LOGGER.debug("New default region '{}' has been selected for the credential.", defaultRegion);
            credential.getParameter(AwsCredentialView.AWS, Map.class).put(AwsCredentialView.DEFAULT_REGION_KEY, defaultRegion);
            defaultRegionChanged = true;
        }
        return defaultRegionChanged;
    }
}
