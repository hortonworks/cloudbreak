package com.sequenceiq.cloudbreak.cloud.aws.validator;

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

import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.model.EvaluationResult;
import com.amazonaws.services.identitymanagement.model.InstanceProfile;
import com.amazonaws.services.identitymanagement.model.PolicyEvaluationDecisionType;
import com.amazonaws.services.identitymanagement.model.Role;
import com.amazonaws.services.identitymanagement.model.SimulatePrincipalPolicyRequest;
import com.amazonaws.services.identitymanagement.model.SimulatePrincipalPolicyResult;
import com.sequenceiq.cloudbreak.cloud.aws.util.AwsIamService;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.cloudbreak.validation.ValidationResult.ValidationResultBuilder;

@ExtendWith(MockitoExtension.class)
public class AwsIDBrokerAssumeRoleValidatorTest {
    @Spy
    private AwsIamService awsIamService;

    @Mock
    private AmazonIdentityManagement iam;

    @InjectMocks
    private AwsIDBrokerAssumeRoleValidator awsIDBrokerAssumeRoleValidator;

    @Test
    public void checkCanAssumeRoles() {
        Role instanceProfileRole = new Role();
        InstanceProfile instanceProfile = new InstanceProfile().withRoles(instanceProfileRole);

        Role role = new Role().withArn("roleArn");
        Collection<Role> roles = Collections.singletonList(role);

        EvaluationResult evalResult = new EvaluationResult()
                .withEvalDecision(PolicyEvaluationDecisionType.Allowed)
                .withEvalResourceName(role.getArn());
        when(iam.simulatePrincipalPolicy(any(SimulatePrincipalPolicyRequest.class)))
                .thenReturn(new SimulatePrincipalPolicyResult().withEvaluationResults(evalResult));

        ValidationResultBuilder validationResultBuilder = new ValidationResultBuilder();
        assertThat(awsIDBrokerAssumeRoleValidator.canAssumeRoles(iam, instanceProfile, roles,
                validationResultBuilder)).isTrue();
        assertThat(validationResultBuilder.build().hasError()).isFalse();
    }

    @Test
    public void checkCannotAssumeRoles() {
        Role instanceProfileRole = new Role();
        InstanceProfile instanceProfile = new InstanceProfile().withArn("instanceProfileArn")
                .withRoles(instanceProfileRole);

        Role role = new Role().withArn("roleArn");
        Collection<Role> roles = Collections.singletonList(role);

        EvaluationResult evalResult = new EvaluationResult()
                .withEvalDecision(PolicyEvaluationDecisionType.ImplicitDeny);
        when(iam.simulatePrincipalPolicy(any(SimulatePrincipalPolicyRequest.class)))
                .thenReturn(new SimulatePrincipalPolicyResult().withEvaluationResults(evalResult));

        ValidationResultBuilder validationResultBuilder = new ValidationResultBuilder();
        assertThat(awsIDBrokerAssumeRoleValidator.canAssumeRoles(iam, instanceProfile, roles,
                validationResultBuilder)).isFalse();
        ValidationResult validationResult = validationResultBuilder.build();
        assertThat(validationResult.hasError()).isTrue();
        assertThat(validationResult.getErrors()).isEqualTo(Collections.singletonList(
                String.format("IDBroker instance profile (%s) doesn't have permissions to assume the role(s): %s",
                        instanceProfile.getArn(), Collections.singletonList(role.getArn()))));
    }

    @Test
    public void checkCannotAssumeOneOfTheRoles() {
        Role instanceProfileRole = new Role();
        InstanceProfile instanceProfile = new InstanceProfile().withArn("instanceProfileArn")
                .withRoles(instanceProfileRole);

        Role role1 = new Role().withArn("role1Arn");
        Role role2 = new Role().withArn("role2Arn");
        Collection<Role> roles = Arrays.asList(role1, role2);

        EvaluationResult evalResult1 = new EvaluationResult()
                .withEvalDecision(PolicyEvaluationDecisionType.Allowed)
                .withEvalResourceName(role1.getArn());
        EvaluationResult evalResult2 = new EvaluationResult()
                .withEvalDecision(PolicyEvaluationDecisionType.ImplicitDeny)
                .withEvalResourceName(role2.getArn());
        when(iam.simulatePrincipalPolicy(any(SimulatePrincipalPolicyRequest.class)))
                .thenReturn(new SimulatePrincipalPolicyResult().withEvaluationResults(evalResult1))
                .thenReturn(new SimulatePrincipalPolicyResult().withEvaluationResults(evalResult2));

        ValidationResultBuilder validationResultBuilder = new ValidationResultBuilder();
        assertThat(awsIDBrokerAssumeRoleValidator.canAssumeRoles(iam, instanceProfile, roles,
                validationResultBuilder)).isFalse();
        ValidationResult validationResult = validationResultBuilder.build();
        assertThat(validationResult.hasError()).isTrue();
        assertThat(validationResult.getErrors()).isEqualTo(Collections.singletonList(
                String.format("IDBroker instance profile (%s) doesn't have permissions to assume the role(s): %s",
                        instanceProfile.getArn(), Collections.singletonList(role2.getArn()))));
    }
}
