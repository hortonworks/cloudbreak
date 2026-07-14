package com.sequenceiq.cloudbreak.core.flow2.cluster.rds.upgrade;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

import jakarta.inject.Inject;
import jakarta.ws.rs.client.Client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.sequenceiq.authorization.service.OwnerAssignmentService;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.security.CrnUserDetailsService;
import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;
import com.sequenceiq.cloudbreak.common.metrics.CommonMetricService;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.common.service.TransactionMetricsService;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.converter.TargetMajorVersionToUpgradeTargetVersionConverter;
import com.sequenceiq.cloudbreak.core.flow2.CloudbreakFlowInformation;
import com.sequenceiq.cloudbreak.core.flow2.StackStatusFinalizer;
import com.sequenceiq.cloudbreak.core.flow2.cluster.rds.cert.rotate.RotateRdsCertificateService;
import com.sequenceiq.cloudbreak.core.flow2.externaldatabase.ExternalDatabaseService;
import com.sequenceiq.cloudbreak.core.flow2.service.CbEventParameterFactory;
import com.sequenceiq.cloudbreak.core.flow2.service.ReactorNotifier;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.domain.stack.Database;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.ha.NodeConfig;
import com.sequenceiq.cloudbreak.ha.service.NodeValidator;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.quartz.configuration.scheduler.TransactionalScheduler;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.UpgradeRdsTriggerRequest;
import com.sequenceiq.cloudbreak.reactor.handler.cluster.upgrade.rds.BackupRdsDataHandler;
import com.sequenceiq.cloudbreak.reactor.handler.cluster.upgrade.rds.GetLatestRdsCertsHandler;
import com.sequenceiq.cloudbreak.reactor.handler.cluster.upgrade.rds.InstallPostgresPackagesHandler;
import com.sequenceiq.cloudbreak.reactor.handler.cluster.upgrade.rds.MigrateAttachedDatahubsDBSettingsHandler;
import com.sequenceiq.cloudbreak.reactor.handler.cluster.upgrade.rds.MigrateDatabaseSettingsHandler;
import com.sequenceiq.cloudbreak.reactor.handler.cluster.upgrade.rds.MigrateServicesDBSettingsHandler;
import com.sequenceiq.cloudbreak.reactor.handler.cluster.upgrade.rds.RestoreRdsDataHandler;
import com.sequenceiq.cloudbreak.reactor.handler.cluster.upgrade.rds.StartClusterManagerHandler;
import com.sequenceiq.cloudbreak.reactor.handler.cluster.upgrade.rds.StartServicesHandler;
import com.sequenceiq.cloudbreak.reactor.handler.cluster.upgrade.rds.StopServicesHandler;
import com.sequenceiq.cloudbreak.reactor.handler.cluster.upgrade.rds.UpdateLatestRdsCertsHandler;
import com.sequenceiq.cloudbreak.reactor.handler.cluster.upgrade.rds.UpdatePostgresVersionHandler;
import com.sequenceiq.cloudbreak.reactor.handler.cluster.upgrade.rds.UpgradeRdsHandler;
import com.sequenceiq.cloudbreak.reactor.handler.cluster.upgrade.rds.WaitForRdsUpgradeHandler;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.cluster.EmbeddedDatabaseService;
import com.sequenceiq.cloudbreak.service.database.DatabaseService;
import com.sequenceiq.cloudbreak.service.metrics.CloudbreakMetricService;
import com.sequenceiq.cloudbreak.service.secret.service.SecretService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.upgrade.rds.RdsUpgradeOrchestratorService;
import com.sequenceiq.cloudbreak.util.StackUtil;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.core.FlowEventListener;
import com.sequenceiq.flow.core.FlowRegister;
import com.sequenceiq.flow.core.edh.FlowUsageSender;
import com.sequenceiq.flow.core.listener.FlowEventCommonListener;
import com.sequenceiq.flow.core.stats.FlowOperationStatisticsPersister;
import com.sequenceiq.flow.core.stats.FlowOperationStatisticsService;
import com.sequenceiq.flow.repository.FlowChainLogRepository;
import com.sequenceiq.flow.repository.FlowLogRepository;
import com.sequenceiq.flow.service.FlowCancelService;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.FreeIpaV1Endpoint;
import com.sequenceiq.notification.WebSocketNotificationService;

