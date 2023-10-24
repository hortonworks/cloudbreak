package com.sequenceiq.cloudbreak.cloud.azure;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.azure.view.AzureDatabaseServerView;
import com.sequenceiq.cloudbreak.cloud.azure.view.AzureNetworkView;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseServer;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.common.model.AzureHighAvailabiltyMode;

@ExtendWith(MockitoExtension.class)
class AzureFlexibleServerDatabaseTemplateModelBuilderTest {
    @InjectMocks
    private AzureFlexibleServerDatabaseTemplateModelBuilder underTest;

    @Mock
    private AzureUtils azureUtils;

    @Test
    void testBuildModel() {
        Network network = new Network(null);
        network.putParameter("subnets", "subnet");
        Map<String, Object> serverParams = new HashMap<>();
        serverParams.put("geoRedundantBackup", false);
        serverParams.put("backupRetentionDays", 3);
        serverParams.put("dbVersion", "dbversion");
        serverParams.put(AzureHighAvailabiltyMode.AZURE_HA_MODE_KEY, AzureHighAvailabiltyMode.SAME_ZONE.name());
        DatabaseServer databaseServer = DatabaseServer.builder()
                .withServerId("dbname")
                .withFlavor("Standard_E4ds_v4")
                .withStorageSize(128L)
                .withRootUserName("root")
                .withRootPassword("pwd")
                .withLocation("location")
                .withParams(serverParams)
                .build();
        DatabaseStack databaseStack = new DatabaseStack(network, databaseServer, Map.of("tag1", "tag1"), "");
        AzureDatabaseServerView azureDatabaseServerView = new AzureDatabaseServerView(databaseServer);
        AzureNetworkView azureNetworkView = new AzureNetworkView(network);
        Map<String, Object> actualResult = underTest.buildModel(azureDatabaseServerView, azureNetworkView, databaseStack);
        assertEquals("location", actualResult.get("location"));
        assertEquals("root", actualResult.get("adminLoginName"));
        assertEquals("pwd", actualResult.get("adminPassword"));
        assertEquals(3, actualResult.get("backupRetentionDays"));
        assertEquals("dbname", actualResult.get("dbServerName"));
        assertEquals("dbversion", actualResult.get("dbVersion"));
        assertEquals(false, actualResult.get("geoRedundantBackup"));
        assertEquals("MemoryOptimized", actualResult.get("skuTier"));
        assertEquals("Standard_E4ds_v4", actualResult.get("skuName"));
        assertEquals(128L, actualResult.get("skuSizeGB"));
        assertEquals("SameZone", actualResult.get("highAvailability"));
    }
}
