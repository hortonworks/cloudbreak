package com.sequenceiq.cloudbreak.cloud.aws.common.validator;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.cloud.aws.common.util.Arn;
import com.sequenceiq.cloudbreak.cloud.model.filesystem.CloudS3View;
import com.sequenceiq.cloudbreak.cloud.storage.LocationHelper;

import software.amazon.awssdk.services.iam.model.EvaluationResult;
import software.amazon.awssdk.services.iam.model.OrganizationsDecisionDetail;
import software.amazon.awssdk.services.iam.model.Role;

public abstract class AbstractAwsSimulatePolicyValidator {

    public static final String DENIED_BY_ORGANIZATION_RULE = " -> Denied by Organization Rule";

    public static final String DENIED_BY_ORGANIZATION_RULE_ERROR_MESSAGE = "\nPlease note SCPs with global condition keys and whitelisted accounts are " +
            "not supported in the AWS Policy Simulator and may cause validation to fail. Please check the SCPs! You can skip organizational policy" +
            " related errors at credential settings, but please note that this may hide valid errors!";

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractAwsSimulatePolicyValidator.class);

    @Inject
    private LocationHelper locationHelper;

    SortedSet<String> getFailedActions(Role role, List<EvaluationResult> evaluationResults, boolean skipOrgPolicyDecisions) {
        LOGGER.debug("Collect policy simulation results, skipOrgPolicyDecisions: {}", skipOrgPolicyDecisions);
        return evaluationResults.stream()
                .filter(this::isEvaluationFailed)
                .filter(evaluationResult -> shouldCheckEvaluationResult(evaluationResult, skipOrgPolicyDecisions))
                .peek(evaluationResult -> LOGGER.debug("Aws EvaluationResult for the failed policy simulation: {}", evaluationResult))
                .map(evaluationResult -> {
                    OrganizationsDecisionDetail organizationsDecisionDetail = evaluationResult.organizationsDecisionDetail();
                    if (organizationsDecisionDetail != null && !organizationsDecisionDetail.allowedByOrganizations()) {
                        return String.format("%s:%s:%s", role.arn(),
                                evaluationResult.evalActionName(), evaluationResult.evalResourceName() + DENIED_BY_ORGANIZATION_RULE);
                    } else {
                        return String.format("%s:%s:%s", role.arn(),
                                evaluationResult.evalActionName(), evaluationResult.evalResourceName());
                    }
                })
                .collect(Collectors.toCollection(TreeSet::new));
    }

    boolean shouldCheckEvaluationResult(EvaluationResult evaluationResult, boolean skipOrgPolicyDecisions) {
        return !skipOrgPolicyDecisions || !shouldSkipOrgPolicyDeny(evaluationResult, skipOrgPolicyDecisions);
    }

    boolean shouldSkipOrgPolicyDeny(EvaluationResult evaluationResult, boolean skipOrgPolicyDecisions) {
        return skipOrgPolicyDecisions
                && evaluationResult.organizationsDecisionDetail() != null
                && !evaluationResult.organizationsDecisionDetail().allowedByOrganizations();
    }

    SortedSet<String> getWarnings(List<EvaluationResult> evaluationResults) {
        return evaluationResults.stream()
                .filter(this::isEvaluationWarning)
                .map(evaluationResult -> String.format("missing context values: %s",
                        String.join(",", new HashSet<>(evaluationResult.missingContextValues()))))
                .collect(Collectors.toCollection(TreeSet::new));
    }

    private boolean isEvaluationFailed(EvaluationResult evaluationResult) {
        return evaluationResult.evalDecisionAsString().toLowerCase(Locale.ROOT).contains("deny")
                && CollectionUtils.isEmpty(evaluationResult.missingContextValues());
    }

    private boolean isEvaluationWarning(EvaluationResult evaluationResult) {
        return evaluationResult.evalDecisionAsString().toLowerCase(Locale.ROOT).contains("deny")
                && !CollectionUtils.isEmpty(evaluationResult.missingContextValues());
    }

    protected String getBackupPolicy() {
        return "aws-datalake-backup-policy.json";
    }

    protected String getRestorePolicy() {
        return "aws-datalake-restore-policy.json";
    }

    protected String removeProtocol(String logLocationBase) {
        return logLocationBase.replaceFirst("^s3.?://", "");
    }

    /**
     * Get the replacements necessary for the backup/restore policy json's
     *
     * @param cloudFileSystem CloudFileSystem to get aws partition
     * @param backupLocation  location where backups are stored.
     * @return map of simple replacements for policy json
     */
    protected Map<String, String> getBackupPolicyJsonReplacements(CloudS3View cloudFileSystem,
            String backupLocation) {
        Map<String, String> replacements = Collections.emptyMap();
        if (!Strings.isNullOrEmpty(backupLocation)) {
            replacements = Map.ofEntries(
                    Map.entry("${ARN_PARTITION}", getAwsPartition(cloudFileSystem)),
                    Map.entry("${BACKUP_LOCATION_BASE}", removeProtocol(backupLocation)),
                    Map.entry("${BACKUP_BUCKET}", locationHelper.parseS3BucketName(backupLocation))
            );
        }
        return replacements;
    }

    protected String getAwsPartition(CloudS3View cloudFileSystem) {
        return Optional.ofNullable(cloudFileSystem)
                .map(CloudS3View::getInstanceProfile)
                .map(Arn::of)
                .map(Arn::getPartition)
                .orElse("aws");
    }

}