import io.micrometer.core.instrument.MeterRegistry;

@ActiveProfiles("integration-test")
@ExtendWith(SpringExtension.class)
class UpgradeRdsFlowIntegrationTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:" + UUID.randomUUID() + ":user:" + UUID.randomUUID();

    private static final long STACK_ID = 1L;

    private static final long CLUSTER_ID = 1234L;

    private static final TargetMajorVersion TARGET_MAJOR_VERSION = TargetMajorVersion.VERSION_11;

    @Inject
    private ReactorNotifier reactorNotifier;

    @Inject
    private FlowRegister flowRegister;

    @MockBean
    private StackDtoService stackDtoService;

    @MockBean
    private UpgradeRdsService upgradeRdsService;

    @MockBean
    private RotateRdsCertificateService rotateRdsCertificateService;

    @MockBean
    private ExternalDatabaseService externalDatabaseService;

    @MockBean
    private EmbeddedDatabaseService embeddedDatabaseService;

    @MockBean
    private DatabaseService databaseService;

    @MockBean
    private CrnUserDetailsService crnUserDetailsService;

    @MockBean
    private NodeConfig nodeConfig;

    @MockBean
    private NodeValidator nodeValidator;

    @MockBean
    private FlowCancelService flowCancelService;

    @BeforeEach
    public void setup() {
        StackView stackView = mock(StackView.class);
        when(stackView.getId()).thenReturn(STACK_ID);
        when(stackView.isAvailable()).thenReturn(true);
        when(stackView.getClusterId()).thenReturn(CLUSTER_ID);

        ClusterView clusterView = mock(ClusterView.class);
        when(clusterView.getId()).thenReturn(CLUSTER_ID);
        when(clusterView.getDbSslEnabled()).thenReturn(true);
        when(clusterView.getDatabaseServerCrn()).thenReturn(null);

        StackDto stackDto = mock(StackDto.class);
        when(stackDto.getStack()).thenReturn(stackView);
        when(stackDto.getCluster()).thenReturn(clusterView);
        when(stackDto.getDatabase()).thenReturn(new Database());
        when(stackDto.getName()).thenReturn("stackname");
        when(stackDto.hasGateway()).thenReturn(false);

        when(stackDtoService.getStackViewById(STACK_ID)).thenReturn(stackView);
        when(stackDtoService.getClusterViewByStackId(STACK_ID)).thenReturn(clusterView);
        when(stackDtoService.getById(STACK_ID)).thenReturn(stackDto);
        when(databaseService.findById(any())).thenReturn(Optional.empty());

        // Embedded, SSL-enabled DB: no provider flow id (WAIT is skipped), refresh state must still run.
        when(embeddedDatabaseService.isAttachedDiskForEmbeddedDatabaseCreated(any())).thenReturn(true);

        doNothing().when(nodeValidator).checkForRecentHeartbeat();
    }

    @Test
    public void testUpgradeRdsRefreshesCertBundleAndFinishes() {
        FlowIdentifier flowIdentifier = triggerFlow();
        letItFlow(flowIdentifier);

        // Refresh state runs between the DB server upgrade and Cluster Manager restart.
        InOrder inOrder = inOrder(upgradeRdsService, rotateRdsCertificateService);
        inOrder.verify(upgradeRdsService).upgradeRdsState(STACK_ID);
        inOrder.verify(upgradeRdsService).getLatestCertsState(STACK_ID);
        inOrder.verify(rotateRdsCertificateService).getLatestRdsCertificate(STACK_ID);
        inOrder.verify(upgradeRdsService).updateLatestCertsState(STACK_ID);
        inOrder.verify(rotateRdsCertificateService).updateLatestRdsCertificate(STACK_ID);
        inOrder.verify(upgradeRdsService).startClusterManagerState(STACK_ID);
        inOrder.verify(upgradeRdsService).rdsUpgradeFinished(STACK_ID, CLUSTER_ID);

        verify(upgradeRdsService, never()).rdsUpgradeFailed(anyLong(), any(), any());
        assertTrue(flowRegister.getRunningFlowIds().isEmpty(), "flow has not finished");
    }

    private FlowIdentifier triggerFlow() {
        String selector = UpgradeRdsEvent.UPGRADE_RDS_EVENT.event();
        return ThreadBasedUserCrnProvider.doAs(
                USER_CRN,
                () -> reactorNotifier.notify(STACK_ID, selector,
                        new UpgradeRdsTriggerRequest(selector, STACK_ID, TARGET_MAJOR_VERSION, null, null)));
    }

    private void letItFlow(FlowIdentifier flowIdentifier) {
        int i = 0;
        do {
            i++;
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
            }
        } while (flowRegister.get(flowIdentifier.getPollableId()) != null && i < 20);
    }

    @Profile("integration-test")
    @TestConfiguration
    @Import({
            TransactionService.class,
            CommonMetricService.class,
            TransactionMetricsService.class,
            CloudbreakMetricService.class,
            Clock.class,
            CbEventParameterFactory.class,
            ReactorNotifier.class,
            UpgradeRdsActions.class,
            UpgradeRdsFlowConfig.class,
            RdsUpgradeFlowTriggerCondition.class,
            StopServicesHandler.class,
            BackupRdsDataHandler.class,
            UpgradeRdsHandler.class,
            WaitForRdsUpgradeHandler.class,
            GetLatestRdsCertsHandler.class,
            UpdateLatestRdsCertsHandler.class,
            MigrateDatabaseSettingsHandler.class,
            RestoreRdsDataHandler.class,
            StartClusterManagerHandler.class,
            MigrateServicesDBSettingsHandler.class,
            StartServicesHandler.class,
            InstallPostgresPackagesHandler.class,
            MigrateAttachedDatahubsDBSettingsHandler.class,
            UpdatePostgresVersionHandler.class
    })
    @ComponentScan(basePackages = {
            "com.sequenceiq.flow",
    })
    static class Config {

        @MockBean
        private FlowLogRepository flowLogRepository;

        @MockBean
        private FlowChainLogRepository flowChainLogRepository;

        @MockBean
        private OwnerAssignmentService ownerAssignmentService;

        @MockBean
        private WebSocketNotificationService webSocketNotificationService;

        @MockBean
        private Client client;

        @MockBean
        private SecretService secretService;

        @MockBean
        private FreeIpaV1Endpoint freeIpaV1Endpoint;

        @MockBean
        private TransactionalScheduler scheduler;

        @MockBean
        private FlowOperationStatisticsService flowOperationStatisticsService;

        @MockBean
        private StackStatusFinalizer stackStatusFinalizer;

        @MockBean
        private CloudbreakFlowMessageService flowMessageService;

        @MockBean
        private StackService stackService;

        @MockBean
        private StackUpdater stackUpdater;

        @MockBean
        private RdsUpgradeOrchestratorService rdsUpgradeOrchestratorService;

        @MockBean
        private TargetMajorVersionToUpgradeTargetVersionConverter targetMajorVersionToUpgradeTargetVersionConverter;

        @MockBean
        private GatewayConfigService gatewayConfigService;

        @MockBean
        private HostOrchestrator hostOrchestrator;

        @MockBean
        private StackUtil stackUtil;

        @MockBean
        private ClusterApiConnectors clusterApiConnectors;

        @MockBean
        private RdsSettingsMigrationService rdsSettingsMigrationService;

        @MockBean
        private AttachedDatahubsRdsSettingsMigrationService attachedDatahubsRdsSettingsMigrationService;

        @MockBean
        private CloudbreakFlowInformation cloudbreakFlowInformation;

        @MockBean
        private FlowOperationStatisticsPersister flowOperationStatisticsPersister;

        @MockBean
        private MeterRegistry meterRegistry;

        @MockBean
        private FlowEventCommonListener flowEventCommonListener;

        @MockBean
        private FlowEventListener flowEventListener;

        @MockBean
        private FlowUsageSender flowUsageSender;

        @Bean
        public EventBus reactor(ExecutorService threadPoolExecutor) {
            return EventBus.builder()
                    .executor(threadPoolExecutor)
                    .exceptionHandler((exception, context) -> {
                    })
                    .unhandledEventHandler(event -> {
                    })
                    .build();
        }
    }
}
