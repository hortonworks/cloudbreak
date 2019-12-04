package com.sequenceiq.cloudbreak.service.rdsconfig;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceMetadataType;
import com.sequenceiq.cloudbreak.common.database.DatabaseCommon;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.service.secret.service.SecretService;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerV4Response;

@RunWith(MockitoJUnitRunner.class)
public class RedbeamsDbServerConfigurerTest {

    private static final String EXAMPLE_JDBC_URL =
        "jdbc:postgresql://dbsvr-ed671174-77de-40e5-ad59-37761d8230d9.c8uqzbscgqmb.eu-west-1.rds.amazonaws.com:5432/clouderamanager";

    private static final String DB_SERVER_CRN = "crn:cdp:redbeams:us-west-1:default:databaseServer:e63520c8-aaf0-4bf3-b872-5613ce496ac3";

    private static final String DB_HOST = "dbsvr-ed671174-77de-40e5-ad59-37761d8230d9.c8uqzbscgqmb.eu-west-1.rds.amazonaws.com";

    private static final String DB_USER = "cmuser";

    private static final String EXAMPLE_JDBC_URL_AZURE =
        "jdbc:postgresql://dbsvr-ed671174-77de-40e5-ad59-37761d8230d9.postgres.database.azure.com:5432/clouderamanager";

    private static final String DB_HOST_AZURE = "dbsvr-ed671174-77de-40e5-ad59-37761d8230d9.postgres.database.azure.com";

    private static final String DB_HOST_SHORT_NAME = "dbsvr-ed671174-77de-40e5-ad59-37761d8230d9";

    @Mock
    private RedbeamsClientService redbeamsClientService;

    @Mock
    private SecretService secretService;

    @Mock
    private DatabaseCommon dbCommon;

    @Mock
    private DbUsernameConverterService dbUsernameConverterService;

    @InjectMocks
    private RedbeamsDbServerConfigurer underTest;

    @Test
    public void getRdsConfig() {
        DatabaseServerV4Response resp = new DatabaseServerV4Response();
        resp.setPort(1234);
        resp.setHost(DB_HOST);
        resp.setDatabaseVendor("postgres");
        when(redbeamsClientService.getByCrn(DB_SERVER_CRN)).thenReturn(resp);
        when(dbCommon.getJdbcConnectionUrl(any(), any(), anyInt(), any())).thenReturn(EXAMPLE_JDBC_URL);
        when(dbUsernameConverterService.toConnectionUsername(anyString(), anyString())).thenReturn(DB_USER);
        Stack testStack = TestUtil.stack();
        InstanceMetaData metaData = testStack.getGatewayInstanceMetadata().iterator().next();
        metaData.setInstanceMetadataType(InstanceMetadataType.GATEWAY_PRIMARY);
        testStack.getGatewayInstanceMetadata().add(metaData);
        Cluster testCluster = TestUtil.cluster();
        testCluster.setDatabaseServerCrn(DB_SERVER_CRN);
        testStack.setCluster(testCluster);

        RDSConfig config = underTest.createNewRdsConfig(testStack, testCluster, "clouderamanager", DB_USER, DatabaseType.CLOUDERA_MANAGER);

        assertEquals("CLOUDERA_MANAGER_simplestack1", config.getName());
        assertEquals(EXAMPLE_JDBC_URL, config.getConnectionURL());
        assertEquals(DB_USER, config.getConnectionUserName());
    }

    @Test
    public void getRdsConfigForAzure() {
        DatabaseServerV4Response resp = new DatabaseServerV4Response();
        resp.setPort(1234);
        resp.setHost(DB_HOST_AZURE);
        resp.setDatabaseVendor("postgres");
        when(redbeamsClientService.getByCrn(DB_SERVER_CRN)).thenReturn(resp);
        when(dbCommon.getJdbcConnectionUrl(any(), any(), anyInt(), any())).thenReturn(EXAMPLE_JDBC_URL_AZURE);
        when(dbUsernameConverterService.toConnectionUsername(anyString(), anyString())).thenReturn("cmuser@" + DB_HOST_SHORT_NAME);
        Stack testStack = TestUtil.stack();
        InstanceMetaData metaData = testStack.getGatewayInstanceMetadata().iterator().next();
        metaData.setInstanceMetadataType(InstanceMetadataType.GATEWAY_PRIMARY);
        testStack.getGatewayInstanceMetadata().add(metaData);
        Cluster testCluster = TestUtil.cluster();
        testCluster.setDatabaseServerCrn(DB_SERVER_CRN);
        testStack.setCluster(testCluster);

        RDSConfig config = underTest.createNewRdsConfig(testStack, testCluster, "clouderamanager", DB_USER, DatabaseType.CLOUDERA_MANAGER);

        assertEquals("CLOUDERA_MANAGER_simplestack1", config.getName());
        assertEquals(EXAMPLE_JDBC_URL_AZURE, config.getConnectionURL());
        assertEquals("cmuser@" + DB_HOST_SHORT_NAME, config.getConnectionUserName());
    }

    @Test
    public void isRemoteDatabaseNeededWhenDbServerCrnIsPresent() {
        Cluster testCluster = TestUtil.cluster();
        testCluster.setDatabaseServerCrn(DB_SERVER_CRN);
        assertTrue(underTest.isRemoteDatabaseNeeded(testCluster));
    }

    @Test
    public void isRemoteDatabaseNeeded() {
        Cluster testCluster = TestUtil.cluster();
        assertFalse(underTest.isRemoteDatabaseNeeded(testCluster));
    }
}
