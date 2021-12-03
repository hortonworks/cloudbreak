package com.sequenceiq.cloudbreak.cloud.aws.common.validator;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.amazonaws.auth.policy.Policy;
import com.amazonaws.services.identitymanagement.model.AmazonIdentityManagementException;
import com.amazonaws.services.identitymanagement.model.EvaluationResult;
import com.amazonaws.services.identitymanagement.model.InstanceProfile;
import com.amazonaws.services.identitymanagement.model.Role;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonIdentityManagementClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.util.AwsIamService;
import com.sequenceiq.cloudbreak.cloud.model.filesystem.CloudS3View;
import com.sequenceiq.cloudbreak.cloud.storage.LocationHelper;
import com.sequenceiq.cloudbreak.validation.ValidationResult.ValidationResultBuilder;

@Component
public class AwsLogRolePermissionValidator extends AbstractAwsSimulatePolicyValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsLogRolePermissionValidator.class);

    @Inject
    private AwsIamService awsIamService;

    @Inject
    private LocationHelper locationHelper;

    public void validate(AmazonIdentityManagementClient iam, InstanceProfile instanceProfile,
            CloudS3View cloudFileSystem, String logLocationBase, ValidationResultBuilder validationResultBuilder) {
        SortedSet<String> failedActions = new TreeSet<>();
        SortedSet<String> warnings = new TreeSet<>();
        if (logLocationBase == null) {
            return;
        }
        Map<String, String> replacements = Map.ofEntries(
                Map.entry("${LOGS_LOCATION_BASE}", removeProtocol(logLocationBase)),
                Map.entry("${LOGS_BUCKET}", locationHelper.parseS3BucketName(logLocationBase))
        );
        Policy policy = awsIamService.getPolicy("aws-cdp-log-policy.json", replacements);
        List<Role> roles = instanceProfile.getRoles();
        List<Policy> policies = Collections.singletonList(policy);
        for (Role role : roles) {
            try {
                List<EvaluationResult> evaluationResults = awsIamService.validateRolePolicies(iam,
                        role, policies);
                failedActions.addAll(getFailedActions(role, evaluationResults));
                warnings.addAll(getWarnings(role, evaluationResults));
            } catch (AmazonIdentityManagementException e) {
                // Only log the error and keep processing. Failed actions won't be added, but
                // processing doesn't get stopped either. This can happen due to rate limiting.
                LOGGER.error("Unable to validate role policies for role {} due to {}", role.getArn(),
                        e.getMessage(), e);
            }
        }

        if (!warnings.isEmpty()) {
            String validationWarningMessage = String.format("The validation of the Logger Instance Profile (%s) was not successful" +
                            " because there are missing context values (%s). This is not an issue in itself you might have an SCPs configured" +
                            " in your aws account and the system couldn't guess these extra parameters.",
                    String.join(", ", instanceProfile.getArn()),
                    String.join(", ", warnings)
            );
            LOGGER.info(validationWarningMessage);
            validationResultBuilder.warning(validationWarningMessage);
        }

        if (!failedActions.isEmpty()) {
            String validationErrorMessage = String.format("Logger Instance Profile (%s) is not set up correctly. " +
                            "Please follow the official documentation on required policies for Logger Instance Profile.\n" +
                            "Missing policies:%n%s",
                    String.join(", ", instanceProfile.getArn()),
                    String.join("\n", failedActions));

            LOGGER.info(validationErrorMessage);
            validationResultBuilder.error(validationErrorMessage);
        }
    }

    private String removeProtocol(String logLocationBase) {
        return logLocationBase.replaceFirst("^s3.?://", "");
    }

}
