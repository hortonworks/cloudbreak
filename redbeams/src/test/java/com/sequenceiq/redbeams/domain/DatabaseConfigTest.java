package com.sequenceiq.redbeams.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DatabaseVendor;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.redbeams.TestData;
import com.sequenceiq.redbeams.api.endpoint.v4.ResourceStatus;

import org.junit.Before;
import org.junit.Test;

public class DatabaseConfigTest {

    private DatabaseConfig config;

    @Before
    public void setUp() {
        config = new DatabaseConfig();
    }

    @Test
    public void testGettersAndSetters() {
        config.setId(1L);
        assertEquals(1L, config.getId().longValue());

        config.setAccountId("myaccount");
        assertEquals("myaccount", config.getAccountId());

        Crn crn = TestData.getTestCrn("database", "mydb");
        config.setResourceCrn(crn);
        assertEquals(crn, config.getResourceCrn());

        config.setName("mydb");
        assertEquals("mydb", config.getName());

        config.setDescription("mine not yours");
        assertEquals("mine not yours", config.getDescription());

        config.setConnectionURL("jdbc:postgresql://myserver.db.example.com:5432");
        assertEquals("jdbc:postgresql://myserver.db.example.com:5432", config.getConnectionURL());

        config.setDatabaseVendor(DatabaseVendor.POSTGRES);
        assertEquals(DatabaseVendor.POSTGRES, config.getDatabaseVendor());

        config.setConnectionDriver("org.postgresql.Driver");
        assertEquals("org.postgresql.Driver", config.getConnectionDriver());

        config.setConnectionUserName("root");
        assertEquals("root", config.getConnectionUserName().getRaw());

        config.setConnectionPassword("cloudera");
        assertEquals("cloudera", config.getConnectionPassword().getRaw());

        long now = System.currentTimeMillis();
        config.setCreationDate(now);
        assertEquals(now, config.getCreationDate().longValue());

        config.setStatus(ResourceStatus.SERVICE_MANAGED);
        assertEquals(ResourceStatus.SERVICE_MANAGED, config.getStatus());

        config.setType("hive");
        assertEquals("hive", config.getType());

        config.setArchived(true);
        assertTrue(config.isArchived());

        config.setDeletionTimestamp(2L);
        assertEquals(2L, config.getDeletionTimestamp().longValue());

        config.setEnvironmentId("myenvironment");
        assertEquals("myenvironment", config.getEnvironmentId());

        config.setServer(new DatabaseServerConfig());
        assertNotNull(config.getServer());
    }

    @Test
    public void testUnsetRelationsToEntitiesToBeDeleted() {
        config.setServer(new DatabaseServerConfig());

        config.unsetRelationsToEntitiesToBeDeleted();

        assertNull(config.getServer());
    }
}
