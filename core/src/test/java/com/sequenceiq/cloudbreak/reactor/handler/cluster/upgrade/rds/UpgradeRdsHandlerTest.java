package com.sequenceiq.cloudbreak.reactor.handler.cluster.upgrade.rds;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.converter.TargetMajorVersionToUpgradeTargetVersionConverter;
import com.sequenceiq.cloudbreak.core.flow2.externaldatabase.ExternalDatabaseService;
import com.sequenceiq.cloudbreak.domain.stack.Database;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.UpgradeRdsFailedEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.UpgradeRdsUpgradeDatabaseServerRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.UpgradeRdsUpgradeDatabaseServerResult;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.cluster.EmbeddedDatabaseService;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.upgrade.rds.RdsUpgradeOrchestratorService;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowType;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.UpgradeTargetMajorVersion;
import com.sequenceiq.redbeams.api.endpoint.v4.stacks.DatabaseServerV4StackRequest;

@ExtendWith(MockitoExtension.class)
class UpgradeRdsHandlerTest {

    private static final Long STACK_ID = 12L;

    private static final TargetMajorVersion TARGET_MAJOR_VERSION = TargetMajorVersion.VERSION_11;

    private static final UpgradeTargetMajorVersion UPGRADE_TARGET_MAJOR_VERSION = UpgradeTargetMajorVersion.VERSION_11;

    private static final String DB_CRN = "dbCrn";

    @Mock
    private ExternalDatabaseService databaseService;

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private RdsUpgradeOrchestratorService rdsUpgradeOrchestratorService;

    @Mock
    private EmbeddedDatabaseService embeddedDatabaseService;

    @Mock
    private TargetMajorVersionToUpgradeTargetVersionConverter targetMajorVersionToUpgradeTargetVersionConverter;

    @Mock
    private EnvironmentService environmentClientService;

    @Mock
    private StackUpdater stackUpdater;

    @Mock
    private HandlerEvent<UpgradeRdsUpgradeDatabaseServerRequest> event;

    @Mock
    private Stack stack;

    @InjectMocks
    private UpgradeRdsHandler underTest;

    @Test
    void selector() {
        assertThat(underTest.selector()).isEqualTo("UPGRADERDSUPGRADEDATABASESERVERREQUEST");
    }

    @Test
    void testDefaultFailureEvent() {
        UpgradeRdsUpgradeDatabaseServerRequest request = new UpgradeRdsUpgradeDatabaseServerRequest(STACK_ID, TARGET_MAJOR_VERSION);

        Selectable defaultFailureEvent = underTest.defaultFailureEvent(1L, new RuntimeException(), Event.wrap(request));

        assertEquals("UPGRADERDSFAILEDEVENT", defaultFailureEvent.selector());
    }

    @Test
    void doAcceptForExternal() throws CloudbreakOrchestratorException {
        UpgradeRdsUpgradeDatabaseServerRequest request = new UpgradeRdsUpgradeDatabaseServerRequest(STACK_ID, TARGET_MAJOR_VERSION);
        StackDto stackDto = spy(StackDto.class);
        ClusterView cluster = mock(ClusterView.class);
        when(stackDto.getCluster()).thenReturn(cluster);
        when(stackDtoService.getById(STACK_ID)).thenReturn(stackDto);
        when(stackDto.getCluster()).thenReturn(cluster);
        when(cluster.getDatabaseServerCrn()).thenReturn(DB_CRN);
        when(event.getData()).thenReturn(request);
        when(targetMajorVersionToUpgradeTargetVersionConverter.convert(TARGET_MAJOR_VERSION)).thenReturn(UPGRADE_TARGET_MAJOR_VERSION);

        DatabaseServerV4StackRequest newSettings = new DatabaseServerV4StackRequest();
        when(databaseService.migrateDatabaseSettingsIfNeeded(stackDto, TARGET_MAJOR_VERSION)).thenReturn(newSettings);
        FlowIdentifier flowIdentifier = new FlowIdentifier(FlowType.FLOW, "flowid");
        when(databaseService.upgradeDatabase(cluster, UpgradeTargetMajorVersion.VERSION_11, newSettings)).thenReturn(flowIdentifier);
        Selectable result = underTest.doAccept(event);

        verify(targetMajorVersionToUpgradeTargetVersionConverter).convert(TARGET_MAJOR_VERSION);
        verify(databaseService).upgradeDatabase(cluster, UpgradeTargetMajorVersion.VERSION_11, newSettings);
        verify(rdsUpgradeOrchestratorService, never()).upgradeEmbeddedDatabase(stackDto);
        verify(embeddedDatabaseService, never()).isAttachedDiskForEmbeddedDatabaseCreated(stackDto);
        assertThat(result.selector()).isEqualTo("UPGRADERDSUPGRADEDATABASESERVERRESULT");
        assertThat(((UpgradeRdsUpgradeDatabaseServerResult) result).getFlowIdentifier()).isEqualTo(flowIdentifier);
    }

