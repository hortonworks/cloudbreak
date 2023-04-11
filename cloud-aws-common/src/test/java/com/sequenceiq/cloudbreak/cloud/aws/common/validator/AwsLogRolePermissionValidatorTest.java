package com.sequenceiq.cloudbreak.cloud.aws.common.validator;

import static com.sequenceiq.cloudbreak.cloud.aws.common.validator.AbstractAwsSimulatePolicyValidator.DENIED_BY_ORGANIZATION_RULE_ERROR_MESSAGE;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonIdentityManagementClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.util.AwsIamService;
import com.sequenceiq.cloudbreak.cloud.model.filesystem.CloudS3View;
import com.sequenceiq.cloudbreak.cloud.storage.LocationHelper;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.cloudbreak.validation.ValidationResult.ValidationResultBuilder;
import com.sequenceiq.common.api.cloudstorage.StorageLocationBase;
import com.sequenceiq.common.model.CloudIdentityType;

import software.amazon.awssdk.core.auth.policy.Policy;
import software.amazon.awssdk.services.iam.model.EvaluationResult;
import software.amazon.awssdk.services.iam.model.InstanceProfile;
import software.amazon.awssdk.services.iam.model.OrganizationsDecisionDetail;
import software.amazon.awssdk.services.iam.model.Role;

@ExtendWith(MockitoExtension.class)
class AwsLogRolePermissionValidatorTest {

    @Mock
    private AwsIamService awsIamService;

    @Mock
    private LocationHelper locationHelper;

    @InjectMocks
    private AwsLogRolePermissionValidator underTest;

    @Test
    void testValidateLog() {
        AmazonIdentityManagementClient amazonIdentityManagementClient = mock(AmazonIdentityManagementClient.class);
        when(awsIamService.getPolicy(eq("aws-cdp-log-policy.json"), anyMap())).thenReturn(new Policy());
        when(locationHelper.parseS3BucketName(any())).thenReturn("logs/");
        InstanceProfile instanceProfile = instanceProfile();
        CloudS3View cloudFileSystem = new CloudS3View(CloudIdentityType.LOG);
        StorageLocationBase storageLocationBase = new StorageLocationBase();
        storageLocationBase.setValue("storageLocationBase");
        cloudFileSystem.setLocations(List.of(storageLocationBase));
        ValidationResultBuilder resultBuilder = new ValidationResultBuilder();
        underTest.validateLog(amazonIdentityManagementClient, instanceProfile, cloudFileSystem, "s3://logs/",
                false, resultBuilder);

        verify(awsIamService, times(1)).validateRolePolicies(eq(amazonIdentityManagementClient), any(), any());
        assertFalse(resultBuilder.build().hasError());
    }

    @Test
    void testValidateBackup() {
        AmazonIdentityManagementClient amazonIdentityManagementClient = mock(AmazonIdentityManagementClient.class);
        when(awsIamService.getPolicy(eq("aws-datalake-restore-policy.json"), anyMap())).thenReturn(new Policy());
        when(locationHelper.parseS3BucketName(any())).thenReturn("logs/");
        InstanceProfile instanceProfile = instanceProfile();
        CloudS3View cloudFileSystem = new CloudS3View(CloudIdentityType.LOG);
        StorageLocationBase storageLocationBase = new StorageLocationBase();
        storageLocationBase.setValue("storageLocationBase");
        cloudFileSystem.setLocations(List.of(storageLocationBase));
        ValidationResultBuilder resultBuilder = new ValidationResultBuilder();
        underTest.validateBackup(amazonIdentityManagementClient, instanceProfile, cloudFileSystem, "s3://logs/",
                false, resultBuilder);

        verify(awsIamService, times(1)).validateRolePolicies(eq(amazonIdentityManagementClient), any(), any());
        assertFalse(resultBuilder.build().hasError());
    }

    @Test
    void testValidateShouldAppendOrgPolicyRelatedErrorMessageWhenRequired() {
        AmazonIdentityManagementClient amazonIdentityManagementClient = mock(AmazonIdentityManagementClient.class);
        InstanceProfile instanceProfile = instanceProfile();
        CloudS3View cloudFileSystem = new CloudS3View(CloudIdentityType.LOG);
        StorageLocationBase storageLocationBase = new StorageLocationBase();
        storageLocationBase.setValue("storageLocationBase");
        cloudFileSystem.setLocations(List.of(storageLocationBase));
        ValidationResultBuilder resultBuilder = new ValidationResultBuilder();
        when(awsIamService.validateRolePolicies(eq(amazonIdentityManagementClient), any(), any())).thenReturn(
                List.of(EvaluationResult.builder().organizationsDecisionDetail(
                        OrganizationsDecisionDetail.builder().allowedByOrganizations(false).build()).evalDecision("deny").build()));
        underTest.validate(amazonIdentityManagementClient, instanceProfile, cloudFileSystem, "s3://logs/",
                resultBuilder, new Policy(), false);

        verify(awsIamService, times(1)).validateRolePolicies(eq(amazonIdentityManagementClient), any(), any());
        ValidationResult result = resultBuilder.build();
        assertTrue(result.hasError());
        assertTrue(result.getFormattedErrors().contains(DENIED_BY_ORGANIZATION_RULE_ERROR_MESSAGE));
    }

    private InstanceProfile instanceProfile() {
        return InstanceProfile.builder()
                .arn("arn:aws:iam::11111111111:instance-profile/instanceprofile")
                .roles(Role.builder().arn("arn:aws:iam::123456890:role/role").build())
                .build();
    }
}