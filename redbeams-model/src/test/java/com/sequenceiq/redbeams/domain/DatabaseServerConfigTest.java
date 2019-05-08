package com.sequenceiq.redbeams.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DatabaseVendor;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.cloudbreak.workspace.resource.WorkspaceResource;

import org.hibernate.annotations.Where;
import org.junit.Before;
import org.junit.Test;

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

        config.setConnectionDriver("postgresql.jar");
        assertEquals("postgresql.jar", config.getConnectionDriver());

        config.setConnectionUserName("root");
        assertEquals("root", config.getConnectionUserName());

        config.setConnectionPassword("cloudera");
        assertEquals("cloudera", config.getConnectionPassword());

        long now = System.currentTimeMillis();
        config.setCreationDate(now);
        assertEquals(now, config.getCreationDate().longValue());

        config.setResourceStatus(ResourceStatus.DEFAULT);
        assertEquals(ResourceStatus.DEFAULT, config.getResourceStatus());

        config.setConnectorJarUrl("http://drivers.example.com/postgresql.jar");
        assertEquals("http://drivers.example.com/postgresql.jar", config.getConnectorJarUrl());

        config.setDeletionTimestamp(2L);
        assertEquals(2L, config.getDeletionTimestamp().longValue());

        config.setArchived(true);
        assertTrue(config.isArchived());

        config.setEnvironmentId("myenvironment");
        assertEquals("myenvironment", config.getEnvironmentId());
    }

    @Test
    public void testGetResource() {
        assertEquals(WorkspaceResource.DATABASE_SERVER, config.getResource());
    }

    @Test
    public void testHasWhereAnnotation() {
        Where whereAnnotation = DatabaseServerConfig.class.getAnnotation(Where.class);

        assertNotNull(whereAnnotation);
        assertEquals("archived = false", whereAnnotation.clause());
    }
}
