package com.sequenceiq.cloudbreak.core.flow2.cluster.rds.rotaterdscert;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.RotateRdsCertificateType.ROTATE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.cluster.api.ClusterModificationService;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.core.bootstrap.service.container.postgres.PostgresConfigService;
import com.sequenceiq.cloudbreak.core.flow2.cluster.rds.cert.check.DatahubCertificateChecker;
import com.sequenceiq.cloudbreak.core.flow2.cluster.rds.cert.rotate.RotateRdsCertificateService;
import com.sequenceiq.cloudbreak.core.flow2.externaldatabase.ExternalDatabaseService;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.dto.DatabaseSslDetails;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.rotaterdscert.RotateRdsCertificateFailedEvent;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.cluster.DatabaseSslService;
import com.sequenceiq.cloudbreak.service.salt.SaltStateParamsService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.view.StackView;

@ExtendWith(MockitoExtension.class)
class RotateRdsCertificateServiceTest {

    private static final Long STACK_ID = 123L;

    @Mock
    private StackUpdater stackUpdater;

    @Mock
    private CloudbreakFlowMessageService flowMessageService;

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private ClusterService clusterService;

    @Mock
    private StackService stackService;

    @Mock
    private ClusterApiConnectors clusterApiConnectors;

    @Mock
    private ExternalDatabaseService externalDatabaseService;

    @Mock
    private HostOrchestrator hostOrchestrator;

    @Mock
    private DatabaseSslService databaseSslService;

    @Mock
    private PostgresConfigService postgresConfigService;

    @Mock
    private SaltStateParamsService saltStateParamsService;

    @Mock
    private DatahubCertificateChecker datahubCertificateChecker;

    @InjectMocks
    private RotateRdsCertificateService underTest;

    @Test
    void testCheckPrerequisitesState() {
        underTest.checkPrerequisitesState(STACK_ID);
        verify(stackUpdater).updateStackStatus(eq(STACK_ID), any(DetailedStackStatus.class), anyString());
        verify(flowMessageService).fireEventAndLog(STACK_ID, UPDATE_IN_PROGRESS.name(), ResourceEvent.ROTATE_RDS_CERTIFICATE_CHECK_PREREQUISITES);
    }

    @Test
    void testGetLatestRdsCertificateState() {
        underTest.getLatestRdsCertificateState(STACK_ID);
        verify(stackUpdater).updateStackStatus(eq(STACK_ID), any(DetailedStackStatus.class), anyString());
        verify(flowMessageService).fireEventAndLog(STACK_ID, UPDATE_IN_PROGRESS.name(), ResourceEvent.ROTATE_RDS_CERTIFICATE_GET_LATEST);
    }

    @Test
    void testUpdateLatestRdsCertificateState() {
        underTest.updateLatestRdsCertificateState(STACK_ID);
        verify(stackUpdater).updateStackStatus(eq(STACK_ID), any(DetailedStackStatus.class), anyString());
        verify(flowMessageService).fireEventAndLog(STACK_ID, UPDATE_IN_PROGRESS.name(), ResourceEvent.ROTATE_RDS_CERTIFICATE_PUSH_LATEST);
    }

    @Test
    void testRestartCmState() {
        underTest.restartCmState(STACK_ID);
        verify(stackUpdater).updateStackStatus(eq(STACK_ID), any(DetailedStackStatus.class), anyString());
        verify(flowMessageService).fireEventAndLog(STACK_ID, UPDATE_IN_PROGRESS.name(), ResourceEvent.ROTATE_RDS_CERTIFICATE_CM_RESTART);
    }

    @Test
    void testRollingRestartRdsCertificateState() {
        underTest.rollingRestartRdsCertificateState(STACK_ID);
        verify(stackUpdater).updateStackStatus(eq(STACK_ID), any(DetailedStackStatus.class), anyString());
        verify(flowMessageService).fireEventAndLog(STACK_ID, UPDATE_IN_PROGRESS.name(), ResourceEvent.ROTATE_RDS_CERTIFICATE_ROLLING_SERVICE_RESTART);
    }

    @Test
    void testRotateOnProviderState() {
        underTest.rotateOnProviderState(STACK_ID);
        verify(stackUpdater).updateStackStatus(eq(STACK_ID), any(DetailedStackStatus.class), anyString());
        verify(flowMessageService).fireEventAndLog(STACK_ID, UPDATE_IN_PROGRESS.name(), ResourceEvent.ROTATE_RDS_CERTIFICATE_ON_PROVIDER);
    }

