package com.sequenceiq.cloudbreak.cloud.aws.validator;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.amazonaws.auth.policy.actions.SecurityTokenServiceActions;
import com.amazonaws.services.identitymanagement.model.AmazonIdentityManagementException;
import com.amazonaws.services.identitymanagement.model.EvaluationResult;
import com.amazonaws.services.identitymanagement.model.InstanceProfile;
import com.amazonaws.services.identitymanagement.model.PolicyEvaluationDecisionType;
import com.amazonaws.services.identitymanagement.model.Role;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonIdentityManagementClient;
import com.sequenceiq.cloudbreak.cloud.aws.util.AwsIamService;
import com.sequenceiq.cloudbreak.validation.ValidationResult.ValidationResultBuilder;

@Component
public class AwsIDBrokerAssumeRoleValidator {
    static final Collection<String> ASSUME_ROLE_ACTION = Collections.singletonList(
            SecurityTokenServiceActions.AssumeRole.getActionName());

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsIDBrokerAssumeRoleValidator.class);

    @Inject
    private AwsIamService awsIamService;

    public boolean canAssumeRoles(AmazonIdentityManagementClient iam, InstanceProfile instanceProfile,
            Collection<Role> roles, ValidationResultBuilder resultBuilder) {
        Collection<String> roleArns = roles.stream().map(Role::getArn).collect(Collectors.toCollection(TreeSet::new));

        for (Role instanceProfileRole : instanceProfile.getRoles()) {
            try {
                List<EvaluationResult> evaluationResults = awsIamService.simulatePrincipalPolicy(iam,
                        instanceProfileRole.getArn(), ASSUME_ROLE_ACTION, roleArns);
                for (EvaluationResult evaluationResult : evaluationResults) {
                    if (PolicyEvaluationDecisionType.Allowed.toString().equals(evaluationResult.getEvalDecision())) {
                        roleArns.remove(evaluationResult.getEvalResourceName());
                    }
                }
            } catch (AmazonIdentityManagementException e) {
                // Log the error and return true. We don't want to block if there is an IAM failure.
                // This can happen due to throttling or other issues.
                LOGGER.error("Unable to check assume role from instance profile {} for roles {} due to {}",
                        instanceProfile.getArn(), roleArns, e.getMessage(), e);
                return true;
            }
            if (roleArns.isEmpty()) {
                return true;
            }
        }

        if (roleArns.isEmpty()) {
            return true;
        } else {
            resultBuilder.error(
                    String.format("IDBroker instance profile (%s) doesn't have permissions to assume " +
                            "the role(s): %s", instanceProfile.getArn(), roleArns));
            return false;
        }
    }
}
