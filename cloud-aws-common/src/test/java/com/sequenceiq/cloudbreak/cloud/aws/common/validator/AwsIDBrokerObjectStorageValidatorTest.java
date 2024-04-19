package com.sequenceiq.cloudbreak.cloud.aws.common.validator;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonIdentityManagementClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.util.AwsIamService;
import com.sequenceiq.cloudbreak.cloud.model.BackupOperationType;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredentialSettings;
import com.sequenceiq.cloudbreak.cloud.model.SpiFileSystem;
import com.sequenceiq.cloudbreak.cloud.model.filesystem.CloudFileSystemView;
import com.sequenceiq.cloudbreak.cloud.model.filesystem.CloudS3View;
import com.sequenceiq.cloudbreak.cloud.model.objectstorage.ObjectStorageValidateRequest;
import com.sequenceiq.cloudbreak.validation.ValidationResult.ValidationResultBuilder;
import com.sequenceiq.common.api.cloudstorage.StorageLocationBase;
import com.sequenceiq.common.model.CloudIdentityType;
import com.sequenceiq.common.model.FileSystemType;

import software.amazon.awssdk.services.iam.model.InstanceProfile;
import software.amazon.awssdk.services.iam.model.Role;

@ExtendWith(MockitoExtension.class)
class AwsIDBrokerObjectStorageValidatorTest {

    @Mock
    private AwsIamService awsIamService;

    @Mock
    private AwsInstanceProfileEC2TrustValidator awsInstanceProfileEC2TrustValidator;

    @Mock
    private AwsLogRolePermissionValidator awsLogRolePermissionValidator;

    @Mock
    private EntitlementService entitlementService;

    @InjectMocks
    private AwsIDBrokerObjectStorageValidator underTest;

    static Stream<Arguments> parameterScenarios() {
        return Stream.of(
                Arguments.of(true, 0),
                Arguments.of(false, 1)
        );
    }

    @ParameterizedTest(name = "skipLogRoleValidationforBackup = {0}, calls = {1}")
    @MethodSource("parameterScenarios")
    void testValidateObjectStorageSkippingLogRoleValidation(boolean skipLogRoleValidationforBackup, int calls) {
        ObjectStorageValidateRequest objectStorageValidateRequest = getObjectStorageValidateRequest(skipLogRoleValidationforBackup);
        AmazonIdentityManagementClient amazonIdentityManagementClient = mock(AmazonIdentityManagementClient.class);
        CloudS3View cloudFileSystem = new CloudS3View(CloudIdentityType.LOG);
        StorageLocationBase storageLocationBase = new StorageLocationBase();
        storageLocationBase.setValue("storageLocationBase");
        cloudFileSystem.setInstanceProfile("arn:aws:iam::11111111111:instance-profile/instanceprofile");
        cloudFileSystem.setLocations(List.of(storageLocationBase));
        List<CloudFileSystemView> cloudFileSystemViews = new ArrayList<>();
        cloudFileSystemViews.add(cloudFileSystem);
        SpiFileSystem spiFileSystem = new SpiFileSystem("s3", FileSystemType.S3, cloudFileSystemViews);
        ValidationResultBuilder resultBuilder = new ValidationResultBuilder();

        when(awsIamService.getInstanceProfile(any(), any(), any(), any())).thenReturn(instanceProfile());
        when(entitlementService.isDatalakeBackupRestorePrechecksEnabled(any())).thenReturn(true);

        underTest.validateObjectStorage(objectStorageValidateRequest, amazonIdentityManagementClient, spiFileSystem, "accountId", resultBuilder);

        verify(awsInstanceProfileEC2TrustValidator, times(1)).isTrusted(any(), any(), any());
        verify(awsLogRolePermissionValidator, times(1)).validateLog(any(), any(), any(), any(), anyBoolean(), any());
        verify(awsLogRolePermissionValidator, times(calls)).validateBackup(any(), any(), any(), any(), anyBoolean(), any());
    }

    private ObjectStorageValidateRequest getObjectStorageValidateRequest(boolean skipLogRoleValidationforBackup) {
        ObjectStorageValidateRequest objectStorageValidateRequest = new ObjectStorageValidateRequest();
        objectStorageValidateRequest.setBackupOperationType(BackupOperationType.RESTORE);
        objectStorageValidateRequest.setSkipLogRoleValidationforBackup(skipLogRoleValidationforBackup);
        CloudCredential cloudCredential = new CloudCredential();
        CloudCredentialSettings cloudCredentialSettings = new CloudCredentialSettings();
        cloudCredential.setCredentialSettings(cloudCredentialSettings);
        objectStorageValidateRequest.setCredential(cloudCredential);
        return objectStorageValidateRequest;
    }

    private InstanceProfile instanceProfile() {
        return InstanceProfile.builder()
                .arn("arn:aws:iam::11111111111:instance-profile/instanceprofile")
                .roles(Role.builder().arn("arn:aws:iam::123456890:role/role").build())
                .build();
    }
}