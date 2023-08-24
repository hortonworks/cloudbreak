package com.sequenceiq.cloudbreak.cloud.aws.common.validator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

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
import software.amazon.awssdk.services.iam.model.OrganizationsDecisionDetail;
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
        Role assumedRoles = Role.builder()
                .arn("arn:aws:iam::12345:role/AdminRole")
                .roleName("AdminRole").build();
        Collection<Role> roles = Collections.singletonList(assumedRoles);

        InstanceProfile instanceProfile = InstanceProfile.builder()
                .arn("arn:aws:iam::12345:instance-profile/idBrokerInstanceProfile")
                .roles(roles)
                .build();

        EvaluationResult evalResult = EvaluationResult.builder()
                .evalDecision(PolicyEvaluationDecisionType.ALLOWED)
                .organizationsDecisionDetail(OrganizationsDecisionDetail.builder().allowedByOrganizations(true).build())
                .evalResourceName(assumedRoles.arn()).build();
        EvaluationResult evalResult2 = EvaluationResult.builder()
                .evalDecision(PolicyEvaluationDecisionType.ALLOWED)
                .organizationsDecisionDetail(OrganizationsDecisionDetail.builder().allowedByOrganizations(true).build())
                .evalResourceName(assumedRoles.arn()).build();
        when(iam.simulatePrincipalPolicy(any()))
                .thenReturn(SimulatePrincipalPolicyResponse.builder().evaluationResults(evalResult, evalResult2).build());

        ValidationResultBuilder validationResultBuilder = new ValidationResultBuilder();
        assertThat(awsIDBrokerAssumeRoleValidator.canAssumeRoles(iam, instanceProfile, roles, false, validationResultBuilder)).isTrue();
        assertThat(validationResultBuilder.build().hasError()).isFalse();
    }

    @Test
    public void checkCannotAssumeRoleWhenOrgPolicyDenied() {
        Role assumedRole1 = Role.builder()
                .arn("arn:aws:iam::12345:role/LogRole")
                .roleName("LogRole").build();
        Role assumedRole2 = Role.builder()
                .arn("arn:aws:iam::12345:role/AdminRole")
                .roleName("AdminRole").build();
        Collection<Role> roles = List.of(assumedRole1, assumedRole2);

        InstanceProfile instanceProfile = InstanceProfile.builder()
                .arn("arn:aws:iam::12345:instance-profile/idBrokerInstanceProfile")
                .roles(roles)
                .build();

        EvaluationResult evalResult = EvaluationResult.builder()
                .evalActionName("s3:ReadObject")
                .evalDecision(PolicyEvaluationDecisionType.ALLOWED)
                .organizationsDecisionDetail(OrganizationsDecisionDetail.builder().allowedByOrganizations(true).build())
                .evalResourceName(assumedRole1.arn()).build();

        EvaluationResult evalResult2 = EvaluationResult.builder()
                .evalActionName("s3:PutObject")
                .evalDecision(PolicyEvaluationDecisionType.IMPLICIT_DENY)
                .organizationsDecisionDetail(OrganizationsDecisionDetail.builder().allowedByOrganizations(false).build())
                .evalResourceName(assumedRole2.arn()).build();
        when(iam.simulatePrincipalPolicy(any()))
                .thenReturn(SimulatePrincipalPolicyResponse.builder().evaluationResults(evalResult, evalResult2).build());

        ValidationResultBuilder validationResultBuilder = new ValidationResultBuilder();
        assertThat(awsIDBrokerAssumeRoleValidator.canAssumeRoles(iam, instanceProfile, roles, false, validationResultBuilder)).isFalse();
        ValidationResult validationResult = validationResultBuilder.build();
        assertThat(validationResult.hasError()).isTrue();
        assertThat(validationResult.getErrors()).isEqualTo(List.of(
                "Data Access Instance profile (arn:aws:iam::12345:instance-profile/idBrokerInstanceProfile) assume validation failed " +
                        "for the role(s): [arn:aws:iam::12345:role/AdminRole]. " +
                        "Please check if you've used the correct Instance profile when setting up Data Access.",
                        "Validation failed due to an Organizational Policy Deny rule when evaluating (s3:PutObject). " +
                        "It's possible bypass this validation by setting 'skipOrgPolicyDecisions' on the credentials settings page. " +
                        "Please note that this could result in other failures during cluster creation."));
    }

    @Test
    public void checkDeniedByOrgPolicyDeniedAssumeRoleWhenSkipOrgPolicyAdded() {
        Role assumedRole1 = Role.builder()
                .arn("arn:aws:iam::12345:role/LogRole")
                .roleName("LogRole").build();
        Role assumedRole2 = Role.builder()
                .arn("arn:aws:iam::12345:role/AdminRole")
                .roleName("AdminRole").build();
        Collection<Role> roles = List.of(assumedRole1, assumedRole2);

        InstanceProfile instanceProfile = InstanceProfile.builder()
                .arn("arn:aws:iam::12345:instance-profile/idBrokerInstanceProfile")
                .roles(roles)
                .build();

        EvaluationResult evalResult = EvaluationResult.builder()
                .evalActionName("s3:ReadObject")
                .evalDecision(PolicyEvaluationDecisionType.ALLOWED)
                .organizationsDecisionDetail(OrganizationsDecisionDetail.builder().allowedByOrganizations(true).build())
                .evalResourceName(assumedRole1.arn()).build();

        EvaluationResult evalResult2 = EvaluationResult.builder()
                .evalActionName("s3:PutObject")
                .evalDecision(PolicyEvaluationDecisionType.IMPLICIT_DENY)
                .organizationsDecisionDetail(OrganizationsDecisionDetail.builder().allowedByOrganizations(false).build())
                .evalResourceName(assumedRole2.arn()).build();
        when(iam.simulatePrincipalPolicy(any()))
                .thenReturn(SimulatePrincipalPolicyResponse.builder().evaluationResults(evalResult, evalResult2).build());

        ValidationResultBuilder validationResultBuilder = new ValidationResultBuilder();
        assertThat(awsIDBrokerAssumeRoleValidator.canAssumeRoles(iam, instanceProfile, roles, true, validationResultBuilder)).isTrue();
        ValidationResult validationResult = validationResultBuilder.build();
        assertThat(validationResult.hasError()).isFalse();
        assertThat(validationResult.getErrors().isEmpty()).isTrue();
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
        assertThat(awsIDBrokerAssumeRoleValidator.canAssumeRoles(iam, instanceProfile, roles, false,
                validationResultBuilder)).isFalse();
        ValidationResult validationResult = validationResultBuilder.build();
        assertThat(validationResult.hasError()).isTrue();
        assertThat(validationResult.getErrors()).isEqualTo(Collections.singletonList(
                String.format("Data Access Instance profile (%s) assume validation failed for the role(s): %s. " +
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
        assertThat(awsIDBrokerAssumeRoleValidator.canAssumeRoles(iam, instanceProfile, roles, false,
                validationResultBuilder)).isFalse();
        ValidationResult validationResult = validationResultBuilder.build();
        assertThat(validationResult.hasError()).isTrue();
        assertThat(validationResult.getErrors()).isEqualTo(Collections.singletonList(
                String.format("Data Access Instance profile (%s) assume validation failed for the role(s): %s. " +
                                "Please check if you've used the correct Instance profile when setting up Data Access.",
                        instanceProfile.arn(), Collections.singletonList(role2.arn()))));
    }
}
