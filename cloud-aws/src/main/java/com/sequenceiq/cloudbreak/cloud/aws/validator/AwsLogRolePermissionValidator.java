package com.sequenceiq.cloudbreak.cloud.aws.validator;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.amazonaws.auth.policy.Policy;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.model.EvaluationResult;
import com.amazonaws.services.identitymanagement.model.InstanceProfile;
import com.amazonaws.services.identitymanagement.model.Role;
import com.sequenceiq.cloudbreak.cloud.aws.util.AwsIamService;
import com.sequenceiq.cloudbreak.cloud.model.filesystem.CloudS3View;
import com.sequenceiq.cloudbreak.validation.ValidationResult.ValidationResultBuilder;

@Component
public class AwsLogRolePermissionValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsLogRolePermissionValidator.class);

    @Inject
    private AwsIamService awsIamService;

    public void validate(AmazonIdentityManagement iam, InstanceProfile instanceProfile,
            CloudS3View cloudFileSystem, ValidationResultBuilder validationResultBuilder) {
        SortedSet<String> failedActions = new TreeSet<>();

        // TODO need to figure out how to get LOGS_LOCATION_BASE value
        Map<String, String> replacements = Map.ofEntries(
                Map.entry("${LOGS_LOCATION_BASE}", "")
        );

        Policy policy = awsIamService.getPolicy("aws-cdp-log-policy.json", replacements);
        List<Role> roles = instanceProfile.getRoles();
        List<Policy> policies = Collections.singletonList(policy);
        for (Role role : roles) {
            List<EvaluationResult> evaluationResults = awsIamService.validateRolePolicies(iam, role,
                    policies);
            failedActions.addAll(getFailedActions(role, evaluationResults));
        }

        if (!failedActions.isEmpty()) {
            validationResultBuilder.error(String.format("The log role (%s) don't have the required permissions: %n%s",
                    String.join(", ", roles.stream().map(Role::getArn).collect(Collectors.toCollection(TreeSet::new))),
                    String.join("\n", failedActions)));
        }
    }

    /**
     * Finds all the denied results and generates a set of failed actions
     *
     * @param role              Role that was being evaluated
     * @param evaluationResults result of the simulate policy
     */
    SortedSet<String> getFailedActions(Role role, List<EvaluationResult> evaluationResults) {
        return evaluationResults.stream()
                .filter(evaluationResult -> evaluationResult.getEvalDecision().toLowerCase().contains("deny"))
                .map(evaluationResult -> String.format("%s:%s:%s", role.getArn(),
                        evaluationResult.getEvalActionName(), evaluationResult.getEvalResourceName()))
                .collect(Collectors.toCollection(TreeSet::new));
    }
}
