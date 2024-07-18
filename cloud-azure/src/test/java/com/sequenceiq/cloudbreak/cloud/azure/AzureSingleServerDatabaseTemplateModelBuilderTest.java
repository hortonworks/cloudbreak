package com.sequenceiq.cloudbreak.cloud.azure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.cloud.azure.view.AzureDatabaseServerView;
import com.sequenceiq.cloudbreak.cloud.azure.view.AzureNetworkView;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseServer;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.cloud.model.Network;

@ExtendWith(MockitoExtension.class)
class AzureSingleServerDatabaseTemplateModelBuilderTest {

    @InjectMocks
    private AzureSingleServerDatabaseTemplateModelBuilder underTest;

    @Mock
    private AzureUtils azureUtils;

    @Test
    void testBuildModel() {
        ReflectionTestUtils.setField(underTest, "defaultBatchSize", 2);
        Network network = new Network(null);
        network.putParameter("subnets", "subnet1/net1,subnet2/net2");
        Map<String, Object> serverParams = new HashMap<>();
        serverParams.put("geoRedundantBackup", true);
        serverParams.put("backupRetentionDays", 3);
        serverParams.put("dbVersion", "dbversion");
        serverParams.put("keyVaultUrl", "https://encrypt.vault/keys/keyname/keyversion");
        DatabaseServer databaseServer = DatabaseServer.builder()
                .withServerId("dbname")
                .withFlavor("MO_Gen5_4")
                .withStorageSize(100L)
                .withRootUserName("root")
                .withRootPassword("pwd")
                .withHighAvailability(true)
                .withLocation("location")
                .withParams(serverParams)
                .build();
        DatabaseStack databaseStack = new DatabaseStack(network, databaseServer, Map.of("tag1", "tag1"), "");
        AzureDatabaseServerView azureDatabaseServerView = new AzureDatabaseServerView(databaseServer);
        AzureNetworkView azureNetworkView = new AzureNetworkView(network);
        when(azureUtils.getResourceName(anyString())).thenReturn("net1");
        when(azureUtils.encodeString("net1")).thenReturn("net1");
        Map<String, Object> actualResult = underTest.buildModel(azureDatabaseServerView, azureNetworkView, databaseStack);
        assertEquals("location", actualResult.get("location"));
        assertEquals("root", actualResult.get("adminLoginName"));
        assertEquals("pwd", actualResult.get("adminPassword"));
        assertEquals(3, actualResult.get("backupRetentionDays"));
        assertEquals("dbname", actualResult.get("dbServerName"));
        assertEquals("dbversion", actualResult.get("dbVersion"));
        assertEquals(true, actualResult.get("geoRedundantBackup"));
        assertEquals("MemoryOptimized", actualResult.get("skuTier"));
        assertEquals("MO_Gen5_4", actualResult.get("skuName"));
        assertEquals(102400L, actualResult.get("skuSizeMB"));
        assertEquals("subnet1/net1,subnet2/net2", actualResult.get("subnets"));
        assertEquals(2, actualResult.get("batchSize"));
        assertEquals("pe-net1-to-dbname", actualResult.get("privateEndpointName"));
        assertEquals(true, actualResult.get("dataEncryption"));
        assertEquals("encrypt", actualResult.get("keyVaultName"));
        assertEquals("keyname", actualResult.get("keyName"));
        assertEquals("keyversion", actualResult.get("keyVersion"));
    }

    @Test
    void testBuildModelMissingEncryptionKeyVersion() {
        ReflectionTestUtils.setField(underTest, "defaultBatchSize", 2);
        Network network = new Network(null);
        network.putParameter("subnets", "subnet1/net1,subnet2/net2");
        Map<String, Object> serverParams = new HashMap<>();
        serverParams.put("geoRedundantBackup", true);
        serverParams.put("backupRetentionDays", 3);
        serverParams.put("dbVersion", "dbversion");
        serverParams.put("keyVaultUrl", "https://encrypt.vault/keys/keyname");
        DatabaseServer databaseServer = DatabaseServer.builder()
                .withServerId("dbname")
                .withFlavor("MO_Gen5_4")
                .withStorageSize(100L)
                .withRootUserName("root")
                .withRootPassword("pwd")
                .withHighAvailability(true)
                .withLocation("location")
                .withParams(serverParams)
                .build();
        DatabaseStack databaseStack = new DatabaseStack(network, databaseServer, Map.of("tag1", "tag1"), "");
        AzureDatabaseServerView azureDatabaseServerView = new AzureDatabaseServerView(databaseServer);
        AzureNetworkView azureNetworkView = new AzureNetworkView(network);
        assertThrows(IllegalArgumentException.class, () -> underTest.buildModel(azureDatabaseServerView, azureNetworkView, databaseStack));
    }
}