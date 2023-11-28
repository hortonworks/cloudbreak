package com.sequenceiq.cloudbreak.core.flow2.cluster.rds.upgrade;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType.DATALAKE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType.WORKLOAD;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.cluster.api.ClusterModificationService;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.AbstractRdsRoleConfigProvider;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.hive.HiveMetastoreConfigProvider;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.hive.HiveRoles;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.knox.KnoxServiceConfigProvider;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.ranger.RangerRoleConfigProvider;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.ranger.RangerRoles;
import com.sequenceiq.cloudbreak.core.bootstrap.service.container.postgres.PostgresConfigService;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.ClusterHostServiceRunner;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel;
import com.sequenceiq.cloudbreak.rotation.ExitCriteriaProvider;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.rdsconfig.AbstractRdsConfigProvider;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;
import com.sequenceiq.cloudbreak.service.salt.SaltStateParamsService;

@ExtendWith(MockitoExtension.class)
class RdsSettingsMigrationServiceTest {
    private static final String USER_NAME = "originalUserName";

    private static final long CLUSTER_ID = 345L;

    @InjectMocks
    private RdsSettingsMigrationService underTest;

    @Mock
    private HostOrchestrator hostOrchestrator;

    @Mock
    private ClusterHostServiceRunner clusterHostServiceRunner;

    @Mock
    private SaltStateParamsService saltStateParamsService;

    @Mock
    private ExitCriteriaProvider exitCriteriaProvider;

    @Mock
    private GatewayConfigService gatewayConfigService;

    @Mock
    private PostgresConfigService postgresConfigService;

    @Mock
    private RdsConfigService rdsConfigService;

    @Mock
    private StackDto stackDto;

    @Mock
    private GatewayConfig primaryGatewayConfig;

    @Mock
    private AbstractRdsConfigProvider rdsConfigProvider1;

    @Mock
    private AbstractRdsConfigProvider rdsConfigProvider2;

    @Mock
    private AbstractRdsConfigProvider rdsConfigProvider3;

    @Mock
    private ClusterApiConnectors clusterApiConnectors;

    @BeforeEach
    void before() throws IllegalAccessException {
        FieldUtils.writeField(underTest, "rdsConfigProviders", Set.of(rdsConfigProvider1, rdsConfigProvider2, rdsConfigProvider3), true);
        lenient().when(rdsConfigProvider1.getRdsType()).thenReturn(DatabaseType.CLOUDERA_MANAGER);
        lenient().when(rdsConfigProvider2.getRdsType()).thenReturn(DatabaseType.HIVE);
        lenient().when(rdsConfigProvider3.getRdsType()).thenReturn(DatabaseType.NIFIREGISTRY);
    }

    @Test
    void collectRdsConfigs() {
        setUpStack(WORKLOAD);
        RDSConfig rdsConfig1 = createRDSConfig(0L, DatabaseType.CLOUDERA_MANAGER, USER_NAME + "@cuttable");
        RDSConfig rdsConfig2 = createRDSConfig(1L, DatabaseType.HIVE, USER_NAME);
        RDSConfig rdsConfig3 = createRDSConfig(2L, DatabaseType.NIFIREGISTRY, USER_NAME + "@cuttable");
        RDSConfig rdsConfig4 = createRDSConfig(3L, DatabaseType.HUE, USER_NAME + "@cuttable");

        when(rdsConfigService.findByClusterId(CLUSTER_ID)).thenReturn(Set.of(rdsConfig1, rdsConfig2, rdsConfig3, rdsConfig4));

        Set<RDSConfig> actualRdsConfigs = underTest.collectRdsConfigs(CLUSTER_ID, this::isClouderaManager);

        assertEquals(1, actualRdsConfigs.size());
        assertEquals(DatabaseType.CLOUDERA_MANAGER.name(), actualRdsConfigs.iterator().next().getType());
        verify(rdsConfigService).findByClusterId(CLUSTER_ID);
    }