    @Test
    void testRotateRdsCertFinished() {
        underTest.rotateRdsCertFinished(STACK_ID);
        verify(stackUpdater).updateStackStatus(eq(STACK_ID), any(DetailedStackStatus.class), anyString());
        verify(flowMessageService).fireEventAndLog(STACK_ID, AVAILABLE.name(), ResourceEvent.ROTATE_RDS_CERTIFICATE_FINISHED);
    }

    @Test
    void testRotateRdsCertFailed() {
        RotateRdsCertificateFailedEvent failedEvent = new RotateRdsCertificateFailedEvent(STACK_ID, ROTATE, new RuntimeException("error"));
        underTest.rotateRdsCertFailed(failedEvent);
        verify(stackUpdater).updateStackStatus(eq(STACK_ID), eq(DetailedStackStatus.ROTATE_RDS_CERTIFICATE_FAILED), anyString());
        verify(flowMessageService).fireEventAndLog(STACK_ID, UPDATE_FAILED.name(), ResourceEvent.ROTATE_RDS_CERTIFICATE_FAILED);
    }

    @Test
    public void testCheckPrerequisites() {
        Long stackId = 1L;
        StackView stackView = mock(StackView.class);
        Cluster cluster = mock(Cluster.class);

        when(stackView.getClusterId()).thenReturn(1L);
        when(stackDtoService.getStackViewById(stackId)).thenReturn(stackView);
        when(clusterService.getCluster(1L)).thenReturn(cluster);
        when(cluster.getDbSslRootCertBundle()).thenReturn("some-cert-bundle");
        when(cluster.getDbSslEnabled()).thenReturn(true);

        underTest.checkPrerequisites(stackId, ROTATE);

        verify(stackDtoService).getStackViewById(stackId);
        verify(clusterService).getCluster(1L);
    }

    @Test
    public void testCheckPrerequisitesThrowsExceptionIfClusterNull() {
        Long stackId = 1L;
        StackView stackView = mock(StackView.class);

        when(stackDtoService.getStackViewById(stackId)).thenReturn(stackView);
        when(clusterService.getCluster(any())).thenReturn(null);

        CloudbreakServiceException exception = assertThrows(CloudbreakServiceException.class, () -> {
            underTest.checkPrerequisites(stackId, ROTATE);
        });

        assertEquals("Data Hub Database not ssl enabled. Rotation of certificate does not supported", exception.getMessage());
    }

    @Test
    public void testCheckPrerequisitesThrowsExceptionIfClusterBundleNull() {
        Long stackId = 1L;
        StackView stackView = mock(StackView.class);
        Cluster cluster = mock(Cluster.class);

        when(cluster.getDbSslRootCertBundle()).thenReturn(null);
        when(stackDtoService.getStackViewById(stackId)).thenReturn(stackView);
        when(clusterService.getCluster(any())).thenReturn(cluster);

        CloudbreakServiceException exception = assertThrows(CloudbreakServiceException.class, () -> {
            underTest.checkPrerequisites(stackId, ROTATE);
        });

        assertEquals("Data Hub Database not ssl enabled. Rotation of certificate does not supported", exception.getMessage());
    }

    @Test
    public void testCheckPrerequisitesThrowsExceptionIfDbSSlNotEnabled() {
        Long stackId = 1L;
        StackView stackView = mock(StackView.class);
        Cluster cluster = mock(Cluster.class);

        when(cluster.getDbSslRootCertBundle()).thenReturn("bundle");
        when(cluster.getDbSslEnabled()).thenReturn(false);
        when(stackDtoService.getStackViewById(stackId)).thenReturn(stackView);
        when(clusterService.getCluster(any())).thenReturn(cluster);

        CloudbreakServiceException exception = assertThrows(CloudbreakServiceException.class, () -> {
            underTest.checkPrerequisites(stackId, ROTATE);
        });

        assertEquals("Data Hub Database not ssl enabled. Rotation of certificate does not supported", exception.getMessage());
    }

    @Test
    public void testCheckPrerequisitesIfEverythingWorks() {
        Long stackId = 1L;
        StackView stackView = mock(StackView.class);
        Cluster cluster = mock(Cluster.class);

        when(cluster.getDbSslRootCertBundle()).thenReturn("bundle");
        when(cluster.getDbSslEnabled()).thenReturn(true);
        when(stackDtoService.getStackViewById(stackId)).thenReturn(stackView);
        when(clusterService.getCluster(any())).thenReturn(cluster);
        when(datahubCertificateChecker.collectDatahubsWhichMustBeUpdated(any())).thenReturn(List.of());

        underTest.checkPrerequisites(stackId, ROTATE);
    }

