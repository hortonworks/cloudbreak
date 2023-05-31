package com.sequenceiq.cloudbreak.service.upgrade.rds;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.springframework.util.ReflectionUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterDeletionBasedExitCriteriaModel;
import com.sequenceiq.cloudbreak.core.bootstrap.service.container.postgres.PostgresConfigService;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.host.OrchestratorStateParams;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.salt.SaltStateParamsService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.util.StackUtil;

@ExtendWith(MockitoExtension.class)
class RdsUpgradeOrchestratorServiceTest {

    private static final long STACK_ID = 123L;

    private static final String BACKUP_LOCATION = "location";

    private static final String BACKUP_INSTANCE_PROFILE = "BACKUP_INSTANCE_PROFILE";

    private static final String DATABASE_ENGINE_VERSION = "11";

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private UpgradeEmbeddedDBStateParamsProvider upgradeEmbeddedDBStateParamsProvider;

    @Mock
    private UpgradeEmbeddedDBPreparationStateParamsProvider upgradeEmbeddedDBPreparationStateParamsProvider;

    @Mock
    private BackupRestoreDBStateParamsProvider backupRestoreDBStateParamsProvider;

    @Mock
    private GatewayConfigService gatewayConfigService;

    @Mock
    private StackUtil stackUtil;

    @Mock
    private HostOrchestrator hostOrchestrator;

    @Mock
    private StackDto stack;

    @Mock
    private Cluster cluster;

    @Mock
    private UpgradeRdsBackupRestoreStateParamsProvider upgradeRdsBackupRestoreStateParamsProvider;

    @Mock
    private PostgresConfigService postgresConfigService;

    @Captor
    private ArgumentCaptor<OrchestratorStateParams> paramCaptor;

    @Captor
    private ArgumentCaptor<SaltConfig> saltConfigParamCaptor;

    @InjectMocks
    private RdsUpgradeOrchestratorService underTest;

    @Mock
    private GatewayConfig gatewayConfig;

    @Mock
    private SaltStateParamsService saltStateParamsService;

    @BeforeEach
    void setUp() {
        Node node1 = new Node("privateIP1", "publicIP1", "instance1", "instanceType1",
                "fqdn1", "hostgroup");
        Node node2 = new Node("privateIP2", "publicIP2", "instance2", "instanceType2",
                "fqdn2", "hostgroup");
        lenient().when(stack.getId()).thenReturn(STACK_ID);
        lenient().when(stack.getCluster()).thenReturn(cluster);
        lenient().when(stackDtoService.getById(STACK_ID)).thenReturn(stack);
        lenient().when(gatewayConfig.getHostname()).thenReturn("fqdn1");
        lenient().when(gatewayConfigService.getPrimaryGatewayConfig(any())).thenReturn(gatewayConfig);
        lenient().when(stackUtil.collectGatewayNodes(any())).thenReturn(Set.of(node1, node2));
    }

    @Test
    void testBackupRdsData() throws CloudbreakOrchestratorException {
        mockCreateStateParams();
        underTest.backupRdsData(STACK_ID, BACKUP_LOCATION, BACKUP_INSTANCE_PROFILE);
        verify(hostOrchestrator).runOrchestratorState(paramCaptor.capture());
        OrchestratorStateParams params = paramCaptor.getValue();
        assertThat(params.getState()).isEqualTo("postgresql/upgrade/backup");
        assertThat(params.getTargetHostNames()).hasSameElementsAs(Set.of("fqdn1"));
        assertOtherStateParams(params);
    }

    @Test
    void testRestoreRdsData() throws CloudbreakOrchestratorException {
        mockCreateStateParams();
        underTest.restoreRdsData(STACK_ID);
        verify(hostOrchestrator).runOrchestratorState(paramCaptor.capture());
        OrchestratorStateParams params = paramCaptor.getValue();
        assertThat(params.getState()).isEqualTo("postgresql/upgrade/restore");
        assertThat(params.getTargetHostNames()).hasSameElementsAs(Set.of("fqdn1"));
        assertOtherStateParams(params);
    }