    @Test
    void doAcceptForEmbedded() throws CloudbreakOrchestratorException {
        UpgradeRdsUpgradeDatabaseServerRequest request = new UpgradeRdsUpgradeDatabaseServerRequest(STACK_ID, TARGET_MAJOR_VERSION);
        StackDto stackDto = spy(StackDto.class);
        ClusterView cluster = mock(ClusterView.class);
        Database database = new Database();

        when(embeddedDatabaseService.isAttachedDiskForEmbeddedDatabaseCreated(stackDto)).thenReturn(true);
        when(stackDtoService.getById(STACK_ID)).thenReturn(stackDto);
        when(stackDto.getCluster()).thenReturn(cluster);
        when(stackDto.getDatabase()).thenReturn(database);
        when(cluster.getDatabaseServerCrn()).thenReturn(null);
        when(event.getData()).thenReturn(request);

        Selectable result = underTest.doAccept(event);

        verify(targetMajorVersionToUpgradeTargetVersionConverter, never()).convert(TARGET_MAJOR_VERSION);
        verify(databaseService, never()).upgradeDatabase(any(ClusterView.class), any(UpgradeTargetMajorVersion.class), any(DatabaseServerV4StackRequest.class));
        verify(databaseService, never()).migrateDatabaseSettingsIfNeeded(any(StackDto.class), any(TargetMajorVersion.class));
        verify(environmentClientService, never()).getByCrn(any());
        verify(rdsUpgradeOrchestratorService).upgradeEmbeddedDatabase(stackDto);
        verify(stackUpdater).updateExternalDatabaseEngineVersion(STACK_ID, TARGET_MAJOR_VERSION.getMajorVersion());
        verify(rdsUpgradeOrchestratorService).updateDatabaseEngineVersion(stackDto);
        assertThat(result.selector()).isEqualTo("UPGRADERDSUPGRADEDATABASESERVERRESULT");
    }

    @Test
    void doAcceptForEmbeddedOnRootShouldThrowException() throws CloudbreakOrchestratorException {
        UpgradeRdsUpgradeDatabaseServerRequest request = new UpgradeRdsUpgradeDatabaseServerRequest(STACK_ID, TARGET_MAJOR_VERSION);
        StackDto stackDto = spy(StackDto.class);
        ClusterView cluster = mock(ClusterView.class);

        when(embeddedDatabaseService.isAttachedDiskForEmbeddedDatabaseCreated(stackDto)).thenReturn(false);
        when(stackDtoService.getById(STACK_ID)).thenReturn(stackDto);
        when(stackDto.getCluster()).thenReturn(cluster);
        when(cluster.getDatabaseServerCrn()).thenReturn(null);
        when(event.getData()).thenReturn(request);

        Selectable result = underTest.doAccept(event);

        verify(targetMajorVersionToUpgradeTargetVersionConverter, never()).convert(TARGET_MAJOR_VERSION);
        verify(databaseService, never()).upgradeDatabase(any(ClusterView.class), any(UpgradeTargetMajorVersion.class), any(DatabaseServerV4StackRequest.class));
        verify(databaseService, never()).migrateDatabaseSettingsIfNeeded(any(StackDto.class), any(TargetMajorVersion.class));
        verify(environmentClientService, never()).getByCrn(any());
        verify(rdsUpgradeOrchestratorService, never()).upgradeEmbeddedDatabase(stackDto);
        assertThat(result.selector()).isEqualTo("UPGRADERDSFAILEDEVENT");
        assertThat(result).isInstanceOf(UpgradeRdsFailedEvent.class);
        UpgradeRdsFailedEvent failedEvent = (UpgradeRdsFailedEvent) result;
        assertThat(failedEvent.getException().getMessage()).isEqualTo("Upgrade cannot be performed with embedded database present on root disk!");
    }

    @Test
    void doAcceptWhenOrchestrationException() throws CloudbreakOrchestratorException {
        UpgradeRdsUpgradeDatabaseServerRequest request = new UpgradeRdsUpgradeDatabaseServerRequest(STACK_ID, TARGET_MAJOR_VERSION);
        StackDto stackDto = spy(StackDto.class);
        ClusterView cluster = mock(ClusterView.class);

        when(embeddedDatabaseService.isAttachedDiskForEmbeddedDatabaseCreated(stackDto)).thenReturn(true);
        when(stackDtoService.getById(STACK_ID)).thenReturn(stackDto);
        when(stackDto.getCluster()).thenReturn(cluster);
        when(cluster.getDatabaseServerCrn()).thenReturn(null);
        when(event.getData()).thenReturn(request);

        doThrow(new CloudbreakOrchestratorFailedException("salt error")).when(rdsUpgradeOrchestratorService).upgradeEmbeddedDatabase(stackDto);

        Selectable result = underTest.doAccept(event);

        verify(rdsUpgradeOrchestratorService).upgradeEmbeddedDatabase(stackDto);
        assertThat(result.selector()).isEqualTo("UPGRADERDSFAILEDEVENT");
        assertThat(result).isInstanceOf(UpgradeRdsFailedEvent.class);
        UpgradeRdsFailedEvent failedEvent = (UpgradeRdsFailedEvent) result;
        assertThat(failedEvent.getException().getMessage()).isEqualTo("salt error");
    }
}