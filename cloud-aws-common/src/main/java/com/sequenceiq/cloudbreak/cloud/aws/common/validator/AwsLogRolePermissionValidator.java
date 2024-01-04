package com.sequenceiq.cloudbreak.cloud.aws.common.validator;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonIdentityManagementClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.util.Arn;
import com.sequenceiq.cloudbreak.cloud.aws.common.util.AwsIamService;
import com.sequenceiq.cloudbreak.cloud.model.filesystem.CloudS3View;
import com.sequenceiq.cloudbreak.cloud.storage.LocationHelper;
import com.sequenceiq.cloudbreak.validation.ValidationResult.ValidationResultBuilder;
import com.sequenceiq.common.api.cloudstorage.StorageLocationBase;
import com.sequenceiq.common.model.FileSystemType;

import software.amazon.awssdk.core.auth.policy.Policy;
import software.amazon.awssdk.services.iam.model.EvaluationResult;
import software.amazon.awssdk.services.iam.model.IamException;
import software.amazon.awssdk.services.iam.model.InstanceProfile;
import software.amazon.awssdk.services.iam.model.Role;

@Component
public class AwsLogRolePermissionValidator extends AbstractAwsSimulatePolicyValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsLogRolePermissionValidator.class);

    @Inject
    private AwsIamService awsIamService;

    @Inject
    private LocationHelper locationHelper;

    public void validateLog(AmazonIdentityManagementClient iam, InstanceProfile instanceProfile, CloudS3View cloudFileSystem, String logsLocationBase,
            boolean skipOrgPolicyDecisions, ValidationResultBuilder validationResultBuilder) {
        if (Strings.isNullOrEmpty(logsLocationBase)) {
            return;
        }
        Arn instanceProfileArn = Arn.of(instanceProfile.arn());
        Map<String, String> logReplacements = Map.ofEntries(
                Map.entry("${ARN_PARTITION}", instanceProfileArn.getPartition()),
                Map.entry("${LOGS_LOCATION_BASE}", removeProtocol(logsLocationBase)),
                Map.entry("${LOGS_BUCKET}", locationHelper.parseS3BucketName(logsLocationBase))
        );
        Policy logPolicy = awsIamService.getPolicy("aws-cdp-log-policy.json", logReplacements);
        validate(iam, instanceProfile, cloudFileSystem, logsLocationBase, validationResultBuilder, logPolicy, skipOrgPolicyDecisions);
    }

    public void validateBackup(AmazonIdentityManagementClient iam, InstanceProfile instanceProfile,
            CloudS3View cloudFileSystem, String backupLocationBase, boolean skipOrgPolicyDecisions, ValidationResultBuilder validationResultBuilder) {
        Map<String, String> backupReplacements = getBackupPolicyJsonReplacements(cloudFileSystem, backupLocationBase);
        Policy restorePolicy = awsIamService.getPolicy(getRestorePolicy(), backupReplacements);
        validate(iam, instanceProfile, cloudFileSystem, backupLocationBase, validationResultBuilder, restorePolicy, skipOrgPolicyDecisions);
    }

    public void validate(AmazonIdentityManagementClient iam, InstanceProfile instanceProfile,
            CloudS3View cloudFileSystem, String locationBase, ValidationResultBuilder validationResultBuilder, Policy policy, boolean skipOrgPolicyDecisions) {
        SortedSet<String> failedActions = new TreeSet<>();
        SortedSet<String> warnings = new TreeSet<>();
        if (locationBase == null) {
            return;
        }
        List<Role> roles = instanceProfile.roles();
        List<Policy> policies = Collections.singletonList(policy);
        for (Role role : roles) {
            try {
                LOGGER.info("Permission validation on role: {} on locations {}", role.arn(),
                        Lists.newArrayList(getLocations(cloudFileSystem.getLocations()), locationBase));
                policies.forEach(p -> LOGGER.info("Policies being validated {}", p.toJson()));
                List<EvaluationResult> evaluationResults = awsIamService.validateRolePolicies(iam, role, policies);
                failedActions.addAll(getFailedActions(role, evaluationResults, skipOrgPolicyDecisions));
                warnings.addAll(getWarnings(evaluationResults));
            } catch (IamException e) {
                // Only log the error and keep processing. Failed actions won't be added, but
                // processing doesn't get stopped either. This can happen due to rate limiting.
                LOGGER.error("Unable to validate role policies for role {} due to {}", role.arn(),
                        e.getMessage(), e);
            }
        }

        if (!warnings.isEmpty()) {
            String validationWarningMessage = String.format("The validation of the Logger Instance Profile (%s) was not successful" +
                            " because there are missing context values (%s). This is not an issue in itself you might have an SCPs configured" +
                            " in your aws account and the system couldn't guess these extra parameters.",
                    String.join(", ", instanceProfile.arn()),
                    String.join(", ", warnings)
            );
            LOGGER.info(validationWarningMessage);
            validationResultBuilder.warning(validationWarningMessage);
        }

        if (!failedActions.isEmpty()) {
            String validationErrorMessage = String.format("Logger Instance Profile (%s) is not set up correctly. " +
                            "Please follow the official documentation on required policies for Logger Instance Profile.\n" +
                            "Missing policies:%n%s",
                    String.join(", ", instanceProfile.arn()),
                    String.join("\n", failedActions));

            if (validationErrorMessage.contains(AbstractAwsSimulatePolicyValidator.DENIED_BY_ORGANIZATION_RULE)) {
                validationErrorMessage = validationErrorMessage.concat(DENIED_BY_ORGANIZATION_RULE_ERROR_MESSAGE);
            }

            LOGGER.info(validationErrorMessage);
            validationResultBuilder.error(validationErrorMessage);
        }
    }

    private List<String> getLocations(List<StorageLocationBase> storageLocationBases) {
        return storageLocationBases.stream()
                .map(this::getStorageLocationBase)
                .collect(Collectors.toList());
    }

    private String getStorageLocationBase(StorageLocationBase location) {
        return location.getValue().replace(FileSystemType.S3.getProtocol() + "://", "");
    }
}
