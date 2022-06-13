package com.sequenceiq.cloudbreak.cloud.aws.common.validator;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import com.amazonaws.services.identitymanagement.model.EvaluationResult;
import com.amazonaws.services.identitymanagement.model.OrganizationsDecisionDetail;
import com.amazonaws.services.identitymanagement.model.Role;
import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.cloud.aws.common.util.Arn;
import com.sequenceiq.cloudbreak.cloud.model.filesystem.CloudS3View;
import com.sequenceiq.cloudbreak.cloud.storage.LocationHelper;

public abstract class AbstractAwsSimulatePolicyValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractAwsSimulatePolicyValidator.class);

    @Inject
    private LocationHelper locationHelper;

    SortedSet<String> getFailedActions(Role role, List<EvaluationResult> evaluationResults) {
        return evaluationResults.stream()
                .filter(this::isEvaluationFailed)
                .peek(evaluationResult -> LOGGER.debug("Aws EvaluationResult for the failed policy simulation: {}", evaluationResult))
                .map(evaluationResult -> {
                    OrganizationsDecisionDetail organizationsDecisionDetail = evaluationResult.getOrganizationsDecisionDetail();
                    if (organizationsDecisionDetail != null && !organizationsDecisionDetail.getAllowedByOrganizations()) {
                        return String.format("%s:%s:%s", role.getArn(),
                                evaluationResult.getEvalActionName(), evaluationResult.getEvalResourceName() + " -> Denied by Organization Rule");
                    } else {
                        return String.format("%s:%s:%s", role.getArn(),
                                evaluationResult.getEvalActionName(), evaluationResult.getEvalResourceName());
                    }
                })
                .collect(Collectors.toCollection(TreeSet::new));
    }

    SortedSet<String> getWarnings(Role role, List<EvaluationResult> evaluationResults) {
        return evaluationResults.stream()
                .filter(this::isEvaluationWarning)
                .map(evaluationResult -> String.format("missing context values: %s",
                        String.join(",", new HashSet<>(evaluationResult.getMissingContextValues()))))
                .collect(Collectors.toCollection(TreeSet::new));
    }

    private boolean isEvaluationFailed(EvaluationResult evaluationResult) {
        return evaluationResult.getEvalDecision().toLowerCase().contains("deny")
                && CollectionUtils.isEmpty(evaluationResult.getMissingContextValues());
    }

    private boolean isEvaluationWarning(EvaluationResult evaluationResult) {
        return evaluationResult.getEvalDecision().toLowerCase().contains("deny")
                && !CollectionUtils.isEmpty(evaluationResult.getMissingContextValues());
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
