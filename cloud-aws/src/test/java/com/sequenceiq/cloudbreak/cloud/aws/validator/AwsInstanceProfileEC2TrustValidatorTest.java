package com.sequenceiq.cloudbreak.cloud.aws.validator;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.amazonaws.auth.policy.Policy;
import com.amazonaws.auth.policy.Principal;
import com.amazonaws.auth.policy.Principal.Services;
import com.amazonaws.auth.policy.Statement;
import com.amazonaws.auth.policy.Statement.Effect;
import com.amazonaws.auth.policy.actions.S3Actions;
import com.amazonaws.auth.policy.actions.SecurityTokenServiceActions;
import com.amazonaws.services.identitymanagement.model.InstanceProfile;
import com.amazonaws.services.identitymanagement.model.Role;
import com.sequenceiq.cloudbreak.cloud.aws.util.AwsIamService;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.cloudbreak.validation.ValidationResult.ValidationResultBuilder;

@ExtendWith(MockitoExtension.class)
public class AwsInstanceProfileEC2TrustValidatorTest {
    @Spy
    private AwsIamService awsIamService;

    @InjectMocks
    private AwsInstanceProfileEC2TrustValidator awsInstanceProfileEC2TrustValidator;

    @Test
    public void ec2NotInPrincipals() {
        assertThat(awsInstanceProfileEC2TrustValidator.checkEC2InPrincipals(
                Collections.singletonList(Principal.All))).isFalse();
        assertThat(awsInstanceProfileEC2TrustValidator.checkEC2InPrincipals(
                Collections.singletonList(Principal.AllServices))).isFalse();
        assertThat(awsInstanceProfileEC2TrustValidator.checkEC2InPrincipals(
                Collections.singletonList(new Principal("Service", "invalid")))).isFalse();
        assertThat(awsInstanceProfileEC2TrustValidator.checkEC2InPrincipals(
                Arrays.asList(
                        Principal.All,
                        Principal.AllServices,
                        new Principal("Service", "invalid")
                ))).isFalse();
    }

    @Test
    public void ec2InPrincipals() {
        assertThat(awsInstanceProfileEC2TrustValidator.checkEC2InPrincipals(Collections.singletonList(
                new Principal("Service", Services.AmazonEC2.getServiceId())))).isTrue();
        assertThat(awsInstanceProfileEC2TrustValidator.checkEC2InPrincipals(
                Arrays.asList(
                        Principal.AllServices,
                        new Principal("Service", Services.AmazonEC2.getServiceId())
                ))).isTrue();
    }

    @Test
    public void assumeRoleNotInActions() {
        assertThat(awsInstanceProfileEC2TrustValidator.checkAssumeRoleInActions(
                Collections.singletonList(S3Actions.CreateBucket))).isFalse();
        assertThat(awsInstanceProfileEC2TrustValidator.checkAssumeRoleInActions(
                Collections.singletonList(SecurityTokenServiceActions.AssumeRoleWithSAML))).isFalse();
        assertThat(awsInstanceProfileEC2TrustValidator.checkAssumeRoleInActions(
                Arrays.asList(
                        S3Actions.CreateBucket,
                        SecurityTokenServiceActions.AssumeRoleWithSAML
                ))).isFalse();
    }

    @Test
    public void assumeRoleInActions() {
        assertThat(awsInstanceProfileEC2TrustValidator.checkAssumeRoleInActions(
                Collections.singletonList(SecurityTokenServiceActions.AssumeRole))).isTrue();
        assertThat(awsInstanceProfileEC2TrustValidator.checkAssumeRoleInActions(
                Arrays.asList(
                        S3Actions.CreateBucket,
                        SecurityTokenServiceActions.AssumeRole
                ))).isTrue();
    }

    @Test
    public void invalidInstanceProfileTrustNoRole() {
        InstanceProfile instanceProfile = new InstanceProfile().withArn("noRoleArn");
        checkInvalidInstanceProfileTrust(instanceProfile);
    }

