package com.sequenceiq.cloudbreak.service.rdsconfig;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.powermock.reflect.Whitebox;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceMetadataType;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;

@RunWith(MockitoJUnitRunner.class)
public class AbstractRdsConfigProviderTest {

    @Mock
    private RdsConfigService rdsConfigService;

    @Mock
    private ClusterService clusterService;

    @Mock
    private RedbeamsDbServerConfigurer dbServerConfigurer;

    private String dbHost = "dbsvr-ed671174-77de-40e5-ad59-37761d8230d9.c8uqzbscgqmb.eu-west-1.rds.amazonaws.com";

    @InjectMocks
    private ClouderaManagerRdsConfigProvider underTest;

    @Before
    public void setUp() throws Exception {
        Whitebox.setInternalState(underTest, "db", "clouderamanager");
    }

    @Test
    public void createservicePillarForLocalRdsConfig() {
        when(rdsConfigService.createIfNotExists(any(), any(), any())).thenAnswer(i -> i.getArguments()[1]);
        Stack testStack = TestUtil.stack();
        InstanceMetaData metaData = testStack.getGatewayInstanceMetadata().iterator().next();
        metaData.setInstanceMetadataType(InstanceMetadataType.GATEWAY_PRIMARY);
        testStack.getGatewayInstanceMetadata().add(metaData);
        Cluster testCluster = TestUtil.cluster();
        Map<String, Object> result = underTest.createServicePillarConfigMapIfNeeded(testStack, testCluster);
        Map<String, Object> postgresData = (Map<String, Object>) result.get("clouderamanager");
        assertEquals("clouderamanager", postgresData.get("database"));
        assertNull(postgresData.get("remote_db_url"));
        assertNotNull(postgresData.get("password"));
    }

    @Test
    public void createservicePillarForRemoteRdsConfig() {
        when(rdsConfigService.createIfNotExists(any(), any(), any())).thenAnswer(i -> i.getArguments()[1]);
        RDSConfig config = TestUtil.rdsConfig(DatabaseType.CLOUDERA_MANAGER);
        when(dbServerConfigurer.getRdsConfig(any(), any(), any(), any())).thenReturn(config);
        when(dbServerConfigurer.isRemoteDatabaseNeeded(any())).thenReturn(true);
        when(dbServerConfigurer.getHostFromJdbcUrl(any())).thenReturn(dbHost);
        when(dbServerConfigurer.getPortFromJdbcUrl(any())).thenReturn("1234");
        Stack testStack = TestUtil.stack();
        InstanceMetaData metaData = testStack.getGatewayInstanceMetadata().iterator().next();
        metaData.setInstanceMetadataType(InstanceMetadataType.GATEWAY_PRIMARY);
        testStack.getGatewayInstanceMetadata().add(metaData);
        Cluster testCluster = TestUtil.cluster();
        testStack.setCluster(testCluster);
        Map<String, Object> result = underTest.createServicePillarConfigMapIfNeeded(testStack, testCluster);
        Map<String, Object> postgresData = (Map<String, Object>) result.get("clouderamanager");
        assertEquals("clouderamanager", postgresData.get("database"));
        assertEquals(dbHost, postgresData.get("remote_db_url"));
        assertEquals("1234", postgresData.get("remote_db_port"));
        assertNotNull(postgresData.get("database"));
    }
}