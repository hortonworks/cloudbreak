package com.sequenceiq.cloudbreak.cloud.aws.common.validator;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.sequenceiq.cloudbreak.cloud.model.filesystem.CloudS3View;
import com.sequenceiq.cloudbreak.service.identitymapping.AccountMappingSubject;
import com.sequenceiq.common.api.cloudstorage.StorageLocationBase;
import com.sequenceiq.common.model.CloudIdentityType;
import com.sequenceiq.common.model.FileSystemType;

public class AwsDataAccessRolePermissionValidatorTest extends AwsIDBrokerMappedRolePermissionValidatorTest {
    private final AwsDataAccessRolePermissionValidator awsDataAccessRolePermissionValidator =
            new AwsDataAccessRolePermissionValidator();

    @Override
    public AwsIDBrokerMappedRolePermissionValidator getValidator() {
        return awsDataAccessRolePermissionValidator;
    }

    @Override
    public void testGetUsers() {
        assertThat(awsDataAccessRolePermissionValidator.getUsers())
                .isEqualTo(AccountMappingSubject.DATA_ACCESS_USERS);
    }

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

    @Override
    public void testGetStorageLocationBase() {
        String path = "testBucket/ranger/audit";
        String expectedStorageLocationBase = "testBucket/ranger/audit";
        StorageLocationBase location = new StorageLocationBase();
        location.setValue(String.format("%s://%s", FileSystemType.S3.getProtocol(), path));
        String storageLocationBase = awsDataAccessRolePermissionValidator.getStorageLocationBase(location);
        assertThat(storageLocationBase).isEqualTo(expectedStorageLocationBase);
    }

    @Override
    public void testCheckLocation() {
        assertThat(awsDataAccessRolePermissionValidator.checkLocation(new StorageLocationBase())).isTrue();
    }

    @Override
    public void testGetPolicyJsonReplacements() {
        String storageLocationBaseStr = "bucket/cluster";
        String bucket = "bucket";
        String dynamodbTableName = "tableName";

        Map<String, String> expectedPolicyJsonReplacements = Map.ofEntries(
                Map.entry("${STORAGE_LOCATION_BASE}", storageLocationBaseStr),
                Map.entry("${DATALAKE_BUCKET}", bucket),
                Map.entry("${DYNAMODB_TABLE_NAME}", dynamodbTableName)
        );

        StorageLocationBase storageLocationBase = new StorageLocationBase();
        storageLocationBase.setValue(storageLocationBaseStr);
        CloudS3View cloudFileSystem = new CloudS3View(CloudIdentityType.ID_BROKER);
        cloudFileSystem.setS3GuardDynamoTableName(dynamodbTableName);
        Map<String, String> policyJsonReplacements = awsDataAccessRolePermissionValidator
                .getPolicyJsonReplacements(storageLocationBase,
                        cloudFileSystem);

        assertThat(policyJsonReplacements).isEqualTo(expectedPolicyJsonReplacements);
    }

    @Override
    public void testGetPolicyJsonReplacementsNoDynamodb() {
        String storageLocationBaseStr = "bucket/cluster";
        String bucket = "bucket";

        Map<String, String> expectedPolicyJsonReplacements = Map.ofEntries(
            Map.entry("${STORAGE_LOCATION_BASE}", storageLocationBaseStr),
            Map.entry("${DATALAKE_BUCKET}", bucket),
            Map.entry("${DYNAMODB_TABLE_NAME}", "")
        );

        StorageLocationBase storageLocationBase = new StorageLocationBase();
        storageLocationBase.setValue(storageLocationBaseStr);
        CloudS3View cloudFileSystem = new CloudS3View(CloudIdentityType.ID_BROKER);
        Map<String, String> policyJsonReplacements = awsDataAccessRolePermissionValidator
                                                        .getPolicyJsonReplacements(storageLocationBase,
                                                            cloudFileSystem);

        assertThat(policyJsonReplacements).isEqualTo(expectedPolicyJsonReplacements);
    }
}
