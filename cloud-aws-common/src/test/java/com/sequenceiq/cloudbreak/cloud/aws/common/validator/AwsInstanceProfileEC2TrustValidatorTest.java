package com.sequenceiq.cloudbreak.cloud.aws.common.validator;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.aws.common.util.AwsIamService;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.cloudbreak.validation.ValidationResult.ValidationResultBuilder;
import com.sequenceiq.common.model.CloudIdentityType;

import software.amazon.awssdk.core.auth.policy.Action;
import software.amazon.awssdk.core.auth.policy.Policy;
import software.amazon.awssdk.core.auth.policy.Principal;
import software.amazon.awssdk.core.auth.policy.Principal.Service;
import software.amazon.awssdk.core.auth.policy.Statement;
import software.amazon.awssdk.services.iam.model.InstanceProfile;
import software.amazon.awssdk.services.iam.model.Role;

@ExtendWith(MockitoExtension.class)
public class AwsInstanceProfileEC2TrustValidatorTest {
    @Spy
    private AwsIamService awsIamService;

    @InjectMocks
    private AwsInstanceProfileEC2TrustValidator awsInstanceProfileEC2TrustValidator;

    @Test
    public void ec2NotInPrincipals() {
        assertThat(awsInstanceProfileEC2TrustValidator.checkEC2InPrincipals(
                Collections.singletonList(Principal.ALL))).isFalse();
        assertThat(awsInstanceProfileEC2TrustValidator.checkEC2InPrincipals(
                Collections.singletonList(Principal.ALL_SERVICES))).isFalse();
        assertThat(awsInstanceProfileEC2TrustValidator.checkEC2InPrincipals(
                Collections.singletonList(new Principal("Service", "invalid")))).isFalse();
        assertThat(awsInstanceProfileEC2TrustValidator.checkEC2InPrincipals(
                Arrays.asList(
                        Principal.ALL,
                        Principal.ALL_SERVICES,
                        new Principal("Service", "invalid")
                ))).isFalse();
    }

    @Test
    public void ec2InPrincipals() {
        assertThat(awsInstanceProfileEC2TrustValidator.checkEC2InPrincipals(Collections.singletonList(
                new Principal("Service", Service.AmazonEC2.getServiceId())))).isTrue();
        assertThat(awsInstanceProfileEC2TrustValidator.checkEC2InPrincipals(
                Arrays.asList(
                        Principal.ALL_SERVICES,
                        new Principal("Service", Service.AmazonEC2.getServiceId())
                ))).isTrue();
    }

    @Test
    public void assumeRoleNotInActions() {
        assertThat(awsInstanceProfileEC2TrustValidator.checkAssumeRoleInActions(
                Collections.singletonList(new Action("s3:CreateBucket")))).isFalse();
        assertThat(awsInstanceProfileEC2TrustValidator.checkAssumeRoleInActions(
                Collections.singletonList(new Action("sts:AssumeRoleWithSAML")))).isFalse();
        assertThat(awsInstanceProfileEC2TrustValidator.checkAssumeRoleInActions(
                Arrays.asList(
                        new Action("s3:CreateBucket"),
                        new Action("sts:AssumeRoleWithSAML")
                ))).isFalse();
    }

    @Test
    public void assumeRoleInActions() {
        assertThat(awsInstanceProfileEC2TrustValidator.checkAssumeRoleInActions(
                Collections.singletonList(new Action("sts:AssumeRole")))).isTrue();
        assertThat(awsInstanceProfileEC2TrustValidator.checkAssumeRoleInActions(
                Arrays.asList(
                        new Action("s3:CreateBucket"),
                        new Action("sts:AssumeRole")
                ))).isTrue();
    }

    @Test
    public void invalidInstanceProfileTrustNoRole() {
        InstanceProfile instanceProfile = InstanceProfile.builder().arn("noRoleArn").build();
        checkInvalidInstanceProfileTrust(instanceProfile);
    }

    @Test
    public void invalidInstanceProfileTrustOneRoleNoPolicy() {
        Role role = Role.builder().arn("roleArn").build();
        InstanceProfile instanceProfile = InstanceProfile.builder().arn("oneRoleNoPolicy")
                .roles(role).build();
        checkInvalidInstanceProfileTrust(instanceProfile);
    }

