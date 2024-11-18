package com.sequenceiq.cloudbreak.core.flow2.externaldatabase.user.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.common.database.DatabaseCommon;
import com.sequenceiq.cloudbreak.core.bootstrap.service.container.postgres.PostgresConfigService;
import com.sequenceiq.cloudbreak.core.flow2.externaldatabase.user.ExternalDatabaseUserOperation;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;
import com.sequenceiq.cloudbreak.reactor.api.event.externaldatabase.UserOperationExternalDatabaseFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.externaldatabase.UserOperationExternalDatabaseRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.externaldatabase.UserOperationExternalDatabaseResult;
import com.sequenceiq.cloudbreak.rotation.SecretRotationSaltService;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;
import com.sequenceiq.cloudbreak.service.rdsconfig.RedbeamsDbServerConfigurer;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerV4Response;

@ExtendWith(MockitoExtension.class)
public class ExecuteExternalDatabaseUserOperationHandlerTest {

    private static final String DL_CRN = "crn:cdp:datalake:us-west-1:tenant:datalake:crn1";

    private static final String DH_CRN = "crn:cdp:datahub:us-west-1:tenant:cluster:crn2";

    private static final String DB_SERVER_CRN = "crn:cdp:redbeams:us-west-1:default:databaseServer:e63520c8-aaf0-4bf3-b872-5613ce496ac3";

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private RedbeamsDbServerConfigurer redbeamsDbServerConfigurer;

    @Mock
    private PostgresConfigService postgresConfigService;

    @Mock
    private SecretRotationSaltService secretRotationSaltService;

    @Mock
    private RdsConfigService rdsConfigService;

    @Mock
    private DatabaseCommon dbCommon;

    @Mock
    private EventBus eventBus;

    @InjectMocks
    private ExecuteExternalDatabaseUserOperationHandler underTest;

    @Test
    void testUserCreationIfNotExists() throws CloudbreakOrchestratorFailedException {
        StackDto datalake = mock(StackDto.class);
        ClusterView datalakeCluster = mock(ClusterView.class);
        when(datalakeCluster.getDatabaseServerCrn()).thenReturn(DB_SERVER_CRN);
        when(datalake.getCluster()).thenReturn(datalakeCluster);
        when(stackDtoService.getByCrn(eq(DL_CRN))).thenReturn(datalake);
        when(postgresConfigService.getDatabaseNameByType(any())).thenReturn("hive");
        DatabaseServerV4Response dbServer = mock(DatabaseServerV4Response.class);
        when(redbeamsDbServerConfigurer.getDatabaseServer(any())).thenReturn(dbServer);
        when(rdsConfigService.findAllByConnectionUrlAndType(any())).thenReturn(Set.of());
        RDSConfig rdsConfig = mock(RDSConfig.class);
        when(redbeamsDbServerConfigurer.createNewRdsConfigForNewUser(any(), any(), any(), any(), any(), any(), any())).thenReturn(rdsConfig);
        when(postgresConfigService.getPillarPropertiesForUserCreation(any(), any(), any())).thenReturn(new SaltPillarProperties("path", Map.of()));
        doNothing().when(secretRotationSaltService).updateSaltPillar(any(), any(), any());
        doNothing().when(secretRotationSaltService).executeSaltStateOnPrimaryGateway(any(), any());
        doNothing().when(eventBus).notify(any(), any());

        underTest.accept(new Event<>(new UserOperationExternalDatabaseRequest(1L, null, null, DL_CRN,
                ExternalDatabaseUserOperation.CREATION, DatabaseType.HIVE, "dbUser")));

        ArgumentCaptor<Map<String, SaltPillarProperties>> saltPillarPropertiesArgumentCaptor = ArgumentCaptor.forClass(Map.class);
        ArgumentCaptor<List<String>> saltStatesArgumentCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);