    @Test
    public void testGetLatestRdsCertificate() {
        Long stackId = 1L;
        StackDto stackDto = mock(StackDto.class);
        Cluster cluster = mock(Cluster.class);

        when(stackDto.getDatalakeCrn()).thenReturn("crn:cdp:datalake:us-west-1:" +
                "9d74eee4-1cad-45d7-b645-7ccf9edbb73d:datalake:b0d5d4dd-5be8-4914-9be8-77040bb177ef");
        when(cluster.hasExternalDatabase()).thenReturn(true);
        when(stackDto.getCluster()).thenReturn(cluster);
        when(stackDtoService.getById(stackId)).thenReturn(stackDto);
        when(stackDtoService.getByCrn(any())).thenReturn(stackDto);
        doNothing().when(externalDatabaseService).updateToLatestSslCert(any());

        when(databaseSslService.getDbSslDetailsForRotationAndUpdateInCluster(any(StackDto.class))).thenReturn(new DatabaseSslDetails(Set.of(), false));

        underTest.getLatestRdsCertificate(stackId);

        verify(externalDatabaseService, times(2)).updateToLatestSslCert(any());
        verify(clusterService, times(2)).getCluster(any());
    }

    @Test
    public void testUpdateLatestRdsCertificate() throws CloudbreakOrchestratorFailedException {
        Long stackId = 1L;
        StackDto stackDto = mock(StackDto.class);
        Cluster cluster = mock(Cluster.class);

        when(stackDtoService.getById(stackId)).thenReturn(stackDto);
        when(stackDto.getCluster()).thenReturn(cluster);
        when(stackDtoService.getById(stackId)).thenReturn(stackDto);

        underTest.updateLatestRdsCertificate(stackId);

        verify(postgresConfigService).uploadServicePillarsForPostgres(any(), any(), any());
        verify(hostOrchestrator).runOrchestratorState(any());
    }

    @Test
    public void testUpdateLatestRdsCertificateShouldThrowOrchestrationExceptionWhenOrchestrationFails() throws CloudbreakOrchestratorFailedException {
        Long stackId = 1L;
        StackDto stackDto = mock(StackDto.class);
        Cluster cluster = mock(Cluster.class);

        when(stackDto.getType()).thenReturn(StackType.WORKLOAD);
        when(stackDto.getName()).thenReturn("uh-oh");
        when(stackDtoService.getById(stackId)).thenReturn(stackDto);
        when(stackDto.getCluster()).thenReturn(cluster);
        when(stackDtoService.getById(stackId)).thenReturn(stackDto);
        doThrow(new CloudbreakOrchestratorFailedException("uh-oh something really bad happened")).when(hostOrchestrator).runOrchestratorState(any());

        CloudbreakServiceException exception = assertThrows(CloudbreakServiceException.class,
                () -> underTest.updateLatestRdsCertificate(stackId));

        assertEquals("Distribution of database SSL certificates failed on Data Hub with name: 'uh-oh'", exception.getMessage());
        verify(postgresConfigService).uploadServicePillarsForPostgres(any(), any(), any());
        verify(hostOrchestrator).runOrchestratorState(any());
    }

    @Test
    public void testRollingRestartServices() {
        Long stackId = 1L;
        Stack stack = mock(Stack.class);
        ClusterApi clusterApiConnector = mock(ClusterApi.class);
        ClusterModificationService clusterModificationService = mock(ClusterModificationService.class);
        doNothing().when(clusterModificationService).rollingRestartServices(false);
        when(clusterApiConnector.clusterModificationService()).thenReturn(clusterModificationService);
        when(stackService.getByIdWithLists(stackId)).thenReturn(stack);
        when(clusterApiConnectors.getConnector(stack)).thenReturn(clusterApiConnector);

        underTest.rollingRestartServices(stackId);

        verify(stackService).getByIdWithLists(stackId);
        verify(clusterApiConnectors).getConnector(stack);
        verify(clusterModificationService).rollingRestartServices(false);
    }

    @Test
    public void testRotateOnProvider() {
        Long stackId = 1L;
        StackDto stackDto = mock(StackDto.class);
        Cluster cluster = mock(Cluster.class);

        when(stackDtoService.getById(any())).thenReturn(stackDto);
        when(stackDto.getCluster()).thenReturn(cluster);
        when(cluster.hasExternalDatabase()).thenReturn(true);
        when(clusterService.getCluster(any())).thenReturn(cluster);

        underTest.rotateOnProvider(stackId);

        verify(externalDatabaseService).rotateSSLCertificate(any());
    }

}
