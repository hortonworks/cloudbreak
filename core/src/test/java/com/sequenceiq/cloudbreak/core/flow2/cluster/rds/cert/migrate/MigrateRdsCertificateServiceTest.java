package com.sequenceiq.cloudbreak.core.flow2.cluster.rds.cert.migrate;

import static com.sequenceiq.cloudbreak.cloud.model.ExternalDatabaseStatus.UPDATE_IN_PROGRESS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.common.collect.Table;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.cluster.model.CMConfigUpdateStrategy;
import com.sequenceiq.cloudbreak.cmtemplate.CentralCmTemplateUpdater;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateComponentConfigProviderProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.converter.StackToTemplatePreparationObjectConverter;
import com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterDeletionBasedExitCriteriaModel;
import com.sequenceiq.cloudbreak.core.bootstrap.service.container.postgres.PostgresConfigService;
import com.sequenceiq.cloudbreak.core.flow2.cluster.rds.upgrade.RdsSettingsMigrationService;
import com.sequenceiq.cloudbreak.core.flow2.externaldatabase.ExternalDatabaseService;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.host.OrchestratorStateParams;
import com.sequenceiq.cloudbreak.rotation.ExitCriteriaProvider;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.cluster.DatabaseSslService;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;
import com.sequenceiq.cloudbreak.service.salt.SaltStateParamsService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.view.ClusterView;

@ExtendWith(MockitoExtension.class)
public class MigrateRdsCertificateServiceTest {

    @InjectMocks
    private MigrateRdsCertificateService migrateRdsCertificateService;

    @Mock
    private StackUpdater stackUpdater;

    @Mock
    private CloudbreakFlowMessageService flowMessageService;

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private ClusterService clusterService;

    @Mock
    private ExternalDatabaseService externalDatabaseService;

    @Mock
    private DatabaseSslService databaseSslService;

    @Mock
    private CmTemplateComponentConfigProviderProcessor cmTemplateComponentConfigProviderProcessor;

    @Mock
    private CentralCmTemplateUpdater centralCmTemplateUpdater;

    @Mock
    private StackToTemplatePreparationObjectConverter stackToTemplatePreparationObjectConverter;

    @Mock
    private RdsSettingsMigrationService rdsSettingsMigrationService;

    @Mock
    private RdsConfigService rdsConfigService;

    @Mock
    private SaltStateParamsService saltStateParamsService;

    @Mock
    private ExitCriteriaProvider exitCriteriaProvider;

    @Mock
    private GatewayConfigService gatewayConfigService;

    @Mock
    private PostgresConfigService postgresConfigService;

    @Mock
    private HostOrchestrator hostOrchestrator;

    @Test
    public void testSetupNonTlsToTlsIfRequired() {
        Long stackId = 1L;

        migrateRdsCertificateService.setupNonTlsToTlsIfRequired(stackId);

        verify(stackUpdater).updateStackStatus(stackId, DetailedStackStatus.ROTATE_RDS_CERTIFICATE_IN_PROGRESS,
                "Setup TLS on cluster if required");
        verify(flowMessageService).fireEventAndLog(stackId, UPDATE_IN_PROGRESS.name(), ResourceEvent.ROTATE_RDS_CERTIFICATE_TLS_SETUP);
    }

    @Test
    public void testUpdateNonTlsToTlsIfRequired() throws Exception {
        Long stackId = 1L;
        StackDto stackDto = mock(StackDto.class);
        when(stackDtoService.getById(stackId)).thenReturn(stackDto);

        TemplatePreparationObject templatePreparationObject = mock(TemplatePreparationObject.class);
        when(stackToTemplatePreparationObjectConverter.convert(stackDto)).thenReturn(templatePreparationObject);

        CmTemplateProcessor cmTemplateProcessor = mock(CmTemplateProcessor.class);
        when(centralCmTemplateUpdater.getCmTemplateProcessor(templatePreparationObject)).thenReturn(cmTemplateProcessor);

        when(cmTemplateComponentConfigProviderProcessor.collectDataConfigurations(cmTemplateProcessor, templatePreparationObject))
                .thenReturn(mock(Table.class));

        migrateRdsCertificateService.updateNonTlsToTlsIfRequired(stackId);

        verify(rdsSettingsMigrationService).updateCMServiceConfigs(
                any(StackDtoDelegate.class),
                any(Table.class),
                any(CMConfigUpdateStrategy.class),
                anyBoolean());
    }

    @Test
    public void testTurnOnSslOnProvider() {
        Long stackId = 1L;
        StackDto stackDto = mock(StackDto.class);
        when(stackDtoService.getById(stackId)).thenReturn(stackDto);
        when(stackDto.getCluster()).thenReturn(mock(ClusterView.class));

        Cluster cluster = mock(Cluster.class);
        when(clusterService.getCluster(anyLong())).thenReturn(cluster);

        when(stackDto.getCluster().hasExternalDatabase()).thenReturn(true);

        migrateRdsCertificateService.turnOnSslOnProvider(stackId);

        verify(externalDatabaseService).turnOnSslOnProvider(cluster);
    }

    @Test
    public void testMigrateRdsToTls() {
        Long stackId = 1L;
        StackDto stackDto = mock(StackDto.class);
        when(stackDtoService.getById(stackId)).thenReturn(stackDto);
        when(stackDto.getCluster()).thenReturn(mock(ClusterView.class));

        Cluster cluster = mock(Cluster.class);
        when(clusterService.getCluster(anyLong())).thenReturn(cluster);

        when(stackDto.getCluster().hasExternalDatabase()).thenReturn(true);

        migrateRdsCertificateService.migrateRdsToTls(stackId);

        verify(externalDatabaseService).migrateRdsToTls(cluster);
    }

    @Test
    public void testMigrateStackToTlsWithExternalDatabase() {
        Long stackId = 1L;
        StackDto stackDto = mock(StackDto.class);
        when(stackDtoService.getById(stackId)).thenReturn(stackDto);
        Cluster cluster = mock(Cluster.class);
        when(clusterService.getCluster(anyLong())).thenReturn(cluster);

        when(stackDto.getCluster()).thenReturn(mock(ClusterView.class));
        when(stackDto.getCluster().hasExternalDatabase()).thenReturn(true);

        migrateRdsCertificateService.migrateStackToTls(stackId);

        verify(clusterService).enableSsl(cluster.getId());
        verify(databaseSslService).getDbSslDetailsForCreationAndUpdateInCluster(stackDto);
    }

    public void testEnableSslOnClusterSide() throws CloudbreakOrchestratorFailedException {
        Long stackId = 1L;
        StackDto stackDto = mock(StackDto.class);
        when(stackDtoService.getById(stackId)).thenReturn(stackDto);

        OrchestratorStateParams stateParams = mock(OrchestratorStateParams.class);
        when(saltStateParamsService.createStateParamsForReachableNodes(stackDto, "postgresql.enable_ssl", 100, 3)).thenReturn(stateParams);

        migrateRdsCertificateService.enableSslOnClusterSide(stackId);

        verify(postgresConfigService).uploadServicePillarsForPostgres(eq(stackDto), any(ClusterDeletionBasedExitCriteriaModel.class), eq(stateParams));
        verify(hostOrchestrator).runOrchestratorState(stateParams);
    }
}