        verify(secretRotationSaltService).updateSaltPillar(any(), saltPillarPropertiesArgumentCaptor.capture(), any());
        assertEquals("path", saltPillarPropertiesArgumentCaptor.getValue().get(PostgresConfigService.POSTGRES_USER).getPath());
        verify(secretRotationSaltService).executeSaltStateOnPrimaryGateway(any(), saltStatesArgumentCaptor.capture());
        assertEquals("postgresql.newuser.init", saltStatesArgumentCaptor.getValue().getFirst());
        verify(rdsConfigService).pureSave(any());
        verify(eventBus).notify(any(), eventCaptor.capture());
        assertEquals(UserOperationExternalDatabaseResult.class, eventCaptor.getValue().getData().getClass());
    }

    @Test
    void testUserCreationIfExists() throws CloudbreakOrchestratorFailedException {
        StackDto datalake = mock(StackDto.class);
        ClusterView datalakeCluster = mock(ClusterView.class);
        when(datalakeCluster.getDatabaseServerCrn()).thenReturn(DB_SERVER_CRN);
        when(datalake.getCluster()).thenReturn(datalakeCluster);
        when(stackDtoService.getByCrn(eq(DL_CRN))).thenReturn(datalake);
        when(postgresConfigService.getDatabaseNameByType(any())).thenReturn("hive");
        DatabaseServerV4Response dbServer = mock(DatabaseServerV4Response.class);
        when(redbeamsDbServerConfigurer.getDatabaseServer(any())).thenReturn(dbServer);
        RDSConfig rdsConfig = mock(RDSConfig.class);
        when(rdsConfig.getConnectionUserName()).thenReturn("dbUser");
        when(rdsConfigService.findAllByConnectionUrlAndType(any())).thenReturn(Set.of(rdsConfig));
        when(postgresConfigService.getPillarPropertiesForUserCreation(any(), any(), any())).thenReturn(new SaltPillarProperties("path", Map.of()));
        doNothing().when(secretRotationSaltService).updateSaltPillar(any(), any(), any());
        doNothing().when(secretRotationSaltService).executeSaltStateOnPrimaryGateway(any(), any());
        doNothing().when(eventBus).notify(any(), any());

        underTest.accept(new Event<>(new UserOperationExternalDatabaseRequest(1L, null, null, DL_CRN,
                ExternalDatabaseUserOperation.CREATION, DatabaseType.HIVE, "dbUser")));

        ArgumentCaptor<Map<String, SaltPillarProperties>> saltPillarPropertiesArgumentCaptor = ArgumentCaptor.forClass(Map.class);
        ArgumentCaptor<List<String>> saltStatesArgumentCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);

        verify(secretRotationSaltService).updateSaltPillar(any(), saltPillarPropertiesArgumentCaptor.capture(), any());
        assertEquals("path", saltPillarPropertiesArgumentCaptor.getValue().get(PostgresConfigService.POSTGRES_USER).getPath());
        verify(secretRotationSaltService).executeSaltStateOnPrimaryGateway(any(), saltStatesArgumentCaptor.capture());
        assertEquals("postgresql.newuser.init", saltStatesArgumentCaptor.getValue().getFirst());
        verify(rdsConfigService).pureSave(any());
        verify(redbeamsDbServerConfigurer, never()).createNewRdsConfigForNewUser(any(), any(), any(), any(), any(), any(), any());
        verify(eventBus).notify(any(), eventCaptor.capture());
        assertEquals(UserOperationExternalDatabaseResult.class, eventCaptor.getValue().getData().getClass());
    }

    @Test
    void testUserDeletionIfExists() throws CloudbreakOrchestratorFailedException {
        StackDto datalake = mock(StackDto.class);
        ClusterView datalakeCluster = mock(ClusterView.class);
        when(datalakeCluster.getDatabaseServerCrn()).thenReturn(DB_SERVER_CRN);
        when(datalake.getCluster()).thenReturn(datalakeCluster);
        when(stackDtoService.getByCrn(eq(DL_CRN))).thenReturn(datalake);
        when(postgresConfigService.getDatabaseNameByType(any())).thenReturn("hive");
        DatabaseServerV4Response dbServer = mock(DatabaseServerV4Response.class);
        when(redbeamsDbServerConfigurer.getDatabaseServer(any())).thenReturn(dbServer);
        RDSConfig rdsConfig = mock(RDSConfig.class);
        when(rdsConfig.getConnectionUserName()).thenReturn("dbUser");
        when(rdsConfigService.findAllByConnectionUrlAndType(any())).thenReturn(Set.of(rdsConfig));
        when(postgresConfigService.getPillarPropertiesForUserDeletion(any(), any(), any())).thenReturn(new SaltPillarProperties("path", Map.of()));
        doNothing().when(secretRotationSaltService).updateSaltPillar(any(), any(), any());
        doNothing().when(secretRotationSaltService).executeSaltStateOnPrimaryGateway(any(), any());
        doNothing().when(eventBus).notify(any(), any());

        underTest.accept(new Event<>(new UserOperationExternalDatabaseRequest(1L, null, null, DL_CRN,
                ExternalDatabaseUserOperation.DELETION, DatabaseType.HIVE, "dbUser")));

        ArgumentCaptor<Map<String, SaltPillarProperties>> saltPillarPropertiesArgumentCaptor = ArgumentCaptor.forClass(Map.class);
        ArgumentCaptor<List<String>> saltStatesArgumentCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);

        verify(secretRotationSaltService).updateSaltPillar(any(), saltPillarPropertiesArgumentCaptor.capture(), any());
        assertEquals("path", saltPillarPropertiesArgumentCaptor.getValue().get(PostgresConfigService.POSTGRES_USER).getPath());
        verify(secretRotationSaltService).executeSaltStateOnPrimaryGateway(any(), saltStatesArgumentCaptor.capture());
        assertEquals("postgresql.deleteuser.init", saltStatesArgumentCaptor.getValue().getFirst());
        verify(rdsConfigService).delete(any(RDSConfig.class));
        verify(redbeamsDbServerConfigurer, never()).createNewRdsConfigForNewUser(any(), any(), any(), any(), any(), any(), any());
        verify(eventBus).notify(any(), eventCaptor.capture());
        assertEquals(UserOperationExternalDatabaseResult.class, eventCaptor.getValue().getData().getClass());
    }

    @Test
    void testUserDeletionIfNotExists() throws CloudbreakOrchestratorFailedException {
        StackDto datalake = mock(StackDto.class);
        ClusterView datalakeCluster = mock(ClusterView.class);
        when(datalakeCluster.getDatabaseServerCrn()).thenReturn(DB_SERVER_CRN);
        when(datalake.getCluster()).thenReturn(datalakeCluster);
        when(stackDtoService.getByCrn(eq(DL_CRN))).thenReturn(datalake);
        when(postgresConfigService.getDatabaseNameByType(any())).thenReturn("hive");
        DatabaseServerV4Response dbServer = mock(DatabaseServerV4Response.class);
        when(redbeamsDbServerConfigurer.getDatabaseServer(any())).thenReturn(dbServer);
        when(rdsConfigService.findAllByConnectionUrlAndType(any())).thenReturn(Set.of());
        when(postgresConfigService.getPillarPropertiesForUserDeletion(any(), any(), any())).thenReturn(new SaltPillarProperties("path", Map.of()));
        doNothing().when(secretRotationSaltService).updateSaltPillar(any(), any(), any());
        doNothing().when(secretRotationSaltService).executeSaltStateOnPrimaryGateway(any(), any());
        doNothing().when(eventBus).notify(any(), any());

        underTest.accept(new Event<>(new UserOperationExternalDatabaseRequest(1L, null, null, DL_CRN,
                ExternalDatabaseUserOperation.DELETION, DatabaseType.HIVE, "dbUser")));

        ArgumentCaptor<Map<String, SaltPillarProperties>> saltPillarPropertiesArgumentCaptor = ArgumentCaptor.forClass(Map.class);
        ArgumentCaptor<List<String>> saltStatesArgumentCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);

        verify(secretRotationSaltService).updateSaltPillar(any(), saltPillarPropertiesArgumentCaptor.capture(), any());
        assertEquals("path", saltPillarPropertiesArgumentCaptor.getValue().get(PostgresConfigService.POSTGRES_USER).getPath());
        verify(secretRotationSaltService).executeSaltStateOnPrimaryGateway(any(), saltStatesArgumentCaptor.capture());
        assertEquals("postgresql.deleteuser.init", saltStatesArgumentCaptor.getValue().getFirst());
        verify(rdsConfigService, never()).delete(any(RDSConfig.class));
        verify(redbeamsDbServerConfigurer, never()).createNewRdsConfigForNewUser(any(), any(), any(), any(), any(), any(), any());
        verify(eventBus).notify(any(), eventCaptor.capture());
        assertEquals(UserOperationExternalDatabaseResult.class, eventCaptor.getValue().getData().getClass());
    }

    @Test
    void testInvalidDbCrn() {
        StackDto datalake = mock(StackDto.class);
        ClusterView datalakeCluster = mock(ClusterView.class);
        when(datalakeCluster.getDatabaseServerCrn()).thenReturn("");
        when(datalake.getCluster()).thenReturn(datalakeCluster);
        when(stackDtoService.getByCrn(eq(DL_CRN))).thenReturn(datalake);

        underTest.accept(new Event<>(new UserOperationExternalDatabaseRequest(1L, null, null, DL_CRN,
                ExternalDatabaseUserOperation.DELETION, DatabaseType.HIVE, "dbUser")));

        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventBus).notify(any(), eventCaptor.capture());
        assertEquals(UserOperationExternalDatabaseFailed.class, eventCaptor.getValue().getData().getClass());
        assertEquals("Database user operation is supported for external databases only!",
                ((UserOperationExternalDatabaseFailed) eventCaptor.getValue().getData()).getException().getMessage());
    }
}