    @Test
    public void invalidInstanceProfileTrustOneRoleBadPolicy() {
        Role role = Role.builder().arn("roleArn").assumeRolePolicyDocument("").build();
        InstanceProfile instanceProfile = InstanceProfile.builder().arn("oneRoleBadPolicy")
                .roles(role).build();
        checkInvalidInstanceProfileTrust(instanceProfile);
    }

    @Test
    public void invalidInstanceProfileTrustOneRoleNoTrustPolicy() {
        Role role = Role.builder().arn("roleArn").assumeRolePolicyDocument(new Policy().toJson()).build();
        InstanceProfile instanceProfile = InstanceProfile.builder().arn("oneRoleNoTrustPolicy")
                .roles(role).build();
        checkInvalidInstanceProfileTrust(instanceProfile);
    }

    @Test
    public void invalidInstanceProfileTrustMultipleRolesNoTrustPolicy() {
        Role role1 = Role.builder().arn("roleArn").assumeRolePolicyDocument(new Policy().toJson()).build();
        Role role2 = Role.builder().arn("roleArn").assumeRolePolicyDocument(new Policy().toJson()).build();
        InstanceProfile instanceProfile = InstanceProfile.builder().arn("multipleRolesNoTrustPolicy")
                .roles(role1, role2).build();
        checkInvalidInstanceProfileTrust(instanceProfile);
    }

    @Test
    public void validInstanceProfileTrustOneRoleTrusted() {
        Policy trustedPolicy = getTrustedPolicy();
        Role role = Role.builder().arn("roleArn").assumeRolePolicyDocument(trustedPolicy.toJson()).build();
        InstanceProfile instanceProfile = InstanceProfile.builder().arn("oneRoleTrusted")
                .roles(role).build();
        checkValidInstanceProfileTrust(instanceProfile);
    }

    @Test
    public void validInstanceProfileTrustMultipleRolesTrusted() {
        Policy untrustedPolicy = new Policy();
        Role role1 = Role.builder().arn("roleArn").assumeRolePolicyDocument(untrustedPolicy.toJson()).build();
        Policy trustedPolicy = getTrustedPolicy();
        Role role2 = Role.builder().arn("roleArn").assumeRolePolicyDocument(trustedPolicy.toJson()).build();
        InstanceProfile instanceProfile = InstanceProfile.builder().arn("multipleRolesTrusted")
                .roles(role1, role2).build();
        checkValidInstanceProfileTrust(instanceProfile);
    }

    private Policy getTrustedPolicy() {
        return new Policy().withStatements(
                new Statement(Statement.Effect.Allow)
                        .withActions(new Action("sts:AssumeRole"))
                        .withPrincipals(new Principal("Service", Service.AmazonEC2.getServiceId()))
        );
    }

    private void checkInvalidInstanceProfileTrust(InstanceProfile instanceProfile) {
        ValidationResultBuilder validationResultBuilder = new ValidationResultBuilder();
        assertThat(awsInstanceProfileEC2TrustValidator.isTrusted(instanceProfile, CloudIdentityType.ID_BROKER,
                validationResultBuilder)).isFalse();
        ValidationResult validationResult = validationResultBuilder.build();
        assertThat(validationResult.hasError()).isTrue();
        assertThat(validationResult.getErrors()).isEqualTo(Collections.singletonList(
                String.format("The instance profile (%s) doesn't have an EC2 trust relationship. " +
                                "Please check if you've used the correct Instance profile when setting up Data Access.",
                        instanceProfile.arn())));
    }

    private void checkValidInstanceProfileTrust(InstanceProfile instanceProfile) {
        ValidationResultBuilder validationResultBuilder = new ValidationResultBuilder();
        assertThat(awsInstanceProfileEC2TrustValidator.isTrusted(instanceProfile, CloudIdentityType.ID_BROKER,
                validationResultBuilder)).isTrue();
        assertThat(validationResultBuilder.build().hasError()).isFalse();
    }
}