    @Test
    void testValidateDbDirectorySpace() throws CloudbreakOrchestratorException {
        mockCreateStateParams();
        when(hostOrchestrator.runCommandOnHosts(anyList(), anySet(), eq("df -k /dbfs | tail -1 | awk '{print $4}'"))).thenReturn(Map.of("fqdn1", "100000"));
        when(hostOrchestrator.runCommandOnHosts(anyList(), anySet(), eq("du -sk /dbfs/pgsql | awk '{print $1}'"))).thenReturn(Map.of("fqdn1", "10000"));
        when(gatewayConfig.getHostname()).thenReturn("fqdn1");
        underTest.validateDbDirectorySpace(STACK_ID);
        verify(hostOrchestrator).runCommandOnHosts(anyList(), eq(Set.of("fqdn1")), eq("df -k /dbfs | tail -1 | awk '{print $4}'"));
        verify(hostOrchestrator).runCommandOnHosts(anyList(), eq(Set.of("fqdn1")), eq("du -sk /dbfs/pgsql | awk '{print $1}'"));
    }

    @Test
    void testValidateDbDirectorySpaceWhenNotEnoughSpace() throws CloudbreakOrchestratorException {
        mockCreateStateParams();
        when(hostOrchestrator.runCommandOnHosts(anyList(), anySet(), eq("df -k /dbfs | tail -1 | awk '{print $4}'"))).thenReturn(Map.of("fqdn1", "10000"));
        when(hostOrchestrator.runCommandOnHosts(anyList(), anySet(), eq("du -sk /dbfs/pgsql | awk '{print $1}'"))).thenReturn(Map.of("fqdn1", "100000"));
        when(gatewayConfig.getHostname()).thenReturn("fqdn1");
        CloudbreakOrchestratorException actualException = assertThrows(CloudbreakOrchestratorException.class,
                () -> underTest.validateDbDirectorySpace(STACK_ID));
        assertThat(actualException).hasMessageStartingWith("Not enough space on attached db volume for postgres upgrade.");
    }

    @Test
    void testValidateDbDirectorySpaceWhenNoPGWResult() throws CloudbreakOrchestratorException {
        mockCreateStateParams();
        when(hostOrchestrator.runCommandOnHosts(anyList(), anySet(), eq("df -k /dbfs | tail -1 | awk '{print $4}'"))).thenReturn(Map.of("fqdn2", "10000"));
        when(hostOrchestrator.runCommandOnHosts(anyList(), anySet(), eq("du -sk /dbfs/pgsql | awk '{print $1}'"))).thenReturn(Map.of("fqdn1", "100000"));
        when(gatewayConfig.getHostname()).thenReturn("fqdn1");
        CloudbreakOrchestratorException actualException = assertThrows(CloudbreakOrchestratorException.class,
                () -> underTest.validateDbDirectorySpace(STACK_ID));
        assertThat(actualException).hasMessageStartingWith("Space validation on attached db volume failed");
    }

    @Test
    void testValidateDbBackupSpace() throws CloudbreakOrchestratorException, JsonProcessingException {
        initGlobalPrivateFields();
        mockCreateStateParams();
        when(hostOrchestrator.runCommandOnHosts(anyList(), anySet(),
                eq("ls -d /hadoopfs/fs* /var /dbfs | xargs -I % rm -rf %/tmp/postgres_upgrade_backup")))
                .thenReturn(Map.of());
        when(hostOrchestrator.runCommandOnHosts(anyList(), anySet(),
                eq("df | grep /hadoopfs/fs | awk '{print $4\" \"$6}' | sort -nr | head -n 1 | awk '{print $2}'")))
                .thenReturn(Map.of("fqdn1", "/hadoopfs/fs1"));
        when(hostOrchestrator.runCommandOnHosts(anyList(), anySet(), eq("df -k /hadoopfs/fs1 | awk '{print $4}' | tail -n 1")))
                .thenReturn(Map.of("fqdn1", "1024000"));
        when(gatewayConfig.getHostname()).thenReturn("fqdn1");
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.readTree("{\"cmd_|-get_external_db_size_test\":{\"changes\":{\"stdout\":\"104857600\",\"stderr\":\"\"}}}");
        when(hostOrchestrator.applyOrchestratorState(any())).thenReturn(List.of(Map.of("fqdn1", jsonNode)));
        Map<String, SaltPillarProperties> pillarParams = Map.of("fqdn1", new SaltPillarProperties("path", Map.of()));
        when(upgradeRdsBackupRestoreStateParamsProvider.createParamsForRdsBackupRestore(stack, "/hadoopfs/fs1")).thenReturn(pillarParams);
        underTest.determineDbBackupLocation(STACK_ID);

        verify(hostOrchestrator).runCommandOnHosts(eq(List.of(gatewayConfig)), eq(Set.of("fqdn1")), eq("df -k /hadoopfs/fs1 | awk '{print $4}' | tail -n 1"));
        verify(hostOrchestrator).applyOrchestratorState(paramCaptor.capture());
        OrchestratorStateParams params = paramCaptor.getValue();
        assertThat(params.getState()).isEqualTo("postgresql/upgrade/external-db-size");
        assertThat(params.getTargetHostNames()).hasSameElementsAs(Set.of("fqdn1"));
        assertOtherStateParams(params);
        verify(hostOrchestrator).saveCustomPillars(saltConfigParamCaptor.capture(), any(), any());
        assertThat(saltConfigParamCaptor.getValue().getServicePillarConfig()).isEqualTo(pillarParams);
    }

