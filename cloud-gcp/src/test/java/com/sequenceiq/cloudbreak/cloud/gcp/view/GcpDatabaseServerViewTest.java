package com.sequenceiq.cloudbreak.cloud.gcp.view;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.cloud.model.DatabaseEngine;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseServer;

public class GcpDatabaseServerViewTest {

    @Test
    public void testGcpDatabaseNetworkView() {
        Map<String, Object> map = new HashMap<>();
        map.put("engineVersion", "1");

        DatabaseServer databaseServer = DatabaseServer.builder()
                .withConnectionDriver("driver")
                .withServerId("driver")
                .withConnectorJarUrl("driver")
                .withEngine(DatabaseEngine.POSTGRESQL)
                .withLocation("location")
                .withPort(99)
                .withStorageSize(50L)
                .withRootUserName("rootUserName")
                .withRootPassword("rootPassword")
                .withFlavor("flavor")
                .withUseSslEnforcement(true)
                .withParams(map)
                .build();

        GcpDatabaseServerView gcpDatabaseServerView = new GcpDatabaseServerView(databaseServer);

        Assert.assertEquals("POSTGRES", gcpDatabaseServerView.getDatabaseType());
        Assert.assertEquals("POSTGRES_1", gcpDatabaseServerView.getDatabaseVersion());
        Assert.assertEquals("driver", gcpDatabaseServerView.getDbServerName());
        Assert.assertEquals("rootUserName", gcpDatabaseServerView.getAdminLoginName());
        Assert.assertEquals(Integer.valueOf(99), gcpDatabaseServerView.getPort());
        Assert.assertEquals("rootPassword", gcpDatabaseServerView.getAdminPassword());
        Assert.assertEquals(Long.valueOf(50L), gcpDatabaseServerView.getAllocatedStorageInGb());
        Assert.assertEquals("1", gcpDatabaseServerView.getDbVersion());

    }
}
