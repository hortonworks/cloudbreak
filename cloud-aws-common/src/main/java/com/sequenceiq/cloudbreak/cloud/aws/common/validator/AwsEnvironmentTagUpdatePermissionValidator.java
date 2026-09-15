package com.sequenceiq.cloudbreak.cloud.aws.common.validator;

import static com.sequenceiq.cloudbreak.cloud.aws.common.AwsSdkErrorCodes.ACCESS_DENIED;

import java.util.List;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.EnvironmentTagUpdatePermissionValidator;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsCredentialVerifier;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsPlatformParameters;
import com.sequenceiq.cloudbreak.cloud.aws.common.PolicyType;
import com.sequenceiq.cloudbreak.cloud.aws.common.exception.AwsPermissionMissingException;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialViewProvider;
import com.sequenceiq.cloudbreak.cloud.exception.TagUpdatePermissionMissingException;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;

import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.awscore.exception.AwsServiceException;

@Component
public class AwsEnvironmentTagUpdatePermissionValidator implements EnvironmentTagUpdatePermissionValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsEnvironmentTagUpdatePermissionValidator.class);

    private static final String SIMULATE_ACTION = "iam:SimulatePrincipalPolicy";

    @Inject
    private AwsPlatformParameters awsPlatformParameters;

    @Inject
    private AwsCredentialVerifier awsCredentialVerifier;

    @Inject
    private AwsCredentialViewProvider credentialViewProvider;

    @Override
    public String supportedPlatform() {
        return CloudPlatform.AWS.name();
    }

    @Override
    public void validate(CloudCredential cloudCredential) throws TagUpdatePermissionMissingException {
        AwsCredentialView awsCredential = credentialViewProvider.createAwsCredentialView(cloudCredential);
        PolicyType policyType = awsCredential.isGovernmentCloudEnabled() ? PolicyType.GOV : PolicyType.PUBLIC;
        String policyJson = awsPlatformParameters.getCdpTagUpdatePolicyJson().get(policyType);
        try {
            awsCredentialVerifier.validateAws(awsCredential, policyJson);
        } catch (AwsPermissionMissingException e) {
            LOGGER.info("AWS tag-update permission check failed for credential '{}': {}", awsCredential.getName(), e.getMessage());
            throw new TagUpdatePermissionMissingException(e.getMessage(), e.getFailedActions(), e);
        } catch (AwsServiceException e) {
            throw toPrerequisiteException(awsCredential, e);
        }
    }

    /**
     * The simulation itself needs permissions: {@code iam:SimulatePrincipalPolicy} to evaluate the policy and
     * {@code sts:GetCallerIdentity} to resolve the principal ARN when the credential has no role ARN. Without
     * them the SDK raises an {@link AwsServiceException} instead of reporting denied actions, which would
     * escape as HTTP 500 and bypass the structured payload the UI reads. Translate it into the same
     * validation error so the caller learns which prerequisite to grant.
     */
    private TagUpdatePermissionMissingException toPrerequisiteException(AwsCredentialView awsCredential, AwsServiceException e) {
        AwsErrorDetails errorDetails = e.awsErrorDetails();
        String errorCode = errorDetails == null ? null : errorDetails.errorCode();
        if (ACCESS_DENIED.equals(errorCode)) {
            LOGGER.info("AWS tag-update permission check could not run for credential '{}', '{}' is denied: {}",
                    awsCredential.getName(), SIMULATE_ACTION, e.getMessage());
            return new TagUpdatePermissionMissingException(
                    String.format("CDP Credential '%s' doesn't have permission for these actions which are required to verify the remaining "
                                    + "tag-update permissions: [ %s : * ]. Grant it (and 'sts:GetCallerIdentity') to the credential's role, "
                                    + "then retry the tag update.", awsCredential.getName(), SIMULATE_ACTION),
                    List.of(SIMULATE_ACTION + " : *"), e);
        }
        LOGGER.warn("AWS tag-update permission check could not run for credential '{}' due to an AWS error (code '{}'): {}",
                awsCredential.getName(), errorCode, e.getMessage(), e);
        return new TagUpdatePermissionMissingException(
                String.format("Failed to verify the tag-update permissions of CDP Credential '%s': %s", awsCredential.getName(), e.getMessage()), e);
    }
}