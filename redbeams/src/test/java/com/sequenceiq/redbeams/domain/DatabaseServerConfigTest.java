package com.sequenceiq.redbeams.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Optional;

import org.hibernate.annotations.Where;
import org.junit.Before;
import org.junit.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DatabaseVendor;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.common.database.DatabaseCommon;
import com.sequenceiq.redbeams.TestData;
import com.sequenceiq.redbeams.api.endpoint.v4.ResourceStatus;
import com.sequenceiq.redbeams.domain.stack.DBStack;

public class DatabaseServerConfigTest {

    private DatabaseServerConfig config;

    @Before
    public void setUp() {
        config = new DatabaseServerConfig();
    }

    @Test
    public void testGettersAndSetters() {
        config.setId(1L);
        assertEquals(1L, config.getId().longValue());

        config.setAccountId("myaccount");
        assertEquals("myaccount", config.getAccountId());

        Crn crn = TestData.getTestCrn("databaseServer", "myserver");
        config.setResourceCrn(crn);
        assertEquals(crn, config.getResourceCrn());

        config.setWorkspaceId(0L);
        assertEquals(0L, config.getWorkspaceId().longValue());

        config.setName("myserver");
        assertEquals("myserver", config.getName());

        config.setDescription("mine not yours");
        assertEquals("mine not yours", config.getDescription());

        config.setHost("myserver.db.example.com");
        assertEquals("myserver.db.example.com", config.getHost());

        config.setPort(5432);
        assertEquals(5432, config.getPort().intValue());

        config.setDatabaseVendor(DatabaseVendor.POSTGRES);
        assertEquals(DatabaseVendor.POSTGRES, config.getDatabaseVendor());

        config.setConnectionDriver("org.postgresql.Driver");
        assertEquals("org.postgresql.Driver", config.getConnectionDriver());

        config.setConnectionUserName("root");
        assertEquals("root", config.getConnectionUserName());

        config.setConnectionPassword("cloudera");
        assertEquals("cloudera", config.getConnectionPassword());

        long now = System.currentTimeMillis();
        config.setCreationDate(now);
        assertEquals(now, config.getCreationDate().longValue());

        config.setResourceStatus(ResourceStatus.SERVICE_MANAGED);
        assertEquals(ResourceStatus.SERVICE_MANAGED, config.getResourceStatus());

        config.setDeletionTimestamp(2L);
        assertEquals(2L, config.getDeletionTimestamp().longValue());

        config.setArchived(true);
        assertTrue(config.isArchived());

        config.setEnvironmentId("myenvironment");
        assertEquals("myenvironment", config.getEnvironmentId());

        config.setResourceStatus(ResourceStatus.SERVICE_MANAGED);
        assertEquals(ResourceStatus.SERVICE_MANAGED, config.getResourceStatus());

        config.setDbStack(new DBStack());
        assertTrue(config.getDbStack().isPresent());
    }

    @Test
    public void testHasWhereAnnotation() {
        Where whereAnnotation = DatabaseServerConfig.class.getAnnotation(Where.class);

        assertNotNull(whereAnnotation);
        assertEquals("archived = false", whereAnnotation.clause());
    }

    @Test
    public void testCreateDatabaseConfig() {
        config.setId(1L);
        config.setAccountId("myaccount");
        Crn crn = TestData.getTestCrn("databaseServer", "myserver");
        config.setResourceCrn(crn);
        config.setWorkspaceId(0L);
        config.setName("myserver");
        config.setDescription("mine not yours");
        config.setHost("myserver.db.example.com");
        config.setPort(5432);
        config.setDatabaseVendor(DatabaseVendor.POSTGRES);
        config.setConnectionDriver("org.postgresql.Driver");
        config.setConnectionUserName("root");
        config.setConnectionPassword("cloudera");
        long now = System.currentTimeMillis();
        config.setCreationDate(now);
        config.setResourceStatus(ResourceStatus.SERVICE_MANAGED);
        config.setDeletionTimestamp(2L);
        config.setArchived(true);
        config.setEnvironmentId("myenvironment");

        DatabaseConfig db = config.createDatabaseConfig("mydb", "hive", ResourceStatus.USER_MANAGED, "dbuser", "dbpass");

        assertEquals(config.getDatabaseVendor(), db.getDatabaseVendor());
        assertEquals("mydb", db.getName());
        assertEquals(config.getDescription(), db.getDescription());
        String connectionUrl = new DatabaseCommon().getJdbcConnectionUrl(config.getDatabaseVendor().jdbcUrlDriverId(),
            config.getHost(), config.getPort(), Optional.of("mydb"));
        assertEquals(connectionUrl, db.getConnectionURL());
        assertEquals(config.getConnectionDriver(), db.getConnectionDriver());
        assertEquals("dbuser", db.getConnectionUserName().getRaw());
        assertEquals("dbpass", db.getConnectionPassword().getRaw());
        assertEquals(ResourceStatus.USER_MANAGED, db.getStatus());
        assertEquals("hive", db.getType());
        assertEquals(config.getEnvironmentId(), db.getEnvironmentId());
        assertEquals(config, db.getServer());
    }
}
