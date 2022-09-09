package com.sequenceiq.cloudbreak.cloud.aws.common.validator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Map;


import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.amazonaws.auth.policy.Policy;
import com.sequenceiq.cloudbreak.cloud.aws.common.util.AwsIamService;
import com.sequenceiq.cloudbreak.cloud.model.BackupOperationType;
import com.sequenceiq.cloudbreak.cloud.model.filesystem.CloudS3View;
import com.sequenceiq.cloudbreak.cloud.storage.LocationHelper;
import com.sequenceiq.cloudbreak.service.identitymapping.AccountMappingSubject;
import com.sequenceiq.common.api.cloudstorage.StorageLocationBase;
import com.sequenceiq.common.model.CloudIdentityType;
import com.sequenceiq.common.model.CloudStorageCdpService;
import com.sequenceiq.common.model.FileSystemType;

@ExtendWith(MockitoExtension.class)
public class AwsDataAccessRolePermissionValidatorTest extends AwsIDBrokerMappedRolePermissionValidatorTest {

    @Mock
    private AwsIamService awsIamService;

    @Mock
    private LocationHelper locationHelper;

    @InjectMocks
    private AwsDataAccessRolePermissionValidator awsDataAccessRolePermissionValidator;

    @Override
    public AwsIDBrokerMappedRolePermissionValidator getValidator() {
        return awsDataAccessRolePermissionValidator;
    }

    @Test
    @Override
    public void testGetUsers() {
        assertThat(awsDataAccessRolePermissionValidator.getUsers())
                .isEqualTo(AccountMappingSubject.DATA_ACCESS_USERS);
    }

    @Test
    @Override
    public void testGetPolicyFileNames() {
        List<String> expectedPolicyFileNamesNoS3Guard = Arrays.asList(
                "aws-cdp-bucket-access-policy.json",
                "aws-cdp-datalake-admin-s3-policy.json"
        );
        List<String> policyFileNamesNoS3Guard = awsDataAccessRolePermissionValidator
                .getPolicyFileNames(false);
        assertThat(policyFileNamesNoS3Guard).isEqualTo(expectedPolicyFileNamesNoS3Guard);

        List<String> expectedPolicyFileNamesS3Guard = Arrays.asList(
                "aws-cdp-bucket-access-policy.json",
                "aws-cdp-datalake-admin-s3-policy.json",
                "aws-cdp-dynamodb-policy.json"
        );
        List<String> policyFileNamesS3Guard = awsDataAccessRolePermissionValidator
                .getPolicyFileNames(true);
        assertThat(policyFileNamesS3Guard).isEqualTo(expectedPolicyFileNamesS3Guard);
    }

    @Test
    @Override
    public void testGetStorageLocationBase() {
        String path = "testBucket/ranger/audit";
        String expectedStorageLocationBase = "testBucket/ranger/audit";
        StorageLocationBase location = new StorageLocationBase();
        location.setValue(String.format("%s://%s", FileSystemType.S3.getProtocol(), path));
        String storageLocationBase = awsDataAccessRolePermissionValidator.getStorageLocationBase(location);
        assertThat(storageLocationBase).isEqualTo(expectedStorageLocationBase);
    }

    @Test
    @Override
    public void testCheckLocation() {
        assertThat(awsDataAccessRolePermissionValidator.checkLocation(new StorageLocationBase())).isTrue();
    }

    @Test
    @Override
    public void testGetPolicyJsonReplacements() {
        String storageLocationBaseStr = "bucket/cluster";
        String bucket = "bucket";
        String dynamodbTableName = "tableName";

        Map<String, String> expectedPolicyJsonReplacements = Map.ofEntries(
                Map.entry("${ARN_PARTITION}", "aws"),
                Map.entry("${STORAGE_LOCATION_BASE}", storageLocationBaseStr),
                Map.entry("${DATALAKE_BUCKET}", bucket),
                Map.entry("${DYNAMODB_TABLE_NAME}", dynamodbTableName)
        );

        StorageLocationBase storageLocationBase = new StorageLocationBase();
        storageLocationBase.setValue(storageLocationBaseStr);
        CloudS3View cloudFileSystem = new CloudS3View(CloudIdentityType.ID_BROKER);
        cloudFileSystem.setS3GuardDynamoTableName(dynamodbTableName);
        cloudFileSystem.setInstanceProfile("arn:aws:iam::11111111111:instance-profile/instanceprofile");
        Map<String, String> policyJsonReplacements = awsDataAccessRolePermissionValidator
                .getPolicyJsonReplacements(storageLocationBase,
                        cloudFileSystem);

        assertThat(policyJsonReplacements).isEqualTo(expectedPolicyJsonReplacements);
    }

    @Test
    @Override
    public void testGetBackupPolicyJsonReplacements() {
        String backupLocation = "bucket/cluster";

        Map<String, String> expectedPolicyJsonReplacements = Map.ofEntries(
                Map.entry("${ARN_PARTITION}", "aws"),
                Map.entry("${BACKUP_LOCATION_BASE}", backupLocation),
                Map.entry("${BACKUP_BUCKET}", "bucket")
        );
        when(locationHelper.parseS3BucketName(anyString())).thenReturn("bucket");
        CloudS3View cloudFileSystem = new CloudS3View(CloudIdentityType.ID_BROKER);
        cloudFileSystem.setInstanceProfile("arn:aws:iam::11111111111:instance-profile/instanceprofile");
        Map<String, String> policyJsonReplacements = awsDataAccessRolePermissionValidator
                .getBackupPolicyJsonReplacements(cloudFileSystem, backupLocation);

        assertThat(policyJsonReplacements).isEqualTo(expectedPolicyJsonReplacements);
    }

