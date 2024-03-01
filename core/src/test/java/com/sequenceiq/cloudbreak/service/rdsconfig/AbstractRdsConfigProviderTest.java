package com.sequenceiq.cloudbreak.service.rdsconfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
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
import com.sequenceiq.cloudbreak.domain.RdsSslMode;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.view.RdsConfigWithoutCluster;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.sdx.common.PlatformAwareSdxConnector;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.cluster.EmbeddedDatabaseService;
import com.sequenceiq.cloudbreak.service.secret.domain.RotationSecret;
import com.sequenceiq.cloudbreak.service.secret.model.SecretResponse;
import com.sequenceiq.cloudbreak.service.secret.service.SecretService;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerV4Response;

@ExtendWith(MockitoExtension.class)
class AbstractRdsConfigProviderTest {

    private static final String DB_HOST = "dbsvr-ed671174-77de-40e5-ad59-37761d8230d9.c8uqzbscgqmb.eu-west-1.rds.amazonaws.com";

    private static final int DB_PORT = 1234;

    private static final String REMOTE_ADMIN = "admin";

    private static final String REMOTE_ADMIN_PASSWORD = "adminPassword";

    private static final long CLUSTER_ID = 1L;

    private static final long RDS_CONFIG_ID = 23L;

    private static final String ROTATION_USER_PATH = "userpath";

    private static final String ROTATION_PASSWORD_PATH = "passpath";

    private static final String ROTATION_NEW_USER = "newuser";

    private static final String ROTATION_OLD_USER = "olduser";

    private static final String ROTATION_NEW_PASS = "newpass";

    private static final String ROTATION_OLD_PASS = "oldpass";

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
    private EmbeddedDatabaseService embeddedDatabaseService;

    @Mock
    private DbUsernameConverterService dbUsernameConverterService;

    @Mock
    private PlatformAwareSdxConnector platformAwareSdxConnector;