    @Test
    void testValidateDbBackupSpaceNotEnoughSpace() throws CloudbreakOrchestratorException, JsonProcessingException {
        initGlobalPrivateFields();
        mockCreateStateParams();
        when(hostOrchestrator.runCommandOnHosts(anyList(), anySet(),
                eq("ls -d /hadoopfs/fs* /var /dbfs | xargs -I % rm -rf %/tmp/postgres_upgrade_backup")))
                .thenReturn(Map.of());
        when(hostOrchestrator.runCommandOnHosts(anyList(), anySet(),
                eq("df | grep /hadoopfs/fs | awk '{print $4\" \"$6}' | sort -nr | head -n 1 | awk '{print $2}'")))
                .thenReturn(Map.of("fqdn1", "/hadoopfs/fs1"));
        when(hostOrchestrator.runCommandOnHosts(anyList(), anySet(), eq("df -k /hadoopfs/fs1 | awk '{print $4}' | tail -n 1")))
                .thenReturn(Map.of("fqdn1", "1024000"));
        when(gatewayConfig.getHostname()).thenReturn("fqdn1");
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.readTree("{\"cmd_|-get_external_db_size_test\":{\"changes\":{\"stdout\":\"20971520000\",\"stderr\":\"\"}}}");
        when(hostOrchestrator.applyOrchestratorState(any())).thenReturn(List.of(Map.of("fqdn1", jsonNode)));

        CloudbreakOrchestratorException ex = assertThrows(CloudbreakOrchestratorException.class,
                () -> underTest.determineDbBackupLocation(STACK_ID));

        assertEquals("/hadoopfs/fs1 volume does not have enough free space (1000MB) for database backup (2000MB).", ex.getMessage());
        verify(hostOrchestrator).runCommandOnHosts(eq(List.of(gatewayConfig)), eq(Set.of("fqdn1")), eq("df -k /hadoopfs/fs1 | awk '{print $4}' | tail -n 1"));
        verify(hostOrchestrator).applyOrchestratorState(paramCaptor.capture());
        OrchestratorStateParams params = paramCaptor.getValue();
        assertThat(params.getState()).isEqualTo("postgresql/upgrade/external-db-size");
        assertThat(params.getTargetHostNames()).hasSameElementsAs(Set.of("fqdn1"));
    }

    @Test
    void testValidateDbBackupSpaceCouldNotGetRootVolumeFreeSpaceNoOutput() throws CloudbreakOrchestratorException {
        mockCreateStateParams();
        when(hostOrchestrator.runCommandOnHosts(anyList(), anySet(),
                eq("ls -d /hadoopfs/fs* /var /dbfs | xargs -I % rm -rf %/tmp/postgres_upgrade_backup")))
                .thenReturn(Map.of());
        when(hostOrchestrator.runCommandOnHosts(anyList(), anySet(),
                eq("df | grep /hadoopfs/fs | awk '{print $4\" \"$6}' | sort -nr | head -n 1 | awk '{print $2}'")))
                .thenReturn(Map.of("fqdn1", "/hadoopfs/fs1"));
        when(hostOrchestrator.runCommandOnHosts(anyList(), anySet(), eq("df -k /hadoopfs/fs1 | awk '{print $4}' | tail -n 1")))
                .thenReturn(Map.of("fqdn1", ""));
        when(gatewayConfig.getHostname()).thenReturn("fqdn1");

        CloudbreakOrchestratorException ex = assertThrows(CloudbreakOrchestratorException.class,
                () -> underTest.determineDbBackupLocation(STACK_ID));

        assertEquals("Could not get free space size on root volume from primary gateway", ex.getMessage());
        verify(hostOrchestrator).runCommandOnHosts(eq(List.of(gatewayConfig)), eq(Set.of("fqdn1")), eq("df -k /hadoopfs/fs1 | awk '{print $4}' | tail -n 1"));
    }

