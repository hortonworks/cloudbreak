package com.sequenceiq.cloudbreak.service.upgrade.rds;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;

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
class UpgradeExternalRdsStateParamsProviderTest {

    @Mock
    private EmbeddedDatabaseService embeddedDatabaseService;

    @InjectMocks
    private UpgradeExternalRdsStateParamsProvider underTest;

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
        assertEquals("backuplocation/tmp/postgres_upgrade_backup", backupParams.get("directory"));
        assertEquals("/var/log/postgres_upgrade_backup.log", backupParams.get("logfile"));
        assertEquals("/var/log/postgres_upgrade_restore.log", restoreParams.get("logfile"));
        assertEquals("/var/log/postgres_upgrade_checkconnection.log", checkConnectionParams.get("logfile"));
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
        assertEquals("/var/tmp/postgres_upgrade_backup", backupParams.get("directory"));
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
        assertEquals("/dbfs/tmp/postgres_upgrade_backup", backupParams.get("directory"));
    }

    // Method returns nested Map with correct structure and keys
    @Test
    public void testVerifyNestedMapStructure() {
        // Given
        UpgradeExternalRdsStateParamsProvider provider = new UpgradeExternalRdsStateParamsProvider();
        String serverUrl = "test-server";
        String userName = "test-user";

        // When
        Map<String, Object> result = provider.createParamsForRdsCanaryCheck(serverUrl, userName);

        // Then
        assertNotNull(result);
        assertTrue(result.containsKey("postgresql-upgrade"));
        Map<String, Object> upgradeMap = (Map<String, Object>) result.get("postgresql-upgrade");
        assertTrue(upgradeMap.containsKey("upgrade"));
        Map<String, Object> innerMap = (Map<String, Object>) upgradeMap.get("upgrade");
        assertTrue(innerMap.containsKey("checkconnection"));
    }

    // ServerUrl parameter is correctly mapped to canary_hostname in properties
    @Test
    public void testServerUrlMapping() {
        // Given
        UpgradeExternalRdsStateParamsProvider provider = new UpgradeExternalRdsStateParamsProvider();
        String serverUrl = "test-server.domain.com";
        String userName = "test-user";

        // When
        Map<String, Object> result = provider.createParamsForRdsCanaryCheck(serverUrl, userName);

        // Then
        Map<String, Object> upgradeMap = (Map<String, Object>) result.get("postgresql-upgrade");
        Map<String, Object> innerMap = (Map<String, Object>) upgradeMap.get("upgrade");
        Map<String, String> connectionProps = (Map<String, String>) innerMap.get("checkconnection");
        assertEquals(serverUrl, connectionProps.get("canary_hostname"));
        assertEquals(userName, connectionProps.get("canary_username"));
    }
}