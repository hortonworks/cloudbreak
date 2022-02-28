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
import com.sequenceiq.cloudbreak.service.secret.model.SecretResponse;
import com.sequenceiq.cloudbreak.service.secret.service.SecretService;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerV4Response;

@RunWith(MockitoJUnitRunner.class)
public class AbstractRdsConfigProviderTest {

    private static final String DB_HOST = "dbsvr-ed671174-77de-40e5-ad59-37761d8230d9.c8uqzbscgqmb.eu-west-1.rds.amazonaws.com";

    private static final int DB_PORT = 1234;

    private static final String REMOTE_ADMIN = "admin";

    private static final String REMOTE_ADMIN_PASSWORD = "adminPassword";

    @Mock
    private RdsConfigService rdsConfigService;

    @Mock
    private ClusterService clusterService;

    @Mock
    private SecretService secretService;

    @Mock
    private RedbeamsDbServerConfigurer dbServerConfigurer;

    @Mock
    private DbUsernameConverterService dbUsernameConverterService;

    @InjectMocks
    private ClouderaManagerRdsConfigProvider underTest;

    @Before
    public void setUp() throws Exception {
        Whitebox.setInternalState(underTest, "db", "clouderamanager");
    }

    @Test
    public void createServicePillarForLocalRdsConfig() {
        when(rdsConfigService.createIfNotExists(any(), any(), any())).thenAnswer(i -> i.getArguments()[1]);
        Stack testStack = TestUtil.stack();
        InstanceMetaData metaData = testStack.getNotTerminatedGatewayInstanceMetadata().iterator().next();
        metaData.setInstanceMetadataType(InstanceMetadataType.GATEWAY_PRIMARY);
        testStack.getNotTerminatedGatewayInstanceMetadata().add(metaData);
        Cluster testCluster = TestUtil.cluster();

        Map<String, Object> result = underTest.createServicePillarConfigMapIfNeeded(testStack, testCluster);

        Map<String, Object> postgresData = (Map<String, Object>) result.get("clouderamanager");
        assertEquals("clouderamanager", postgresData.get("database"));
        assertNull(postgresData.get("remote_db_url"));
        assertNotNull(postgresData.get("password"));
    }

    @Test
    public void createServicePillarForRemoteRdsConfig() {
        when(rdsConfigService.createIfNotExists(any(), any(), any())).thenAnswer(i -> i.getArguments()[1]);
        RDSConfig config = TestUtil.rdsConfig(DatabaseType.CLOUDERA_MANAGER);
        when(dbServerConfigurer.createNewRdsConfig(any(), any(), any(), any(), any())).thenReturn(config);
        when(dbServerConfigurer.isRemoteDatabaseNeeded(any())).thenReturn(true);
        DatabaseServerV4Response resp = new DatabaseServerV4Response();
        resp.setHost(DB_HOST);
        resp.setPort(DB_PORT);
        SecretResponse username = new SecretResponse("user", "name");
        SecretResponse password = new SecretResponse("pass", "word");
        resp.setConnectionUserName(username);
        resp.setConnectionPassword(password);
        when(dbServerConfigurer.getDatabaseServer(any())).thenReturn(resp);
        when(secretService.getByResponse(username)).thenReturn(REMOTE_ADMIN);
        when(secretService.getByResponse(password)).thenReturn(REMOTE_ADMIN_PASSWORD);
        Stack testStack = TestUtil.stack();
        InstanceMetaData metaData = testStack.getNotTerminatedGatewayInstanceMetadata().iterator().next();
        metaData.setInstanceMetadataType(InstanceMetadataType.GATEWAY_PRIMARY);
        testStack.getNotTerminatedGatewayInstanceMetadata().add(metaData);
        Cluster testCluster = TestUtil.cluster();
        testStack.setCluster(testCluster);

        Map<String, Object> result = underTest.createServicePillarConfigMapIfNeeded(testStack, testCluster);

        Map<String, Object> postgresData = (Map<String, Object>) result.get("clouderamanager");
        assertEquals("clouderamanager", postgresData.get("database"));
        assertEquals(REMOTE_ADMIN, postgresData.get("remote_admin"));
        assertEquals(REMOTE_ADMIN_PASSWORD, postgresData.get("remote_admin_pw"));
        assertEquals(DB_HOST, postgresData.get("remote_db_url"));
        assertEquals(DB_PORT, postgresData.get("remote_db_port"));
        assertNotNull(postgresData.get("password"));
    }
}