    @Test
    void testValidateDbBackupSpaceCheckingDbSizeNoResult() throws CloudbreakOrchestratorException {
        mockCreateStateParams();
        when(hostOrchestrator.runCommandOnHosts(anyList(), anySet(),
                eq("ls -d /hadoopfs/fs* /var /dbfs | xargs -I % rm -rf %/tmp/postgres_upgrade_backup")))
                .thenReturn(Map.of());
        when(hostOrchestrator.runCommandOnHosts(anyList(), anySet(),
                eq("df | grep /hadoopfs/fs | awk '{print $4\" \"$6}' | sort -nr | head -n 1 | awk '{print $2}'")))
                .thenReturn(Map.of("fqdn1", "/hadoopfs/fs1"));
        when(hostOrchestrator.runCommandOnHosts(anyList(), anySet(), eq("df -k /hadoopfs/fs1 | awk '{print $4}' | tail -n 1")))
                .thenReturn(Map.of("fqdn1", "1000"));
        when(gatewayConfig.getHostname()).thenReturn("fqdn1");
        when(hostOrchestrator.applyOrchestratorState(any())).thenReturn(List.of());

        CloudbreakOrchestratorException ex = assertThrows(CloudbreakOrchestratorException.class,
                () -> underTest.determineDbBackupLocation(STACK_ID));

        assertEquals("Orchestrator engine checking database size did not return any results", ex.getMessage());
        verify(hostOrchestrator).runCommandOnHosts(eq(List.of(gatewayConfig)), eq(Set.of("fqdn1")), eq("df -k /hadoopfs/fs1 | awk '{print $4}' | tail -n 1"));
        verify(hostOrchestrator).applyOrchestratorState(paramCaptor.capture());
        OrchestratorStateParams params = paramCaptor.getValue();
        assertThat(params.getState()).isEqualTo("postgresql/upgrade/external-db-size");
        assertThat(params.getTargetHostNames()).hasSameElementsAs(Set.of("fqdn1"));
        assertOtherStateParams(params);
    }

    @Test
    void testValidateDbBackupSpaceCheckingDbSizeScriptErrorOutput() throws CloudbreakOrchestratorException, JsonProcessingException {
        mockCreateStateParams();
        when(hostOrchestrator.runCommandOnHosts(anyList(), anySet(),
                eq("ls -d /hadoopfs/fs* /var /dbfs | xargs -I % rm -rf %/tmp/postgres_upgrade_backup")))
                .thenReturn(Map.of());
        when(hostOrchestrator.runCommandOnHosts(anyList(), anySet(),
                eq("df | grep /hadoopfs/fs | awk '{print $4\" \"$6}' | sort -nr | head -n 1 | awk '{print $2}'")))
                .thenReturn(Map.of("fqdn1", "/hadoopfs/fs1"));
        when(hostOrchestrator.runCommandOnHosts(anyList(), anySet(), eq("df -k /hadoopfs/fs1 | awk '{print $4}' | tail -n 1")))
                .thenReturn(Map.of("fqdn1", "1000"));
        when(gatewayConfig.getHostname()).thenReturn("fqdn1");
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.readTree("{\"cmd_|-get_external_db_size_test\":{\"changes\":{\"stderr\":\"error\"}}}");
        when(hostOrchestrator.applyOrchestratorState(any())).thenReturn(List.of(Map.of("fqdn1", jsonNode)));

        CloudbreakOrchestratorException ex = assertThrows(CloudbreakOrchestratorException.class,
                () -> underTest.determineDbBackupLocation(STACK_ID));

        assertEquals("Could not determine database size, because of the following error: error", ex.getMessage());
        verify(hostOrchestrator).runCommandOnHosts(eq(List.of(gatewayConfig)), eq(Set.of("fqdn1")), eq("df -k /hadoopfs/fs1 | awk '{print $4}' | tail -n 1"));
        verify(hostOrchestrator).applyOrchestratorState(paramCaptor.capture());
        OrchestratorStateParams params = paramCaptor.getValue();
        assertThat(params.getState()).isEqualTo("postgresql/upgrade/external-db-size");
        assertThat(params.getTargetHostNames()).hasSameElementsAs(Set.of("fqdn1"));
        assertOtherStateParams(params);
    }