    @Test
    void updateRdsConfigsDatalake() {
        setUpStack(DATALAKE);
        RDSConfig rdsConfig1 = createRDSConfig(0L, DatabaseType.CLOUDERA_MANAGER, USER_NAME + "@cuttable");
        RDSConfig rdsConfig2 = createRDSConfig(1L, DatabaseType.HIVE, USER_NAME);
        RDSConfig rdsConfig3 = createRDSConfig(2L, DatabaseType.NIFIREGISTRY, USER_NAME + "@cuttable");
        RDSConfig rdsConfig4 = createRDSConfig(3L, DatabaseType.HUE, USER_NAME + "@cuttable");

        Set<RDSConfig> rdsConfigs = new LinkedHashSet<>();
        rdsConfigs.add(rdsConfig1);
        rdsConfigs.add(rdsConfig2);
        rdsConfigs.add(rdsConfig3);
        rdsConfigs.add(rdsConfig4);

        Set<RDSConfig> updatedRdsConfigs = underTest.updateRdsConfigs(stackDto, rdsConfigs);

        verify(rdsConfigService, times(1)).pureSaveAll(anySet());
        assertEquals(4, updatedRdsConfigs.size());
        Iterator<RDSConfig> iterator = updatedRdsConfigs.iterator();
        assertEquals(USER_NAME, iterator.next().getConnectionUserName());
        assertEquals(USER_NAME, iterator.next().getConnectionUserName());
        assertEquals(USER_NAME, iterator.next().getConnectionUserName());
        assertEquals(USER_NAME, iterator.next().getConnectionUserName());
        assertFalse(iterator.hasNext());
    }

    @Test
    void updateRdsConfigsWorkload() {
        setUpStack(WORKLOAD);
        RDSConfig rdsConfig1 = createRDSConfig(0L, DatabaseType.CLOUDERA_MANAGER, USER_NAME + "@cuttable");
        RDSConfig rdsConfig2 = createRDSConfig(1L, DatabaseType.HIVE, USER_NAME);
        RDSConfig rdsConfig3 = createRDSConfig(2L, DatabaseType.NIFIREGISTRY, USER_NAME + "@cuttable");
        RDSConfig rdsConfig4 = createRDSConfig(3L, DatabaseType.HUE, USER_NAME + "@cuttable");

        when(rdsConfigService.getClustersUsingResource(rdsConfig1)).thenReturn(Set.of(mock(Cluster.class)));
        when(rdsConfigService.getClustersUsingResource(rdsConfig2)).thenReturn(Set.of(mock(Cluster.class), mock(Cluster.class)));
        when(rdsConfigService.getClustersUsingResource(rdsConfig3)).thenReturn(Set.of(mock(Cluster.class)));

        Set<RDSConfig> rdsConfigs = new LinkedHashSet<>();
        rdsConfigs.add(rdsConfig1);
        rdsConfigs.add(rdsConfig2);
        rdsConfigs.add(rdsConfig3);
        rdsConfigs.add(rdsConfig4);

        Set<RDSConfig> updatedRdsConfigs = underTest.updateRdsConfigs(stackDto, rdsConfigs);

        verify(rdsConfigService, times(1)).pureSaveAll(anySet());
        assertEquals(3, updatedRdsConfigs.size());
        Iterator<RDSConfig> iterator = updatedRdsConfigs.iterator();
        assertEquals(USER_NAME, iterator.next().getConnectionUserName());
        assertEquals(USER_NAME, iterator.next().getConnectionUserName());
        assertEquals(USER_NAME, iterator.next().getConnectionUserName());
        assertFalse(iterator.hasNext());
    }

    @Test
    void collectCMServiceConfigs() throws Exception {
        AbstractRdsRoleConfigProvider roleConfigProvider1 = new KnoxServiceConfigProvider();
        AbstractRdsRoleConfigProvider roleConfigProvider2 = new HiveMetastoreConfigProvider();
        AbstractRdsRoleConfigProvider roleConfigProvider3 = new RangerRoleConfigProvider();
        FieldUtils.writeField(underTest, "rdsRoleConfigProviders", Set.of(roleConfigProvider1, roleConfigProvider2, roleConfigProvider3), true);

        RDSConfig rdsConfig1 = createRDSConfig(0L, DatabaseType.CLOUDERA_MANAGER, USER_NAME + "@cuttable");
        RDSConfig rdsConfig2 = createRDSConfig(1L, DatabaseType.HIVE, USER_NAME);
        RDSConfig rdsConfig3 = createRDSConfig(2L, DatabaseType.NIFIREGISTRY, USER_NAME + "@cuttable");
        RDSConfig rdsConfig4 = createRDSConfig(3L, DatabaseType.RANGER, USER_NAME + "@cuttable");

        Table<String, String, String> actualResult = underTest.collectCMServiceConfigs(Set.of(rdsConfig1, rdsConfig2, rdsConfig3, rdsConfig4));
        assertEquals(2, actualResult.size());
        assertEquals(USER_NAME, actualResult.get(HiveRoles.HIVE, "hive_metastore_database_user"));
        assertEquals(USER_NAME + "@cuttable", actualResult.get(RangerRoles.RANGER, "ranger_database_user"));
    }

