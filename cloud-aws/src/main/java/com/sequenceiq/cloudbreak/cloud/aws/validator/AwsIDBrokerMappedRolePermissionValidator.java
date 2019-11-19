package com.sequenceiq.cloudbreak.cloud.aws.validator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.auth.policy.Policy;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.model.EvaluationResult;
import com.amazonaws.services.identitymanagement.model.Role;
import com.sequenceiq.cloudbreak.cloud.aws.util.AwsIamService;
import com.sequenceiq.cloudbreak.cloud.model.filesystem.CloudS3View;
import com.sequenceiq.cloudbreak.validation.ValidationResult.ValidationResultBuilder;
import com.sequenceiq.common.api.cloudstorage.AccountMappingBase;
import com.sequenceiq.common.api.cloudstorage.StorageLocationBase;

public abstract class AwsIDBrokerMappedRolePermissionValidator {
    private static final Logger LOGGER = LoggerFactory.getLogger(AwsIDBrokerMappedRolePermissionValidator.class);

    @Inject
    private AwsIamService awsIamService;

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
    public void validate(AmazonIdentityManagement iam, CloudS3View cloudFileSystem,
            ValidationResultBuilder validationResultBuilder) {
        AccountMappingBase accountMappings = cloudFileSystem.getAccountMapping();
        if (accountMappings != null) {
            SortedSet<String> roleArns = getRoleArnsForUsers(getUsers(), accountMappings.getUserMappings());
            Set<Role> roles = awsIamService.getValidRoles(iam, roleArns, validationResultBuilder);

            boolean s3guardEnabled = cloudFileSystem.getS3GuardDynamoTableName() != null;
            List<String> policyFileNames = getPolicyFileNames(s3guardEnabled);

            SortedSet<String> failedActions = new TreeSet<>();
            for (StorageLocationBase location : cloudFileSystem.getLocations()) {
                if (checkLocation(location)) {
                    Map<String, String> replacements = getPolicyJsonReplacements(location, cloudFileSystem);
                    List<Policy> policies = getPolicies(policyFileNames, replacements);
                    for (Role role : roles) {
                        List<EvaluationResult> evaluationResults = awsIamService.validateRolePolicies(iam,
                                role, policies);
                        failedActions.addAll(getFailedActions(role, evaluationResults));
                    }
                }
            }
            if (!failedActions.isEmpty()) {
                validationResultBuilder.error(String.format("The role(s) (%s) don't have the required permissions:%n%s",
                        String.join(", ", roles.stream().map(Role::getArn).collect(Collectors.toCollection(TreeSet::new))),
                        String.join("\n", failedActions)));
            }
        }
    }

    /**
     * Get the replacements necessary for the policy json
     *
     * @param location        StorageLocationBase to get storage paths from
     * @param cloudFileSystem CloudFileSystem to get dynamodb table name
     * @return map of simple replacements for policy json
     */
    protected Map<String, String> getPolicyJsonReplacements(StorageLocationBase location,
            CloudS3View cloudFileSystem) {
        String storageLocationBase = getStorageLocationBase(location);
        String datalakeBucket = storageLocationBase.split("/", 2)[0];
        String dynamodbTableName = cloudFileSystem.getS3GuardDynamoTableName() != null ?
                cloudFileSystem.getS3GuardDynamoTableName() : "";
        return Map.ofEntries(
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

    /**
     * Finds all the denied results and generates a set of failed actions
     *
     * @param role              Role that was being evaluated
     * @param evaluationResults result of simulating the policy
     */
    SortedSet<String> getFailedActions(Role role, List<EvaluationResult> evaluationResults) {
        return evaluationResults.stream()
                .filter(evaluationResult -> evaluationResult.getEvalDecision().toLowerCase().contains("deny"))
                .map(evaluationResult -> String.format("%s:%s:%s", role.getArn(),
                        evaluationResult.getEvalActionName(), evaluationResult.getEvalResourceName()))
                .collect(Collectors.toCollection(TreeSet::new));
    }
}