    @InjectMocks
    private ClouderaManagerRdsConfigProvider underTest;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(underTest, "db", "clouderamanager");
        when(platformAwareSdxConnector.getSdxCrnByEnvironmentCrn(any())).thenReturn(Optional.empty());
    }

    static Object[][] sslDataProvider() {
        return new Object[][]{
                // sslEnforcement, rdsSslModeExpected
                {false, RdsSslMode.DISABLED},
                {true, RdsSslMode.ENABLED},
        };
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("sslDataProvider")
    void createServicePillarForLocalRdsConfig(boolean sslEnforcement, RdsSslMode rdsSslModeExpected) {
        when(rdsConfigService.createIfNotExists(any(), any(), any())).thenAnswer(i -> {
            RDSConfig rdsConfig = i.getArgument(1, RDSConfig.class);
            rdsConfig.setId(RDS_CONFIG_ID);
            return rdsConfig;
        });
        RdsConfigWithoutCluster rdsConfigWithoutCluster = mock(RdsConfigWithoutCluster.class);
        when(rdsConfigWithoutCluster.getType()).thenReturn(DatabaseType.CLOUDERA_MANAGER.name());
        when(rdsConfigWithoutCluster.getStatus()).thenReturn(ResourceStatus.DEFAULT);
        when(rdsConfigWithoutCluster.getDatabaseEngine()).thenReturn(DatabaseVendor.POSTGRES);
        when(rdsConfigWithoutCluster.getConnectionPassword()).thenReturn("pwd");
        Stack testStack = TestUtil.stack();
        StackDto stackDto = mock(StackDto.class);
        InstanceMetaData metaData = new InstanceMetaData();
        metaData.setInstanceMetadataType(InstanceMetadataType.GATEWAY_PRIMARY);
        metaData.setDiscoveryFQDN("fqdn");
        when(stackDto.getPrimaryGatewayInstance()).thenReturn(metaData);
        Cluster testCluster = TestUtil.cluster();
        testCluster.setId(CLUSTER_ID);
        when(stackDto.getCluster()).thenReturn(testCluster);
        when(stackDto.getStack()).thenReturn(testStack);
        when(rdsConfigWithoutClusterService.findByClusterId(anyLong())).thenReturn(Set.of(rdsConfigWithoutCluster));
        when(embeddedDatabaseService.isSslEnforcementForEmbeddedDatabaseEnabled(testStack, testCluster, null)).thenReturn(sslEnforcement);

        Map<String, Object> result = underTest.createServicePillarConfigMapIfNeeded(stackDto);

        ArgumentCaptor<RDSConfig> rdsConfigCaptor = ArgumentCaptor.forClass(RDSConfig.class);
        verify(rdsConfigService).createIfNotExists(any(), rdsConfigCaptor.capture(), any());

        assertEquals("CLOUDERA_MANAGER_simplestack1", rdsConfigCaptor.getValue().getName());
        assertEquals(1, rdsConfigCaptor.getValue().getClusters().size());
        assertEquals(CLUSTER_ID, rdsConfigCaptor.getValue().getClusters().iterator().next().getId().longValue());
        assertThat(rdsConfigCaptor.getValue().getSslMode()).isEqualTo(rdsSslModeExpected);

        Map<String, Object> postgresData = (Map<String, Object>) result.get("clouderamanager");
        assertEquals("clouderamanager", postgresData.get("database"));
        assertNull(postgresData.get("remote_db_url"));
        assertNotNull(postgresData.get("password"));

        verify(clusterService).addRdsConfigToCluster(RDS_CONFIG_ID, CLUSTER_ID);
    }

    @Test
    void createServicePillarForRemoteRdsConfig() {
        when(rdsConfigService.createIfNotExists(any(), any(), any())).thenAnswer(i -> {
            RDSConfig rdsConfig = i.getArgument(1, RDSConfig.class);
            rdsConfig.setId(RDS_CONFIG_ID);
            return rdsConfig;
        });
        RdsConfigWithoutCluster rdsConfigWithoutCluster = mock(RdsConfigWithoutCluster.class);
        when(rdsConfigWithoutCluster.getType()).thenReturn(DatabaseType.CLOUDERA_MANAGER.name());
        when(rdsConfigWithoutCluster.getStatus()).thenReturn(ResourceStatus.DEFAULT);
        when(rdsConfigWithoutCluster.getDatabaseEngine()).thenReturn(DatabaseVendor.POSTGRES);
        when(rdsConfigWithoutCluster.getConnectionPassword()).thenReturn("pwd");
        RDSConfig config = new RDSConfig();
        config.setType(DatabaseType.CLOUDERA_MANAGER.name());
        when(dbServerConfigurer.createNewRdsConfig(any(), any(), any(), any(), any(), any(), any())).thenReturn(config);
        when(dbServerConfigurer.isRemoteDatabaseRequested(any())).thenReturn(true);
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
        StackDto stackDto = mock(StackDto.class);
        Cluster testCluster = TestUtil.cluster();
        testCluster.setId(CLUSTER_ID);
        when(stackDto.getCluster()).thenReturn(testCluster);
        when(stackDto.getStack()).thenReturn(testStack);
        when(rdsConfigWithoutClusterService.findByClusterId(anyLong())).thenReturn(Set.of(rdsConfigWithoutCluster));

        Map<String, Object> result = underTest.createServicePillarConfigMapIfNeeded(stackDto);

        ArgumentCaptor<RDSConfig> rdsConfigCaptor = ArgumentCaptor.forClass(RDSConfig.class);
        verify(rdsConfigService).createIfNotExists(any(), rdsConfigCaptor.capture(), any());

        assertThat(rdsConfigCaptor.getValue()).isSameAs(config);

        Map<String, Object> postgresData = (Map<String, Object>) result.get("clouderamanager");
        assertEquals("clouderamanager", postgresData.get("database"));
        assertEquals(REMOTE_ADMIN, postgresData.get("remote_admin"));
        assertEquals(REMOTE_ADMIN_PASSWORD, postgresData.get("remote_admin_pw"));
        assertEquals(DB_HOST, postgresData.get("remote_db_url"));
        assertEquals(DB_PORT, postgresData.get("remote_db_port"));
        assertNotNull(postgresData.get("password"));

        verify(clusterService).addRdsConfigToCluster(RDS_CONFIG_ID, CLUSTER_ID);
    }

    @Test
    void createRotationPillarForRemoteRdsConfig() {
        RdsConfigWithoutCluster rdsConfigWithoutCluster = mock(RdsConfigWithoutCluster.class);
        when(rdsConfigWithoutCluster.getType()).thenReturn(DatabaseType.CLOUDERA_MANAGER.name());
        when(rdsConfigWithoutCluster.getStatus()).thenReturn(ResourceStatus.DEFAULT);
        when(rdsConfigWithoutCluster.getDatabaseEngine()).thenReturn(DatabaseVendor.POSTGRES);
        when(rdsConfigWithoutCluster.getConnectionUserNamePath()).thenReturn(ROTATION_USER_PATH);
        when(rdsConfigWithoutCluster.getConnectionPasswordPath()).thenReturn(ROTATION_PASSWORD_PATH);
        when(secretService.getRotation(eq(ROTATION_USER_PATH))).thenReturn(new RotationSecret(ROTATION_NEW_USER, ROTATION_OLD_USER));
        when(secretService.getRotation(eq(ROTATION_PASSWORD_PATH))).thenReturn(new RotationSecret(ROTATION_NEW_PASS, ROTATION_OLD_PASS));
        when(dbUsernameConverterService.toDatabaseUsername(eq(ROTATION_NEW_USER))).thenReturn(ROTATION_NEW_USER);
        when(dbUsernameConverterService.toDatabaseUsername(eq(ROTATION_OLD_USER))).thenReturn(ROTATION_OLD_USER);
        when(rdsConfigService.existsByClusterIdAndType(anyLong(), any(DatabaseType.class))).thenReturn(Boolean.TRUE);
        when(rdsConfigWithoutClusterService.findByClusterId(anyLong())).thenReturn(Set.of(rdsConfigWithoutCluster));
        StackDto stackDto = mock(StackDto.class);
        ClusterView clusterView = mock(ClusterView.class);
        when(clusterView.getId()).thenReturn(CLUSTER_ID);
        when(stackDto.getCluster()).thenReturn(clusterView);

        Map<String, Object> result = underTest.createServicePillarConfigMapForRotation(stackDto);

        Map<String, Object> postgresData = (Map<String, Object>) result.get("clouderamanager");
        assertEquals("clouderamanager", postgresData.get("database"));
        assertEquals(ROTATION_OLD_USER, postgresData.get("oldUser"));
        assertEquals(ROTATION_OLD_PASS, postgresData.get("oldPassword"));
        assertEquals(ROTATION_NEW_USER, postgresData.get("newUser"));
        assertEquals(ROTATION_NEW_PASS, postgresData.get("newPassword"));
    }

}
