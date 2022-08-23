package com.sequenceiq.cloudbreak.service.upgrade.rds;

import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BackupRestoreEmbeddedDBStateParamsProviderTest {

    @InjectMocks
    private BackupRestoreEmbeddedDBStateParamsProvider underTest;

    @Test
    void testCreateParams() {
        Map<String, Object> actualResult = underTest.createParamsForBackupRestore();
        Map<String, String> upgradeParams = (Map<String, String>) ((Map<String, Object>) actualResult.get("postgres")).get("upgrade");
        Assertions.assertEquals(upgradeParams.get("embeddeddb_host"), "localhost");
        Assertions.assertEquals(upgradeParams.get("embeddeddb_port"), "5432");
        Assertions.assertEquals(upgradeParams.get("embeddeddb_user"), "postgres");
        Assertions.assertEquals(upgradeParams.get("embeddeddb_password"), "postgres");
    }
}
