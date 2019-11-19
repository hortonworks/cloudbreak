package com.sequenceiq.cloudbreak.cloud.aws.validator;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.amazonaws.auth.policy.actions.SecurityTokenServiceActions;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.model.EvaluationResult;
import com.amazonaws.services.identitymanagement.model.InstanceProfile;
import com.amazonaws.services.identitymanagement.model.PolicyEvaluationDecisionType;
import com.amazonaws.services.identitymanagement.model.Role;
import com.sequenceiq.cloudbreak.cloud.aws.util.AwsIamService;
import com.sequenceiq.cloudbreak.validation.ValidationResult.ValidationResultBuilder;

@Component
public class AwsIDBrokerAssumeRoleValidator {
    static final Collection<String> ASSUME_ROLE_ACTION = Collections.singletonList(
            SecurityTokenServiceActions.AssumeRole.getActionName());

    @Inject
    private AwsIamService awsIamService;

    public boolean canAssumeRoles(AmazonIdentityManagement iam, InstanceProfile instanceProfile,
            Collection<Role> roles, ValidationResultBuilder resultBuilder) {
        SortedSet<String> failedRoles = new TreeSet<>();
        for (Role role : roles) {
            if (!canAssumeRole(iam, instanceProfile, role)) {
                failedRoles.add(role.getArn());
            }
        }
        if (failedRoles.isEmpty()) {
            return true;
        } else {
            resultBuilder.error(
                    String.format("IDBroker instance profile (%s) doesn't have permissions to assume " +
                            "the role(s): %s", instanceProfile.getArn(), failedRoles));
            return false;
        }
    }

    boolean canAssumeRole(AmazonIdentityManagement iam, InstanceProfile instanceProfile, Role role) {
        for (Role instanceProfileRole : instanceProfile.getRoles()) {
            Collection<String> resources = Collections.singletonList(role.getArn());
            List<EvaluationResult> evaluationResults = awsIamService.simulatePrincipalPolicy(iam,
                    instanceProfileRole.getArn(), ASSUME_ROLE_ACTION, resources);
            for (EvaluationResult evaluationResult : evaluationResults) {
                if (PolicyEvaluationDecisionType.Allowed.toString()
                        .equals(evaluationResult.getEvalDecision())) {
                    return true;
                }
            }
        }
        return false;
    }
}
