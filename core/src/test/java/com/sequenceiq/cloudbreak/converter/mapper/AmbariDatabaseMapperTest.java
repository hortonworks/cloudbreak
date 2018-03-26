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
import com.sequenceiq.cloudbreak.api.model.rds.RdsType;
import com.sequenceiq.cloudbreak.cloud.model.AmbariDatabase;
import com.sequenceiq.cloudbreak.domain.Cluster;
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
        RDSConfig rdsConfig = mapper.mapAmbariDatabaseDetailsJsonToRdsConfig(json, cluster, false);
        assertEquals(mapper.mapName(cluster), rdsConfig.getName());
        assertEquals(mapper.mapConnectionUrl(json), rdsConfig.getConnectionURL());
        assertEquals(ResourceStatus.USER_MANAGED, rdsConfig.getStatus());
        assertEquals(RdsType.AMBARI.name(), rdsConfig.getType());
        assertEquals("org.postgresql.Driver", rdsConfig.getConnectionDriver());
        assertNull(rdsConfig.getId());
        assertNull(rdsConfig.getClusters());
        assertNull(rdsConfig.getStackVersion());
        assertNotNull(rdsConfig.getCreationDate());
    }

    @Test
    public void testMapConnectionUrl() {
        String url = mapper.mapConnectionUrl(json);
        assertEquals("jdbc:postgresql://" + json.getHost() + ":"
                + json.getPort() + "/" + json.getName(), url);
    }

    @Test
    public void testMapName() {
        String name = mapper.mapName(cluster);
        assertEquals(RdsType.AMBARI.name() + cluster.getId(), name);
    }

    @Test
    public void testMapAmbariDatabaseToAmbariDatabaseDetailJson() {
        AmbariDatabase ambariDatabase = new AmbariDatabase();
        ambariDatabase.setVendor(DatabaseVendor.POSTGRES.ambariVendor());
        AmbariDatabaseDetailsJson ambariDatabaseDetailsJson = mapper.mapAmbariDatabaseToAmbariDatabaseDetailJson(ambariDatabase);
        assertEquals(mapper.mapVendorByValue(DatabaseVendor.POSTGRES.ambariVendor()), ambariDatabaseDetailsJson.getVendor());
    }

    @Test
    public void testMapVendorByValue() {
        assertNull(mapper.mapVendorByValue(null));
        assertEquals(DatabaseVendor.POSTGRES, mapper.mapVendorByValue(DatabaseVendor.POSTGRES.ambariVendor()));
    }
}