    @Test
    void testValidateDbBackupSpaceCheckingDbSizeScriptNoOutput() throws CloudbreakOrchestratorException, JsonProcessingException {
        mockCreateStateParams();
        when(hostOrchestrator.runCommandOnHosts(anyList(), anySet(),
                eq("ls -d /hadoopfs/fs* /var /dbfs | xargs -I % rm -rf %/tmp/postgres_upgrade_backup")))
                .thenReturn(Map.of());
        when(hostOrchestrator.runCommandOnHosts(anyList(), anySet(),
                eq("df | grep /hadoopfs/fs | awk '{print $4\" \"$6}' | sort -nr | head -n 1 | awk '{print $2}'")))
                .thenReturn(Map.of("fqdn1", "/hadoopfs/fs1"));
        when(hostOrchestrator.runCommandOnHosts(anyList(), anySet(), eq("df -k /hadoopfs/fs1 | awk '{print $4}' | tail -n 1")))
                .thenReturn(Map.of("fqdn1", "1000"));
        when(gatewayConfig.getHostname()).thenReturn("fqdn1");
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.readTree("{\"cmd_|-get_external_db_size_test\":{\"changes\":{\"something\":\"else\"}}}");
        when(hostOrchestrator.applyOrchestratorState(any())).thenReturn(List.of(Map.of("fqdn1", jsonNode)));

        CloudbreakOrchestratorException ex = assertThrows(CloudbreakOrchestratorException.class,
                () -> underTest.determineDbBackupLocation(STACK_ID));

        assertEquals("Could not determine database size, because orchestration engine did not have return value", ex.getMessage());
        verify(hostOrchestrator).runCommandOnHosts(eq(List.of(gatewayConfig)), eq(Set.of("fqdn1")), eq("df -k /hadoopfs/fs1 | awk '{print $4}' | tail -n 1"));
        verify(hostOrchestrator).applyOrchestratorState(paramCaptor.capture());
        OrchestratorStateParams params = paramCaptor.getValue();
        assertThat(params.getState()).isEqualTo("postgresql/upgrade/external-db-size");
        assertThat(params.getTargetHostNames()).hasSameElementsAs(Set.of("fqdn1"));
        assertOtherStateParams(params);
    }

    @Test
    void testUpdateDatabaseEngineVersion() throws CloudbreakOrchestratorFailedException {
        underTest.updateDatabaseEngineVersion(STACK_ID, DATABASE_ENGINE_VERSION);

        verify(stackDtoService).getById(STACK_ID);
        verify(postgresConfigService).decorateServicePillarWithPostgresIfNeeded(anyMap(), eq(stack));
        verify(hostOrchestrator).saveCustomPillars(any(), any(), any());
    }

    @Test
    void testUpdateDatabaseEngineVersionShouldThrowExceptionWhenTheHostOrchestratorThrowsException() throws CloudbreakOrchestratorFailedException {
        doThrow(new CloudbreakOrchestratorFailedException("Error")).when(hostOrchestrator).saveCustomPillars(any(), any(), any());

        assertThrows(CloudbreakServiceException.class, () -> underTest.updateDatabaseEngineVersion(STACK_ID, DATABASE_ENGINE_VERSION));

        verify(stackDtoService).getById(STACK_ID);
        verify(postgresConfigService).decorateServicePillarWithPostgresIfNeeded(anyMap(), eq(stack));
        verify(hostOrchestrator).saveCustomPillars(any(), any(), any());
    }

    private void assertOtherStateParams(OrchestratorStateParams params) {
        assertThat(params.getPrimaryGatewayConfig()).isEqualTo(gatewayConfig);
        assertThat(params.getExitCriteriaModel()).isInstanceOf(ClusterDeletionBasedExitCriteriaModel.class);
        assertThat(((ClusterDeletionBasedExitCriteriaModel) params.getExitCriteriaModel()).getStackId().get()).isEqualTo(STACK_ID);
    }

    private void initGlobalPrivateFields() {
        Field backupValidationRatio = ReflectionUtils.findField(RdsUpgradeOrchestratorService.class, "backupValidationRatio");
        ReflectionUtils.makeAccessible(backupValidationRatio);
        ReflectionUtils.setField(backupValidationRatio, underTest, 0.1);
    }

    private void mockCreateStateParams() {
        when(saltStateParamsService.createStateParams(any(), any(), anyBoolean(), anyInt(), anyInt())).thenAnswer(new Answer<OrchestratorStateParams>() {
            @Override
            public OrchestratorStateParams answer(InvocationOnMock invocation) throws Throwable {
                OrchestratorStateParams orchestratorStateParams = new OrchestratorStateParams();
                orchestratorStateParams.setPrimaryGatewayConfig(gatewayConfig);
                orchestratorStateParams.setTargetHostNames(Set.of("fqdn1"));
                orchestratorStateParams.setState(invocation.getArgument(1, String.class));
                orchestratorStateParams.setExitCriteriaModel(new ClusterDeletionBasedExitCriteriaModel(stack.getId(), cluster.getId()));
                return orchestratorStateParams;
            }
        });
    }
}
