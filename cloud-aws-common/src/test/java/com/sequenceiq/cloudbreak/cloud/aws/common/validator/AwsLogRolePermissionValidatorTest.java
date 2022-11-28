package com.sequenceiq.cloudbreak.cloud.aws.common.validator;

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

import com.amazonaws.auth.policy.Policy;
import com.amazonaws.services.identitymanagement.model.EvaluationResult;
import com.amazonaws.services.identitymanagement.model.InstanceProfile;
import com.amazonaws.services.identitymanagement.model.OrganizationsDecisionDetail;
import com.amazonaws.services.identitymanagement.model.Role;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonIdentityManagementClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.util.AwsIamService;
import com.sequenceiq.cloudbreak.cloud.model.filesystem.CloudS3View;
import com.sequenceiq.cloudbreak.cloud.storage.LocationHelper;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.cloudbreak.validation.ValidationResult.ValidationResultBuilder;
import com.sequenceiq.common.api.cloudstorage.StorageLocationBase;
import com.sequenceiq.common.model.CloudIdentityType;

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
                List.of(new EvaluationResult().withOrganizationsDecisionDetail(
                        new OrganizationsDecisionDetail().withAllowedByOrganizations(false)).withEvalDecision("deny")));
        underTest.validate(amazonIdentityManagementClient, instanceProfile, cloudFileSystem, "s3://logs/",
                resultBuilder, new Policy(), false);

        verify(awsIamService, times(1)).validateRolePolicies(eq(amazonIdentityManagementClient), any(), any());
        ValidationResult result = resultBuilder.build();
        assertTrue(result.hasError());
        assertTrue(result.getFormattedErrors().contains("Please note SCPs with global condition keys and whitelisted accounts are not supported"));
    }

    private InstanceProfile instanceProfile() {
        return new InstanceProfile()
                .withArn("arn:aws:iam::11111111111:instance-profile/instanceprofile")
                .withRoles(new Role().withArn("arn:aws:iam::123456890:role/role"));
    }
}