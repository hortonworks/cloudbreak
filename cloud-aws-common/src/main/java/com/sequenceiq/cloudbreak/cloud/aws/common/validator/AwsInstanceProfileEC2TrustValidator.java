package com.sequenceiq.cloudbreak.cloud.aws.common.validator;

import static com.sequenceiq.cloudbreak.cloud.aws.common.AwsAccessConfigType.INSTANCE_PROFILE;
import static com.sequenceiq.cloudbreak.cloud.aws.common.util.AwsValidationMessageUtil.getAdviceMessage;

import java.util.List;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.aws.common.util.AwsIamService;
import com.sequenceiq.cloudbreak.validation.ValidationResult.ValidationResultBuilder;
import com.sequenceiq.common.model.CloudIdentityType;

import software.amazon.awssdk.core.auth.policy.Action;
import software.amazon.awssdk.core.auth.policy.Policy;
import software.amazon.awssdk.core.auth.policy.Principal;
import software.amazon.awssdk.core.auth.policy.Statement;
import software.amazon.awssdk.services.iam.model.InstanceProfile;
import software.amazon.awssdk.services.iam.model.Role;

@Component
public class AwsInstanceProfileEC2TrustValidator {

    @Inject
    private AwsIamService awsIamService;

    public boolean isTrusted(InstanceProfile instanceProfile, CloudIdentityType cloudIdentityType, ValidationResultBuilder resultBuilder) {
        List<Role> instanceProfileRoles = instanceProfile.roles();
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
                        instanceProfile.arn()));
        return false;
    }

    boolean checkEC2InPrincipals(List<Principal> principals) {
        return principals
                .stream()
                .anyMatch(principal -> "Service".equals(principal.getProvider())
                        && Principal.Service.AmazonEC2.getServiceId().equals(principal.getId()));
    }

    boolean checkAssumeRoleInActions(List<Action> actions) {
        return actions.stream().anyMatch(
                action -> "sts:AssumeRole".equals(action.getActionName()));
    }
}
