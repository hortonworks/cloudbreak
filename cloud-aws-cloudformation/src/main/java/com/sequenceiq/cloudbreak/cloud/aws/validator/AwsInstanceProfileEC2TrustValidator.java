package com.sequenceiq.cloudbreak.cloud.aws.validator;

import static com.sequenceiq.cloudbreak.cloud.aws.common.AwsAccessConfigType.INSTANCE_PROFILE;
import static com.sequenceiq.cloudbreak.cloud.aws.common.util.AwsValidationMessageUtil.getAdviceMessage;

import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.amazonaws.auth.policy.Action;
import com.amazonaws.auth.policy.Policy;
import com.amazonaws.auth.policy.Principal;
import com.amazonaws.auth.policy.Principal.Services;
import com.amazonaws.auth.policy.Statement;
import com.amazonaws.auth.policy.actions.SecurityTokenServiceActions;
import com.amazonaws.services.identitymanagement.model.InstanceProfile;
import com.amazonaws.services.identitymanagement.model.Role;
import com.sequenceiq.cloudbreak.cloud.aws.common.util.AwsIamService;
import com.sequenceiq.cloudbreak.validation.ValidationResult.ValidationResultBuilder;
import com.sequenceiq.common.model.CloudIdentityType;

@Component
public class AwsInstanceProfileEC2TrustValidator {
    @Inject
    private AwsIamService awsIamService;

    public boolean isTrusted(InstanceProfile instanceProfile, CloudIdentityType cloudIdentityType, ValidationResultBuilder resultBuilder) {
        List<Role> instanceProfileRoles = instanceProfile.getRoles();
        for (Role role : instanceProfileRoles) {
            Policy assumeRolePolicy = awsIamService.getAssumeRolePolicy(role);
            if (assumeRolePolicy != null) {
                for (Statement statement : assumeRolePolicy.getStatements()) {
                    if (checkAssumeRoleInActions(statement.getActions()) &&
                            checkEC2InPrincipals(statement.getPrincipals())) {
                        return true;
                    }
                }
            }
        }
        resultBuilder.error(
                String.format("The instance profile (%s) doesn't have an EC2 trust relationship. " +
                                getAdviceMessage(INSTANCE_PROFILE, cloudIdentityType),
                        instanceProfile.getArn()));
        return false;
    }

    boolean checkEC2InPrincipals(List<Principal> principals) {
        return principals
                .stream()
                .anyMatch(principal -> "Service".equals(principal.getProvider())
                        && Services.AmazonEC2.getServiceId().equals(principal.getId()));
    }

    boolean checkAssumeRoleInActions(List<Action> actions) {
        return actions.stream().anyMatch(
                action -> SecurityTokenServiceActions.AssumeRole.getActionName().equals(action.getActionName()));
    }
}