    @Test
    void updateCMServiceConfigs() throws Exception {
        ClusterApi clusterApi = mock(ClusterApi.class);
        StackDto stackDto = mock(StackDto.class);
        ClusterModificationService clusterModificationService = mock(ClusterModificationService.class);
        when(clusterApi.clusterModificationService()).thenReturn(clusterModificationService);
        when(clusterApiConnectors.getConnector(any(StackDto.class))).thenReturn(clusterApi);
        Table<String, String, String> cmServiceConfigs = HashBasedTable.create();
        underTest.updateCMServiceConfigs(stackDto, cmServiceConfigs);
        verify(clusterModificationService, times(1)).updateConfigWithoutRestart(cmServiceConfigs);
    }

    @Test
    void updateSaltPillars() throws Exception {
        // GIVEN
        when(clusterHostServiceRunner.getClouderaManagerDatabasePillarProperties(any())).thenReturn(new SaltPillarProperties("path", Map.of()));
        when(postgresConfigService.getPostgreSQLServerPropertiesForRotation(stackDto)).thenReturn(new SaltPillarProperties("path1", Map.of()));
        // WHEN
        underTest.updateSaltPillars(stackDto, CLUSTER_ID);
        // THEN
        ArgumentCaptor<SaltConfig> saltConfigArgumentCaptor = ArgumentCaptor.forClass(SaltConfig.class);
        verify(hostOrchestrator, times(1)).saveCustomPillars(saltConfigArgumentCaptor.capture(), isNull(), isNull());
        SaltConfig saltConfig = saltConfigArgumentCaptor.getValue();
        assertEquals("path", saltConfig.getServicePillarConfig().get(ClusterHostServiceRunner.CM_DATABASE_PILLAR_KEY).getPath());
        assertEquals("path1", saltConfig.getServicePillarConfig().get(PostgresConfigService.POSTGRESQL_SERVER).getPath());
    }

    @Test
    void updateCMDatabaseConfiguration() throws Exception {
        // GIVEN
        ExitCriteriaModel exitCriteriaModel = mock(ExitCriteriaModel.class);
        when(primaryGatewayConfig.getHostname()).thenReturn("hostname");
        when(gatewayConfigService.getPrimaryGatewayConfig(stackDto)).thenReturn(primaryGatewayConfig);
        when(exitCriteriaProvider.get(stackDto)).thenReturn(exitCriteriaModel);
        // WHEN
        underTest.updateCMDatabaseConfiguration(stackDto);
        // THEN
        verify(hostOrchestrator).executeSaltState(primaryGatewayConfig, Set.of("hostname"), List.of("cloudera.manager.update-db-user"),
                exitCriteriaProvider.get(stackDto), Optional.of(10), Optional.of(3));
    }

    private RDSConfig createRDSConfig(long id, DatabaseType databaseType, String connectionUserName) {
        RDSConfig rdsConfig = new RDSConfig();
        rdsConfig.setId(id);
        rdsConfig.setName("rds-config-" + id);
        rdsConfig.setType(databaseType.name());
        rdsConfig.setConnectionUserName(connectionUserName);
        return rdsConfig;
    }

    private void setUpStack(StackType stackType) {
        Stack stack = new Stack();
        stack.setType(stackType);
        lenient().when(stackDto.getStack()).thenReturn(stack);
        Cluster cluster = new Cluster();
        cluster.setId(CLUSTER_ID);
        lenient().when(stackDto.getCluster()).thenReturn(cluster);
    }

    private boolean isClouderaManager(RDSConfig rdsConfig) {
        return DatabaseType.CLOUDERA_MANAGER.name().equals(rdsConfig.getType());
    }
}