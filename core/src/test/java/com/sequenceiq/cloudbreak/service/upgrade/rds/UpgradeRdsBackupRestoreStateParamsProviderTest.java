package com.sequenceiq.cloudbreak.service.upgrade.rds;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;
import com.sequenceiq.cloudbreak.service.cluster.EmbeddedDatabaseService;
import com.sequenceiq.cloudbreak.view.ClusterView;

@ExtendWith(MockitoExtension.class)
class UpgradeRdsBackupRestoreStateParamsProviderTest {

    @Mock
    private EmbeddedDatabaseService embeddedDatabaseService;

    @InjectMocks
    private UpgradeRdsBackupRestoreStateParamsProvider underTest;

    @Test
    void testCreateParamsWhenRemoteDBAndBackupLocationGiven() {
        StackDto stackDto = mock(StackDto.class);
        ClusterView clusterView = mock(ClusterView.class);
        when(stackDto.getCluster()).thenReturn(clusterView);
        when(clusterView.getDatabaseServerCrn()).thenReturn("dbCrn");
        Map<String, SaltPillarProperties> actualResult = underTest.createParamsForRdsBackupRestore(stackDto, "backuplocation");
        Map<String, Object> pillarParams = (Map<String, Object>) actualResult.get("postgresql-upgrade").getProperties().get("upgrade");
        Map<String, Object> backupParams = (Map<String, Object>) pillarParams.get("backup");
        Map<String, Object> restoreParams = (Map<String, Object>) pillarParams.get("restore");
        Map<String, Object> checkConnectionParams = (Map<String, Object>) pillarParams.get("checkconnection");
        Assertions.assertEquals(backupParams.get("directory"), "backuplocation/tmp/postgres_upgrade_backup");
        Assertions.assertEquals(backupParams.get("logfile"), "/var/log/postgres_upgrade_backup.log");
        Assertions.assertEquals(restoreParams.get("logfile"), "/var/log/postgres_upgrade_restore.log");
        Assertions.assertEquals(checkConnectionParams.get("logfile"), "/var/log/postgres_upgrade_checkconnection.log");
    }

    @Test
    void testCreateParamsWhenRemoteDBAndBackupLocationNull() {
        StackDto stackDto = mock(StackDto.class);
        ClusterView clusterView = mock(ClusterView.class);
        when(clusterView.getDatabaseServerCrn()).thenReturn("dbCrn");
        when(stackDto.getCluster()).thenReturn(clusterView);
        Map<String, SaltPillarProperties> actualResult = underTest.createParamsForRdsBackupRestore(stackDto, null);
        Map<String, Object> pillarParams = (Map<String, Object>) actualResult.get("postgresql-upgrade").getProperties().get("upgrade");
        Map<String, Object> backupParams = (Map<String, Object>) pillarParams.get("backup");
        Assertions.assertEquals(backupParams.get("directory"), "/var/tmp/postgres_upgrade_backup");
    }

    @Test
    void testCreateParamsWhenEmbeddedDB() {
        StackDto stackDto = mock(StackDto.class);
        ClusterView clusterView = mock(ClusterView.class);
        when(stackDto.getCluster()).thenReturn(clusterView);
        when(embeddedDatabaseService.isAttachedDiskForEmbeddedDatabaseCreated(stackDto)).thenReturn(true);
        Map<String, SaltPillarProperties> actualResult = underTest.createParamsForRdsBackupRestore(stackDto, null);
        Map<String, Object> pillarParams = (Map<String, Object>) actualResult.get("postgresql-upgrade").getProperties().get("upgrade");
        Map<String, Object> backupParams = (Map<String, Object>) pillarParams.get("backup");
        Assertions.assertEquals(backupParams.get("directory"), "/dbfs/tmp/postgres_upgrade_backup");
    }
}