    @Test
    public void invalidInstanceProfileTrustOneRoleNoPolicy() {
        Role role = new Role().withArn("roleArn");
        InstanceProfile instanceProfile = new InstanceProfile().withArn("oneRoleNoPolicy")
                .withRoles(role);
        checkInvalidInstanceProfileTrust(instanceProfile);
    }

    @Test
    public void invalidInstanceProfileTrustOneRoleBadPolicy() {
        Role role = new Role().withArn("roleArn").withAssumeRolePolicyDocument("");
        InstanceProfile instanceProfile = new InstanceProfile().withArn("oneRoleBadPolicy")
                .withRoles(role);
        checkInvalidInstanceProfileTrust(instanceProfile);
    }

    @Test
    public void invalidInstanceProfileTrustOneRoleNoTrustPolicy() {
        Role role = new Role().withArn("roleArn").withAssumeRolePolicyDocument(new Policy().toJson());
        InstanceProfile instanceProfile = new InstanceProfile().withArn("oneRoleNoTrustPolicy")
                .withRoles(role);
        checkInvalidInstanceProfileTrust(instanceProfile);
    }

    @Test
    public void invalidInstanceProfileTrustMultipleRolesNoTrustPolicy() {
        Role role1 = new Role().withArn("roleArn").withAssumeRolePolicyDocument(new Policy().toJson());
        Role role2 = new Role().withArn("roleArn").withAssumeRolePolicyDocument(new Policy().toJson());
        InstanceProfile instanceProfile = new InstanceProfile().withArn("multipleRolesNoTrustPolicy")
                .withRoles(role1, role2);
        checkInvalidInstanceProfileTrust(instanceProfile);
    }

    @Test
    public void validInstanceProfileTrustOneRoleTrusted() {
        Policy trustedPolicy = getTrustedPolicy();
        Role role = new Role().withArn("roleArn").withAssumeRolePolicyDocument(trustedPolicy.toJson());
        InstanceProfile instanceProfile = new InstanceProfile().withArn("oneRoleTrusted")
                .withRoles(role);
        checkValidInstanceProfileTrust(instanceProfile);
    }

    @Test
    public void validInstanceProfileTrustMultipleRolesTrusted() {
        Policy untrustedPolicy = new Policy();
        Role role1 = new Role().withArn("roleArn").withAssumeRolePolicyDocument(untrustedPolicy.toJson());
        Policy trustedPolicy = getTrustedPolicy();
        Role role2 = new Role().withArn("roleArn").withAssumeRolePolicyDocument(trustedPolicy.toJson());
        InstanceProfile instanceProfile = new InstanceProfile().withArn("multipleRolesTrusted")
                .withRoles(role1, role2);
        checkValidInstanceProfileTrust(instanceProfile);
    }

    private Policy getTrustedPolicy() {
        return new Policy().withStatements(
                new Statement(Effect.Allow)
                        .withActions(SecurityTokenServiceActions.AssumeRole)
                        .withPrincipals(new Principal("Service", Services.AmazonEC2.getServiceId()))
        );
    }

    private void checkInvalidInstanceProfileTrust(InstanceProfile instanceProfile) {
        ValidationResultBuilder validationResultBuilder = new ValidationResultBuilder();
        assertThat(awsInstanceProfileEC2TrustValidator.isTrusted(instanceProfile,
                validationResultBuilder)).isFalse();
        ValidationResult validationResult = validationResultBuilder.build();
        assertThat(validationResult.hasError()).isTrue();
        assertThat(validationResult.getErrors()).isEqualTo(Collections.singletonList(
                String.format("The instance profile (%s) doesn't have an EC2 trust relationship.",
                        instanceProfile.getArn())));
    }

    private void checkValidInstanceProfileTrust(InstanceProfile instanceProfile) {
        ValidationResultBuilder validationResultBuilder = new ValidationResultBuilder();
        assertThat(awsInstanceProfileEC2TrustValidator.isTrusted(instanceProfile,
                validationResultBuilder)).isTrue();
        assertThat(validationResultBuilder.build().hasError()).isFalse();
    }
}
