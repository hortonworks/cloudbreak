package com.sequenceiq.cloudbreak.cloud.aws.common.validator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonIdentityManagementClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.util.AwsIamService;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.cloudbreak.validation.ValidationResult.ValidationResultBuilder;

import software.amazon.awssdk.services.iam.model.EvaluationResult;
import software.amazon.awssdk.services.iam.model.InstanceProfile;
import software.amazon.awssdk.services.iam.model.PolicyEvaluationDecisionType;
import software.amazon.awssdk.services.iam.model.Role;
import software.amazon.awssdk.services.iam.model.SimulatePrincipalPolicyRequest;
import software.amazon.awssdk.services.iam.model.SimulatePrincipalPolicyResponse;

@ExtendWith(MockitoExtension.class)
public class AwsIDBrokerAssumeRoleValidatorTest {
    @Spy
    private AwsIamService awsIamService;

    @Mock
    private AmazonIdentityManagementClient iam;

    @InjectMocks
    private AwsIDBrokerAssumeRoleValidator awsIDBrokerAssumeRoleValidator;

    @Test
    public void checkCanAssumeRoles() {
        Role instanceProfileRole = Role.builder().build();
        InstanceProfile instanceProfile = InstanceProfile.builder().roles(instanceProfileRole).build();

        Role role = Role.builder().arn("roleArn").build();
        Collection<Role> roles = Collections.singletonList(role);

        EvaluationResult evalResult = EvaluationResult.builder()
                .evalDecision(PolicyEvaluationDecisionType.ALLOWED)
                .evalResourceName(role.arn()).build();
        when(iam.simulatePrincipalPolicy(any()))
                .thenReturn(SimulatePrincipalPolicyResponse.builder().evaluationResults(evalResult).build());

        ValidationResultBuilder validationResultBuilder = new ValidationResultBuilder();
        assertThat(awsIDBrokerAssumeRoleValidator.canAssumeRoles(iam, instanceProfile, roles, validationResultBuilder)).isTrue();
        assertThat(validationResultBuilder.build().hasError()).isFalse();
    }

    @Test
    public void checkCannotAssumeRoles() {
        Role instanceProfileRole = Role.builder().build();
        InstanceProfile instanceProfile = InstanceProfile.builder().arn("instanceProfileArn")
                .roles(instanceProfileRole).build();

        Role role = Role.builder().arn("roleArn").build();
        Collection<Role> roles = Collections.singletonList(role);

        EvaluationResult evalResult = EvaluationResult.builder()
                .evalDecision(PolicyEvaluationDecisionType.IMPLICIT_DENY).build();
        when(iam.simulatePrincipalPolicy(any(SimulatePrincipalPolicyRequest.class)))
                .thenReturn(SimulatePrincipalPolicyResponse.builder().evaluationResults(evalResult).build());

        ValidationResultBuilder validationResultBuilder = new ValidationResultBuilder();
        assertThat(awsIDBrokerAssumeRoleValidator.canAssumeRoles(iam, instanceProfile, roles,
                validationResultBuilder)).isFalse();
        ValidationResult validationResult = validationResultBuilder.build();
        assertThat(validationResult.hasError()).isTrue();
        assertThat(validationResult.getErrors()).isEqualTo(Collections.singletonList(
                String.format("Data Access Instance profile (%s) doesn't have permissions to assume the role(s): %s. " +
                                "Please check if you've used the correct Instance profile when setting up Data Access.",
                        instanceProfile.arn(), Collections.singletonList(role.arn()))));
    }

    @Test
    public void checkCannotAssumeOneOfTheRoles() {
        Role instanceProfileRole = Role.builder().build();
        InstanceProfile instanceProfile = InstanceProfile.builder().arn("instanceProfileArn")
                .roles(instanceProfileRole).build();

        Role role1 = Role.builder().arn("role1Arn").build();
        Role role2 = Role.builder().arn("role2Arn").build();
        Collection<Role> roles = Arrays.asList(role1, role2);

        EvaluationResult evalResult1 = EvaluationResult.builder()
                .evalDecision(PolicyEvaluationDecisionType.ALLOWED)
                .evalResourceName(role1.arn()).build();
        EvaluationResult evalResult2 = EvaluationResult.builder()
                .evalDecision(PolicyEvaluationDecisionType.IMPLICIT_DENY)
                .evalResourceName(role2.arn()).build();
        when(iam.simulatePrincipalPolicy(any(SimulatePrincipalPolicyRequest.class)))
                .thenReturn(SimulatePrincipalPolicyResponse.builder().evaluationResults(evalResult1).build())
                .thenReturn(SimulatePrincipalPolicyResponse.builder().evaluationResults(evalResult2).build());

        ValidationResultBuilder validationResultBuilder = new ValidationResultBuilder();
        assertThat(awsIDBrokerAssumeRoleValidator.canAssumeRoles(iam, instanceProfile, roles,
                validationResultBuilder)).isFalse();
        ValidationResult validationResult = validationResultBuilder.build();
        assertThat(validationResult.hasError()).isTrue();
        assertThat(validationResult.getErrors()).isEqualTo(Collections.singletonList(
                String.format("Data Access Instance profile (%s) doesn't have permissions to assume the role(s): %s. " +
                                "Please check if you've used the correct Instance profile when setting up Data Access.",
                        instanceProfile.arn(), Collections.singletonList(role2.arn()))));
    }
}
