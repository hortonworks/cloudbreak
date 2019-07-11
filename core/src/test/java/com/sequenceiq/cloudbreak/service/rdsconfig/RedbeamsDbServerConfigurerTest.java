package com.sequenceiq.cloudbreak.service.rdsconfig;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceMetadataType;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.service.secret.service.SecretService;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerV4Response;

@RunWith(MockitoJUnitRunner.class)
public class RedbeamsDbServerConfigurerTest {

    private String exampleJdbcUrl = "jdbc:postgresql://dbsvr-ed671174-77de-40e5-ad59-37761d8230d9.c8uqzbscgqmb.eu-west-1.rds.amazonaws.com:5432/hive";

    private String dbServerCrn = "crn:altus:redbeams:us-west-1:default:databaseServer:e63520c8-aaf0-4bf3-b872-5613ce496ac3";

    private String dbHost = "dbsvr-ed671174-77de-40e5-ad59-37761d8230d9.c8uqzbscgqmb.eu-west-1.rds.amazonaws.com";

    @Mock
    private RedbeamsClientService redbeamsClientService;

    @Mock
    private SecretService secretService;

    @InjectMocks
    private RedbeamsDbServerConfigurer underTest;

    @Test
    public void getRdsConfig() {
        DatabaseServerV4Response resp = new DatabaseServerV4Response();
        resp.setPort(1234);
        resp.setHost(dbHost);
        when(redbeamsClientService.getByCrn(dbServerCrn)).thenReturn(resp);
        Stack testStack = TestUtil.stack();
        InstanceMetaData metaData = testStack.getGatewayInstanceMetadata().iterator().next();
        metaData.setInstanceMetadataType(InstanceMetadataType.GATEWAY_PRIMARY);
        testStack.getGatewayInstanceMetadata().add(metaData);
        Cluster testCluster = TestUtil.cluster();
        testCluster.setDatabaseServerCrn(dbServerCrn);
        testStack.setCluster(testCluster);
        RDSConfig config = underTest.getRdsConfig(testStack, testCluster, "clouderamanager", DatabaseType.CLOUDERA_MANAGER);
        assertEquals("CLOUDERA_MANAGER_simplestack1", config.getName());
        assertEquals("jdbc:postgresql://dbsvr-ed671174-77de-40e5-ad59-37761d8230d9.c8uqzbscgqmb.eu-west-1.rds.amazonaws.com:1234/clouderamanager",
                config.getConnectionURL());
    }

    @Test
    public void getRemoteDbUrl() {
        String result = underTest.getHostFromJdbcUrl(exampleJdbcUrl);
        assertEquals(result, "dbsvr-ed671174-77de-40e5-ad59-37761d8230d9.c8uqzbscgqmb.eu-west-1.rds.amazonaws.com");
    }

    @Test
    public void getRemoteDbPort() {
        String result = underTest.getPortFromJdbcUrl(exampleJdbcUrl);
        assertEquals("5432", result);
    }

    @Test
    public void isRemoteDatabaseNeededWhenDbServerCrnIsPresent() {
        Cluster testCluster = TestUtil.cluster();
        testCluster.setDatabaseServerCrn(dbServerCrn);
        assertTrue(underTest.isRemoteDatabaseNeeded(testCluster));
    }

    @Test
    public void isRemoteDatabaseNeeded() {
        Cluster testCluster = TestUtil.cluster();
        assertFalse(underTest.isRemoteDatabaseNeeded(testCluster));
    }
}