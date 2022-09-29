package com.sequenceiq.cloudbreak.service.upgrade.rds;

import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.telemetry.fluent.cloud.AdlsGen2Config;
import com.sequenceiq.cloudbreak.telemetry.fluent.cloud.AdlsGen2ConfigGenerator;

@ExtendWith(MockitoExtension.class)
class BackupRestoreDBStateParamsProviderTest {

    private static final String BACKUP_LOCATION = "abfs://backup_location";

    private static final String ABFS_STORAGE_ACCOUNT_NAME = "anAccount";

    private static final String ABFS_FILESYSTEM_NAME = "aFileSystem";

    private static final String ABFS_FOLDER = "aFolder";

    @Mock
    private AdlsGen2ConfigGenerator adlsGen2ConfigGenerator;

    @InjectMocks
    private BackupRestoreDBStateParamsProvider underTest;

    @Test
    void testCreateBackupParamsWithoutLocation() {
        Map<String, Object> actualResult = underTest.createParamsForBackupRestore(null);
        Map<String, String> upgradeParams = (Map<String, String>) ((Map<String, Object>) actualResult.get("postgres")).get("upgrade");
        assertEmbeddedParams(upgradeParams);
        Assertions.assertNull(upgradeParams.get("backup_location"));
    }

    @Test
    void testCreateBackupParamsWithLocation() {
        AdlsGen2Config adlsGen2Config = new AdlsGen2Config(ABFS_FOLDER, ABFS_FILESYSTEM_NAME, ABFS_STORAGE_ACCOUNT_NAME, false);
        when(adlsGen2ConfigGenerator.generateStorageConfig(BACKUP_LOCATION)).thenReturn(adlsGen2Config);

        Map<String, Object> actualResult = underTest.createParamsForBackupRestore(BACKUP_LOCATION);
        Map<String, String> upgradeParams = (Map<String, String>) ((Map<String, Object>) actualResult.get("postgres")).get("upgrade");
        assertEmbeddedParams(upgradeParams);
        Assertions.assertEquals(BACKUP_LOCATION, upgradeParams.get("backup_location"));
        Assertions.assertEquals(ABFS_STORAGE_ACCOUNT_NAME, upgradeParams.get("abfs_account_name"));
        Assertions.assertEquals(ABFS_FILESYSTEM_NAME, upgradeParams.get("abfs_file_system"));
        Assertions.assertEquals(ABFS_FOLDER, upgradeParams.get("abfs_file_system_folder"));
    }

    private void assertEmbeddedParams(Map<String, String> upgradeParams) {
        Assertions.assertEquals("localhost", upgradeParams.get("embeddeddb_host"));
        Assertions.assertEquals("5432", upgradeParams.get("embeddeddb_port"));
        Assertions.assertEquals("postgres", upgradeParams.get("embeddeddb_user"));
        Assertions.assertEquals("postgres", upgradeParams.get("embeddeddb_password"));
    }
}
