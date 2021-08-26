package com.sequenceiq.cloudbreak.cloud.aws.common.validator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
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
import com.sequenceiq.cloudbreak.cloud.model.filesystem.CloudS3View;
import com.sequenceiq.cloudbreak.service.identitymapping.AccountMappingSubject;
import com.sequenceiq.common.api.cloudstorage.StorageLocationBase;
import com.sequenceiq.common.model.CloudIdentityType;
import com.sequenceiq.common.model.CloudStorageCdpService;
import com.sequenceiq.common.model.FileSystemType;

@ExtendWith(MockitoExtension.class)
public class AwsRangerAuditRolePermissionValidatorTest extends AwsIDBrokerMappedRolePermissionValidatorTest {

    @Mock
    private AwsIamService awsIamService;

    @InjectMocks
    private AwsRangerAuditRolePermissionValidator awsRangerAuditRolePermissionValidator;

    @Override
    public AwsIDBrokerMappedRolePermissionValidator getValidator() {
        return awsRangerAuditRolePermissionValidator;
    }

    @Test
    @Override
    public void testGetUsers() {
        assertThat(awsRangerAuditRolePermissionValidator.getUsers())
                .isEqualTo(AccountMappingSubject.RANGER_AUDIT_USERS);
    }

    @Test
    @Override
    public void testGetPolicyFileNames() {
        List<String> expectedPolicyFileNamesNoS3Guard = Arrays.asList(
                "aws-cdp-bucket-access-policy.json",
                "aws-cdp-ranger-audit-s3-policy.json"
        );
        List<String> policyFileNamesNoS3Guard = awsRangerAuditRolePermissionValidator
                .getPolicyFileNames(false);
        assertThat(policyFileNamesNoS3Guard).isEqualTo(expectedPolicyFileNamesNoS3Guard);

        List<String> expectedPolicyFileNamesS3Guard = Arrays.asList(
                "aws-cdp-bucket-access-policy.json",
                "aws-cdp-ranger-audit-s3-policy.json",
                "aws-cdp-dynamodb-policy.json"
        );
        List<String> policyFileNamesS3Guard = awsRangerAuditRolePermissionValidator
                .getPolicyFileNames(true);
        assertThat(policyFileNamesS3Guard).isEqualTo(expectedPolicyFileNamesS3Guard);
    }

    @Test
    @Override
    public void testGetStorageLocationBase() {
        String path = "testBucket/ranger/audit";
        String expectedStorageLocationBase = "testBucket";
        StorageLocationBase location = new StorageLocationBase();
        location.setValue(String.format("%s://%s", FileSystemType.S3.getProtocol(), path));
        String storageLocationBase = awsRangerAuditRolePermissionValidator.getStorageLocationBase(location);
        assertThat(storageLocationBase).isEqualTo(expectedStorageLocationBase);
    }

    @Test
    @Override
    public void testCheckLocation() {
        assertThat(awsRangerAuditRolePermissionValidator.checkLocation(new StorageLocationBase())).isFalse();

        StorageLocationBase nonRangerAuditLocation = new StorageLocationBase();
        nonRangerAuditLocation.setType(CloudStorageCdpService.HBASE_ROOT);
        assertThat(awsRangerAuditRolePermissionValidator.checkLocation(nonRangerAuditLocation)).isFalse();

        StorageLocationBase rangerAuditLocation = new StorageLocationBase();
        rangerAuditLocation.setType(CloudStorageCdpService.RANGER_AUDIT);
        assertThat(awsRangerAuditRolePermissionValidator.checkLocation(rangerAuditLocation)).isTrue();
    }

    @Test
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
        Map<String, String> policyJsonReplacements = awsRangerAuditRolePermissionValidator
                .getPolicyJsonReplacements(storageLocationBase,
                        cloudFileSystem);

        assertThat(policyJsonReplacements).isEqualTo(expectedPolicyJsonReplacements);
    }

    @Test
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
        Map<String, String> policyJsonReplacements = awsRangerAuditRolePermissionValidator
                                                        .getPolicyJsonReplacements(storageLocationBase,
                                                            cloudFileSystem);

        assertThat(policyJsonReplacements).isEqualTo(expectedPolicyJsonReplacements);
    }

    @Test
    @Override
    public void testCollectPolicies() {
        ArgumentCaptor<Map<String, String>> replacementsCaptor = ArgumentCaptor.forClass(Map.class);
        when(awsIamService.getPolicy(anyString(), replacementsCaptor.capture())).thenReturn(new Policy());
        CloudS3View cloudFileSystem = new CloudS3View(CloudIdentityType.ID_BROKER);
        StorageLocationBase storageLocationBase1 = new StorageLocationBase();
        storageLocationBase1.setType(CloudStorageCdpService.RANGER_AUDIT);
        storageLocationBase1.setValue("s3a://bucket/cluster/ranger/audit");
        cloudFileSystem.setLocations(List.of(storageLocationBase1));

        List<Policy> policies = getValidator().collectPolicies(cloudFileSystem, List.of("policyFile1", "policyFile2"));

        assertEquals(2, policies.size());
        Map<String, String> replacements = replacementsCaptor.getValue();
        assertEquals("bucket/cluster", replacements.get("${STORAGE_LOCATION_BASE}"));
        assertEquals("bucket", replacements.get("${DATALAKE_BUCKET}"));
        assertEquals("", replacements.get("${DYNAMODB_TABLE_NAME}"));
    }
}