    @Test
    @Override
    public void testGetBackupPolicyJsonReplacementsWithEmptyBackupLocation() {
        String backupLocation = null;
        CloudS3View cloudFileSystem = new CloudS3View(CloudIdentityType.ID_BROKER);
        cloudFileSystem.setInstanceProfile("arn:aws:iam::11111111111:instance-profile/instanceprofile");
        Map<String, String> policyJsonReplacements = awsDataAccessRolePermissionValidator
                .getBackupPolicyJsonReplacements(cloudFileSystem, backupLocation);

        assertTrue(policyJsonReplacements.isEmpty());
    }

    @Test
    @Override
    public void testGetPolicyJsonReplacementsNoDynamodb() {
        String storageLocationBaseStr = "bucket/cluster";
        String bucket = "bucket";

        Map<String, String> expectedPolicyJsonReplacements = Map.ofEntries(
                Map.entry("${ARN_PARTITION}", "aws"),
                Map.entry("${STORAGE_LOCATION_BASE}", storageLocationBaseStr),
                Map.entry("${DATALAKE_BUCKET}", bucket),
                Map.entry("${DYNAMODB_TABLE_NAME}", "")
        );

        StorageLocationBase storageLocationBase = new StorageLocationBase();
        storageLocationBase.setValue(storageLocationBaseStr);
        CloudS3View cloudFileSystem = new CloudS3View(CloudIdentityType.ID_BROKER);
        cloudFileSystem.setInstanceProfile("arn:aws:iam::11111111111:instance-profile/instanceprofile");
        Map<String, String> policyJsonReplacements = awsDataAccessRolePermissionValidator
                .getPolicyJsonReplacements(storageLocationBase,
                        cloudFileSystem);

        assertThat(policyJsonReplacements).isEqualTo(expectedPolicyJsonReplacements);
    }

    @Test
    @Override
    public void testCollectPolicies() {
        ArgumentCaptor<Map<String, String>> replacementsCaptor = ArgumentCaptor.forClass(Map.class);
        when(awsIamService.getPolicy(eq("policyFile1"), replacementsCaptor.capture())).thenReturn(new Policy());
        CloudS3View cloudFileSystem = new CloudS3View(CloudIdentityType.ID_BROKER);
        StorageLocationBase storageLocationBase1 = new StorageLocationBase();
        storageLocationBase1.setType(CloudStorageCdpService.HIVE_METASTORE_WAREHOUSE);
        storageLocationBase1.setValue("s3a://bucket/cluster/hive/metadata");
        cloudFileSystem.setLocations(List.of(storageLocationBase1));
        cloudFileSystem.setInstanceProfile("arn:aws:iam::11111111111:instance-profile/instanceprofile");

        List<Policy> policies = getValidator().collectPolicies(cloudFileSystem, List.of("policyFile1"));

        assertEquals(1, policies.size());
        Map<String, String> replacements = replacementsCaptor.getValue();
        assertEquals("bucket/cluster/hive/metadata", replacements.get("${STORAGE_LOCATION_BASE}"));
        assertEquals("bucket", replacements.get("${DATALAKE_BUCKET}"));
        assertEquals("", replacements.get("${DYNAMODB_TABLE_NAME}"));
    }

    @Test
    @Override
    public void testCollectBackupPolicies() {
        String backupLocation = "bucket/cluster";
        ArgumentCaptor<Map<String, String>> replacementsCaptor = ArgumentCaptor.forClass(Map.class);
        when(awsIamService.getPolicy(anyString(), replacementsCaptor.capture())).thenReturn(new Policy());
        when(locationHelper.parseS3BucketName(backupLocation)).thenReturn("bucket");
        CloudS3View cloudFileSystem = new CloudS3View(CloudIdentityType.ID_BROKER);
        cloudFileSystem.setInstanceProfile("arn:aws:iam::11111111111:instance-profile/instanceprofile");

        List<Policy> policies = getValidator().collectBackupRestorePolicies(cloudFileSystem, BackupOperationType.ANY, backupLocation);

        assertEquals(2, policies.size());
        Map<String, String> replacements = replacementsCaptor.getValue();
        assertEquals("bucket", replacements.get("${BACKUP_BUCKET}"));
        assertEquals("bucket/cluster", replacements.get("${BACKUP_LOCATION_BASE}"));
        assertEquals("aws", replacements.get("${ARN_PARTITION}"));

        policies = getValidator().collectBackupRestorePolicies(cloudFileSystem, BackupOperationType.BACKUP, backupLocation);
        assertEquals(1, policies.size());

        policies = getValidator().collectBackupRestorePolicies(cloudFileSystem, BackupOperationType.RESTORE, backupLocation);
        assertEquals(1, policies.size());
    }

    @Test
    public void testCollectBackupFiles() {

        List<String> policies = getValidator().getPolicyFiles(BackupOperationType.ANY);

        assertEquals(2, policies.size());
        assertTrue(policies.contains(getValidator().getBackupPolicy()));
        assertTrue(policies.contains(getValidator().getRestorePolicy()));

        policies = getValidator().getPolicyFiles(BackupOperationType.BACKUP);
        assertEquals(1, policies.size());
        assertTrue(policies.contains(getValidator().getBackupPolicy()));

        policies = getValidator().getPolicyFiles(BackupOperationType.RESTORE);
        assertEquals(1, policies.size());
        assertTrue(policies.contains(getValidator().getRestorePolicy()));
    }
}
