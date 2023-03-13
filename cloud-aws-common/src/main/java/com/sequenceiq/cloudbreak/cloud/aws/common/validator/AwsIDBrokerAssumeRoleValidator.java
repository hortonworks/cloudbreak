package com.sequenceiq.cloudbreak.cloud.aws.common.validator;

import static com.sequenceiq.cloudbreak.cloud.aws.common.AwsAccessConfigType.INSTANCE_PROFILE;
import static com.sequenceiq.cloudbreak.cloud.aws.common.util.AwsValidationMessageUtil.getAdviceMessage;
import static com.sequenceiq.common.model.CloudIdentityType.ID_BROKER;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonIdentityManagementClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.util.AwsIamService;
import com.sequenceiq.cloudbreak.validation.ValidationResult.ValidationResultBuilder;

import software.amazon.awssdk.services.iam.model.EvaluationResult;
import software.amazon.awssdk.services.iam.model.IamException;
import software.amazon.awssdk.services.iam.model.InstanceProfile;
import software.amazon.awssdk.services.iam.model.PolicyEvaluationDecisionType;
import software.amazon.awssdk.services.iam.model.Role;

@Component
public class AwsIDBrokerAssumeRoleValidator {

    private static final Collection<String> ASSUME_ROLE_ACTION = Collections.singletonList("sts:AssumeRole");

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsIDBrokerAssumeRoleValidator.class);

    @Inject
    private AwsIamService awsIamService;

    public boolean canAssumeRoles(AmazonIdentityManagementClient iam, InstanceProfile instanceProfile,
            Collection<Role> roles, ValidationResultBuilder resultBuilder) {
        Collection<String> roleArns = roles.stream().map(Role::arn).collect(Collectors.toCollection(TreeSet::new));

        for (Role instanceProfileRole : instanceProfile.roles()) {
            try {
                List<EvaluationResult> evaluationResults = awsIamService.simulatePrincipalPolicy(iam,
                        instanceProfileRole.arn(), ASSUME_ROLE_ACTION, roleArns);
                for (EvaluationResult evaluationResult : evaluationResults) {
                    if (PolicyEvaluationDecisionType.ALLOWED.toString().equals(evaluationResult.evalDecision().toString())) {
                        roleArns.remove(evaluationResult.evalResourceName());
                    }
                }
            } catch (IamException e) {
                // Log the error and return true. We don't want to block if there is an IAM failure.
                // This can happen due to throttling or other issues.
                // If error messages access denied we add the error to the result
                LOGGER.error("Unable to check assume role from instance profile {} for roles {} due to {}",
                        instanceProfile.arn(), roleArns, e.getMessage(), e);
                if ("AccessDenied".equals(e.awsErrorDetails().errorCode())) {
                    resultBuilder.error(String.format("Unable to check assume role from Instance profile %s from roles %s because access is denied.",
                            instanceProfile.arn(), roleArns));
                    return false;
                }
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
                    String.format("Data Access Instance profile (%s) doesn't have permissions to assume " +
                                    "the role(s): %s. %s",
                            instanceProfile.arn(), roleArns, getAdviceMessage(INSTANCE_PROFILE, ID_BROKER)));
            return false;
        }
    }
}
