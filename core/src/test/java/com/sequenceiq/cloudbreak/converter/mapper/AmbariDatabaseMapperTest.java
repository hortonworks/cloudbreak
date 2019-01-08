package com.sequenceiq.cloudbreak.converter.mapper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.Test;
import org.mapstruct.factory.Mappers;

import com.sequenceiq.cloudbreak.api.model.AmbariDatabaseDetailsJson;
import com.sequenceiq.cloudbreak.api.model.DatabaseVendor;
import com.sequenceiq.cloudbreak.api.model.ResourceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.cloud.model.AmbariDatabase;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.RDSConfig;

public class AmbariDatabaseMapperTest {

    private AmbariDatabaseMapper mapper;

    private AmbariDatabaseDetailsJson json;

    private Cluster cluster;

    @Before
    public void setUp() {
        mapper = Mappers.getMapper(AmbariDatabaseMapper.class);
        json = new AmbariDatabaseDetailsJson();
        json.setHost("testHost");
        json.setPort(1234);
        json.setName("testName");
        json.setVendor(DatabaseVendor.POSTGRES);
        cluster = new Cluster();
        cluster.setId(1L);
    }

    @Test
    public void testMapAmbariDatabaseDetailsJsonToRdsConfig() {
        RDSConfig rdsConfig = mapper.mapAmbariDatabaseDetailsJsonToRdsConfig(json, cluster, null);
        assertEquals(mapper.mapName(null, cluster), rdsConfig.getName());
        assertEquals(mapper.mapConnectionUrl(json), rdsConfig.getConnectionURL());
        assertEquals(ResourceStatus.USER_MANAGED, rdsConfig.getStatus());
        assertEquals(DatabaseType.AMBARI.name(), rdsConfig.getType());
        assertEquals("org.postgresql.Driver", rdsConfig.getConnectionDriver());
        assertNull(rdsConfig.getId());
        assertNull(rdsConfig.getClusters());
        assertNull(rdsConfig.getStackVersion());
        assertNotNull(rdsConfig.getCreationDate());
    }

    @Test
    public void testMapConnectionUrl() {
        String url = mapper.mapConnectionUrl(json);
        assertEquals("jdbc:postgresql://" + json.getHost() + ':'
                + json.getPort() + '/' + json.getName(), url);
    }

    @Test
    public void testMapName() {
        String name = mapper.mapName(null, cluster);
        assertEquals(DatabaseType.AMBARI.name() + "_CLUSTER_" + cluster.getId(), name);
    }

    @Test
    public void testMapAmbariDatabaseToAmbariDatabaseDetailJson() {
        AmbariDatabase ambariDatabase = new AmbariDatabase();
        ambariDatabase.setVendor(DatabaseVendor.POSTGRES.databaseType());
        AmbariDatabaseDetailsJson ambariDatabaseDetailsJson = mapper.mapAmbariDatabaseToAmbariDatabaseDetailJson(ambariDatabase);
        assertEquals(mapper.mapVendorByValue(DatabaseVendor.POSTGRES.databaseType()), ambariDatabaseDetailsJson.getVendor());
    }

    @Test
    public void testMapVendorByValue() {
        assertNull(mapper.mapVendorByValue(null));
        assertEquals(DatabaseVendor.POSTGRES, mapper.mapVendorByValue(DatabaseVendor.POSTGRES.databaseType()));
    }
}