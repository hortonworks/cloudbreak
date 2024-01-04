package com.sequenceiq.cloudbreak.cloud.aws.common.validator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonIdentityManagementClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.util.AwsIamService;
import com.sequenceiq.cloudbreak.cloud.model.BackupOperationType;
import com.sequenceiq.cloudbreak.cloud.model.filesystem.CloudS3View;
import com.sequenceiq.cloudbreak.validation.ValidationResult.ValidationResultBuilder;
import com.sequenceiq.common.api.cloudstorage.AccountMappingBase;
import com.sequenceiq.common.api.cloudstorage.StorageLocationBase;

import software.amazon.awssdk.core.auth.policy.Policy;
import software.amazon.awssdk.services.iam.model.EvaluationResult;
import software.amazon.awssdk.services.iam.model.IamException;
import software.amazon.awssdk.services.iam.model.Role;

public abstract class AwsIDBrokerMappedRolePermissionValidator extends AbstractAwsSimulatePolicyValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsIDBrokerMappedRolePermissionValidator.class);

    private static final int MAX_SIZE = 5;

    @Inject
    private AwsIamService awsIamService;

    @Inject
    private EntitlementService entitlementService;

    /**
     * Returns list of users to filter roles
     *
     * @return list of user strings
     */
    abstract Set<String> getUsers();

    /**
     * Returns policy file names
     *
     * @param s3guardEnabled true if s3guard is enabled false otherwise
     * @return list of policy file names
     */
    abstract List<String> getPolicyFileNames(boolean s3guardEnabled);

    /**
     * Returns the storage location base string
     *
     * @param location StorageLocationBase to evaluate
     * @return storage location base string
     */
    abstract String getStorageLocationBase(StorageLocationBase location);

    /**
     * Checks if the location should be checked or not for role permissions
     *
     * @param location StorageLocationBase to evaluate
     * @return true if location is valid to check false otherwise
     */
    abstract boolean checkLocation(StorageLocationBase location);

    /**
     * Validates the cloudFileSystem
     *
     * @param cloudFileSystem         cloud file system to evaluate
     * @param validationResultBuilder builder for any errors encountered
     */
    public void validate(AmazonIdentityManagementClient iam, CloudS3View cloudFileSystem, String backupLocation, String accountId,
            BackupOperationType backupOperationType, ValidationResultBuilder validationResultBuilder, boolean skipOrgPolicyDecisions) {
        AccountMappingBase accountMappings = cloudFileSystem.getAccountMapping();
        if (accountMappings != null) {
            SortedSet<String> roleArns = getRoleArnsForUsers(getUsers(), accountMappings.getUserMappings());
            LOGGER.info("Getting role from AWS, roleArns.size: {}, roleArns: {}", roleArns.size(), roleArns);
            Set<Role> roles = awsIamService.getValidRoles(iam, roleArns, validationResultBuilder);

            boolean s3guardEnabled = cloudFileSystem.getS3GuardDynamoTableName() != null;
            List<String> policyFileNames = getPolicyFileNames(s3guardEnabled);

            SortedSet<String> failedActions = new TreeSet<>();
            SortedSet<String> warnings = new TreeSet<>();
            List<Policy> policies = collectPolicies(cloudFileSystem, policyFileNames);
            if (shouldValidateBackupLocation(accountId, backupLocation)) {
                policies.addAll(collectBackupRestorePolicies(cloudFileSystem, backupOperationType, backupLocation));
            }
            for (Role role : roles) {
                try {
                    LOGGER.info("Permission validation on role: {} on locations {}", role.arn(),
                            Lists.newArrayList(getLocations(cloudFileSystem.getLocations()), backupLocation));
                    policies.forEach(p -> LOGGER.info("Policies being validated {}", p.toJson()));
                    List<EvaluationResult> evaluationResults = awsIamService.validateRolePolicies(iam, role, policies);
                    failedActions.addAll(getFailedActions(role, evaluationResults, skipOrgPolicyDecisions));
                    warnings.addAll(getWarnings(evaluationResults));
                } catch (IamException e) {
                    // Only log the error and keep processing. Failed actions won't be added, but
                    // processing doesn't get stopped either. This can happen due to rate limiting.
                    LOGGER.error("Unable to validate role policies for role {} due to {}", role.arn(), e.getMessage(), e);
                }
            }
            if (!warnings.isEmpty()) {
                String validationWarningMessage = String.format("The validation of the Data Access Role (%s) was not successful" +
                                " because there are missing context values (%s). This is not an issue in itself you might have an SCPs configured" +
                                " in your aws account and the system couldn't guess these extra parameters.",
                        String.join(", ", roles.stream().map(Role::arn).collect(Collectors.toCollection(TreeSet::new))),
                        String.join(", ", warnings)
                );
                LOGGER.info(validationWarningMessage);
                validationResultBuilder.warning(validationWarningMessage);
            }
            if (!failedActions.isEmpty()) {
                String validationErrorMessage = String.format("Data Access Role (%s) is not set up correctly. " +
                                "Please follow the official documentation on required policies for Data Access Role.%n" +
                                "Missing policies (chunked):%n%s",
                        String.join(", ", roles.stream().map(Role::arn).collect(Collectors.toCollection(TreeSet::new))),
                        String.join("\n", failedActions.stream().limit(MAX_SIZE).collect(Collectors.toSet())));

                if (validationErrorMessage.contains(AbstractAwsSimulatePolicyValidator.DENIED_BY_ORGANIZATION_RULE)) {
                    validationErrorMessage = validationErrorMessage.concat(DENIED_BY_ORGANIZATION_RULE_ERROR_MESSAGE);
                }

                String fullErrorMessage = String.format("Data Access Role (%s) is not set up correctly. Missing policies:%n%s",
                        String.join(", ", roles.stream().map(Role::arn).collect(Collectors.toCollection(TreeSet::new))),
                        String.join("\n", failedActions));

                LOGGER.info(fullErrorMessage);
                validationResultBuilder.error(validationErrorMessage);
            }
        }
    }

    List<Policy> collectPolicies(CloudS3View cloudFileSystem, List<String> policyFileNames) {
        List<Policy> policies = new ArrayList<>();
        for (StorageLocationBase location : cloudFileSystem.getLocations()) {
            if (checkLocation(location)) {
                Map<String, String> replacements = getPolicyJsonReplacements(location, cloudFileSystem);
                List<Policy> policiesWithReplacements = getPolicies(policyFileNames, replacements);
                policies.addAll(policiesWithReplacements);
            }
        }
        return policies;
    }

    /**
     * Collects the policies and applies the replacements on them
     * @param cloudFileSystem CloudFileSystem to get aws partition
     * @param backupLocation Location of backup
     * @return list of AWS policies that have to applied
     */
    List<Policy> collectBackupRestorePolicies(CloudS3View cloudFileSystem, BackupOperationType backupOperationType, String backupLocation) {
        List<Policy> policies = new ArrayList<>();
        if (!Strings.isNullOrEmpty(backupLocation)) {
            Map<String, String> replacements = getBackupPolicyJsonReplacements(cloudFileSystem, backupLocation);
            List<String> policyFileNames = getPolicyFiles(backupOperationType);
            policies.addAll(getPolicies(policyFileNames, replacements));
        }
        return policies;
    }

    List<String> getPolicyFiles(BackupOperationType backupOperationType) {
        if (backupOperationType == null) {
            return Collections.EMPTY_LIST;
        }
        switch (backupOperationType) {
            case ANY:
                return Arrays.asList(getBackupPolicy(), getRestorePolicy());
            case BACKUP:
                return Arrays.asList(getBackupPolicy());
            case RESTORE:
                return Arrays.asList(getRestorePolicy());
            default:
                return Collections.EMPTY_LIST;
        }
    }

    /**
     * Get the replacements necessary for the policy json
     *
     * @param location        StorageLocationBase to get storage paths from
     * @param cloudFileSystem CloudFileSystem to get dynamodb table name
     * @return map of simple replacements for policy json
     */
    Map<String, String> getPolicyJsonReplacements(StorageLocationBase location,
            CloudS3View cloudFileSystem) {
        String storageLocationBase = getStorageLocationBase(location);
        String datalakeBucket = storageLocationBase.split("/", 2)[0];
        String dynamodbTableName = cloudFileSystem.getS3GuardDynamoTableName() != null ?
                cloudFileSystem.getS3GuardDynamoTableName() : "";
        return Map.ofEntries(
                Map.entry("${ARN_PARTITION}", getAwsPartition(cloudFileSystem)),
                Map.entry("${STORAGE_LOCATION_BASE}", storageLocationBase),
                Map.entry("${DATALAKE_BUCKET}", datalakeBucket),
                Map.entry("${DYNAMODB_TABLE_NAME}", dynamodbTableName)
        );
    }

    /**
     * Retrieve the unique set of role arns for the list of provided users
     *
     * @param users        list of users to filter by
     * @param userMappings user mappings to filter on
     * @return set of AWS roles
     */
    SortedSet<String> getRoleArnsForUsers(Collection<String> users,
            Map<String, String> userMappings) {
        LOGGER.debug("Filter out specific users: {} from userMappings: {}", users, userMappings);
        return userMappings.entrySet().stream()
                .filter(m -> users.contains(m.getKey()))
                .map(Entry::getValue).collect(Collectors.toCollection(TreeSet::new));
    }

    /**
     * Returns Policy objects after the policy json has had replacements
     *
     * @param policyFileNames policy file names to read before replacement
     * @param replacements    simple replacements to be made to the policy json
     * @return list of Policy objects after replacements made to policy json
     */
    List<Policy> getPolicies(List<String> policyFileNames,
            Map<String, String> replacements) {
        List<Policy> policies = new ArrayList<>(policyFileNames.size());
        for (String policyFileName : policyFileNames) {
            Policy policy = awsIamService.getPolicy(policyFileName, replacements);
            if (policy != null) {
                policies.add(policy);
            }
        }
        return policies;
    }

    private List<String> getLocations(List<StorageLocationBase> storageLocationBases) {
        return storageLocationBases.stream().map(storageLocationBase -> getStorageLocationBase(storageLocationBase))
                .collect(Collectors.toList());
    }

    private boolean shouldValidateBackupLocation(String accountId, String backupLocation) {
        return (entitlementService.isDatalakeBackupRestorePrechecksEnabled(accountId)) &&
                !Strings.isNullOrEmpty(backupLocation);
    }
}
