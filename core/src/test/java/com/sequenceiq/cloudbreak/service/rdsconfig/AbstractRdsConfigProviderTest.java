package com.sequenceiq.cloudbreak.service.rdsconfig;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DatabaseVendor;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceMetadataType;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.view.RdsConfigWithoutCluster;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.secret.model.SecretResponse;
import com.sequenceiq.cloudbreak.service.secret.service.SecretService;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerV4Response;

@ExtendWith(MockitoExtension.class)
class AbstractRdsConfigProviderTest {

    private static final String DB_HOST = "dbsvr-ed671174-77de-40e5-ad59-37761d8230d9.c8uqzbscgqmb.eu-west-1.rds.amazonaws.com";

    private static final int DB_PORT = 1234;

    private static final String REMOTE_ADMIN = "admin";

    private static final String REMOTE_ADMIN_PASSWORD = "adminPassword";

    @Mock
    private RdsConfigWithoutClusterService rdsConfigWithoutClusterService;

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

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(underTest, "db", "clouderamanager");
    }

    @Test
    void createServicePillarForLocalRdsConfig() {
        when(rdsConfigService.createIfNotExists(any(), any(), any())).thenAnswer(i -> i.getArguments()[1]);
        RdsConfigWithoutCluster rdsConfigWithoutCluster = mock(RdsConfigWithoutCluster.class);
        when(rdsConfigWithoutCluster.getType()).thenReturn(DatabaseType.CLOUDERA_MANAGER.name());
        when(rdsConfigWithoutCluster.getStatus()).thenReturn(ResourceStatus.DEFAULT);
        when(rdsConfigWithoutCluster.getDatabaseEngine()).thenReturn(DatabaseVendor.POSTGRES);
        when(rdsConfigWithoutCluster.getConnectionPassword()).thenReturn("pwd");
        Stack testStack = TestUtil.stack();
        InstanceMetadataView metaData = testStack.getNotTerminatedAndNotZombieGatewayInstanceMetadata().iterator().next();
        testStack.getNotTerminatedAndNotZombieGatewayInstanceMetadata().add(metaData);
        Cluster testCluster = TestUtil.cluster();
        testCluster.setId(1L);
        testStack.setCluster(testCluster);
        testCluster.setRdsConfigs(new HashSet<>());
        when(rdsConfigWithoutClusterService.findByClusterId(anyLong())).thenReturn(Set.of(rdsConfigWithoutCluster));

        Map<String, Object> result = underTest.createServicePillarConfigMapIfNeeded(testStack, testCluster);

        ArgumentCaptor<RDSConfig> rdsConfigCaptor = ArgumentCaptor.forClass(RDSConfig.class);
        verify(rdsConfigService).createIfNotExists(any(), rdsConfigCaptor.capture(), any());

        assertEquals("CLOUDERA_MANAGER_simplestack1", rdsConfigCaptor.getValue().getName());
        assertEquals(1, rdsConfigCaptor.getValue().getClusters().size());
        assertEquals(1L, rdsConfigCaptor.getValue().getClusters().iterator().next().getId().longValue());

        Map<String, Object> postgresData = (Map<String, Object>) result.get("clouderamanager");
        assertEquals("clouderamanager", postgresData.get("database"));
        assertNull(postgresData.get("remote_db_url"));
        assertNotNull(postgresData.get("password"));
    }

    @Test
    void createServicePillarForRemoteRdsConfig() {
        when(rdsConfigService.createIfNotExists(any(), any(), any())).thenAnswer(i -> i.getArguments()[1]);
        RdsConfigWithoutCluster rdsConfigWithoutCluster = mock(RdsConfigWithoutCluster.class);
        when(rdsConfigWithoutCluster.getType()).thenReturn(DatabaseType.CLOUDERA_MANAGER.name());
        when(rdsConfigWithoutCluster.getStatus()).thenReturn(ResourceStatus.DEFAULT);
        when(rdsConfigWithoutCluster.getDatabaseEngine()).thenReturn(DatabaseVendor.POSTGRES);
        when(rdsConfigWithoutCluster.getConnectionPassword()).thenReturn("pwd");
        RDSConfig config = new RDSConfig();
        config.setType(DatabaseType.CLOUDERA_MANAGER.name());
        when(dbServerConfigurer.createNewRdsConfig(any(), any(), any(), any(), any(), any(), any())).thenReturn(config);
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
        InstanceMetaData metaData = (InstanceMetaData) testStack.getNotTerminatedAndNotZombieGatewayInstanceMetadata().iterator().next();
        metaData.setInstanceMetadataType(InstanceMetadataType.GATEWAY_PRIMARY);
        testStack.getNotTerminatedAndNotZombieGatewayInstanceMetadata().add(metaData);
        Cluster testCluster = TestUtil.cluster();
        testStack.setCluster(testCluster);
        when(rdsConfigWithoutClusterService.findByClusterId(anyLong())).thenReturn(Set.of(rdsConfigWithoutCluster));

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
