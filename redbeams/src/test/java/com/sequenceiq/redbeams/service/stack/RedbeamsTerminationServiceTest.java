package com.sequenceiq.redbeams.service.stack;

import static com.sequenceiq.redbeams.api.model.common.Status.DELETE_COMPLETED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.crn.CrnTestUtil;
import com.sequenceiq.flow.core.FlowLogService;
import com.sequenceiq.flow.service.FlowCancelService;
import com.sequenceiq.redbeams.api.endpoint.v4.ResourceStatus;
import com.sequenceiq.redbeams.api.model.common.DetailedDBStackStatus;
import com.sequenceiq.redbeams.api.model.common.Status;
import com.sequenceiq.redbeams.domain.DatabaseServerConfig;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.domain.stack.DBStackStatus;
import com.sequenceiq.redbeams.flow.RedbeamsFlowManager;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsEvent;
import com.sequenceiq.redbeams.flow.redbeams.termination.RedbeamsTerminationEvent;
import com.sequenceiq.redbeams.flow.redbeams.termination.RedbeamsTerminationFlowConfig;
import com.sequenceiq.redbeams.service.dbserverconfig.DatabaseServerConfigService;

@ExtendWith(MockitoExtension.class)
class RedbeamsTerminationServiceTest {

    private static final Crn SERVER_CRN = CrnTestUtil.getDatabaseServerCrnBuilder()
            .setAccountId("accountId")
            .setResource("resource")
            .build();

    private static final String SERVER_CRN_STRING = SERVER_CRN.toString();

    private static final Crn SERVER_2_CRN = CrnTestUtil.getDatabaseServerCrnBuilder()
            .setAccountId("accountId")
            .setResource("resourceother")
            .build();

    private static final String SERVER_2_CRN_STRING = SERVER_2_CRN.toString();

    private static final String ENVIRONMENT_CRN = "envCrn";

    private static final String DATABASE_SERVER_NAME = "myserver";

    @Mock
    private DBStackService dbStackService;

    @Mock
    private DBStackStatusUpdater dbStackStatusUpdater;

    @Mock
    private RedbeamsFlowManager flowManager;

    @Mock
    private DatabaseServerConfigService databaseServerConfigService;

    @Mock
    private FlowCancelService cancelService;

    @Mock
    private FlowLogService flowLogService;

    @InjectMocks
    private RedbeamsTerminationService underTest;

    private DBStack dbStack;

    private DBStack dbStack2;

    private DatabaseServerConfig server;

    private DatabaseServerConfig server2;

    @BeforeEach
    void setup() {
        dbStack = new DBStack();
        dbStack.setId(1L);

        DBStackStatus dbStackStatus = new DBStackStatus();
        dbStack.setDBStackStatus(dbStackStatus);
        dbStack.getDbStackStatus().setStatus(Status.AVAILABLE);

        dbStack2 = new DBStack();
        dbStack2.setId(2L);

        dbStackStatus = new DBStackStatus();
        dbStack2.setDBStackStatus(dbStackStatus);
        dbStack2.getDbStackStatus().setStatus(Status.AVAILABLE);

        server = new DatabaseServerConfig();
        server.setId(1L);
        server.setResourceCrn(SERVER_CRN);
        server.setResourceStatus(ResourceStatus.SERVICE_MANAGED);

        server2 = new DatabaseServerConfig();
        server2.setId(2L);
        server2.setResourceCrn(SERVER_2_CRN);
        server2.setResourceStatus(ResourceStatus.SERVICE_MANAGED);
    }

    @Test
    void testTerminateByCrn() {
        mockTerminationByCrn(SERVER_CRN_STRING, dbStack, server);

        DatabaseServerConfig terminatingServer = underTest.terminateByCrn(SERVER_CRN_STRING, true);

        assertEquals(server, terminatingServer);

        verifyTermination(1L);
    }

    @Test
    void testTerminateByName() {
        when(databaseServerConfigService.getByName(anyLong(), eq(ENVIRONMENT_CRN), eq(DATABASE_SERVER_NAME))).thenReturn(server);
        mockTerminationByCrn(SERVER_CRN_STRING, dbStack, server);

        DatabaseServerConfig terminatingServer = underTest.terminateByName(ENVIRONMENT_CRN, DATABASE_SERVER_NAME, true);

        assertEquals(server, terminatingServer);

        verifyTermination(1L);
    }

    @Test
    void testTerminateWhenTerminationIsDeleteCompletedAndArchivedShouldNotCallTermination() {
        when(databaseServerConfigService.getByCrn(SERVER_CRN_STRING)).thenReturn(server);
        dbStack.getDbStackStatus().setStatus(DELETE_COMPLETED);
        server.setArchived(true);
        when(dbStackService.getByCrn(SERVER_CRN_STRING)).thenReturn(dbStack);

        DatabaseServerConfig terminatingServer = underTest.terminateByCrn(SERVER_CRN_STRING, true);

        assertEquals(server, terminatingServer);

        verify(dbStackStatusUpdater, never()).updateStatus(anyLong(), any());
        verify(cancelService, never()).cancelRunningFlows(1L);
        verify(flowManager, never()).notify(eq(RedbeamsTerminationEvent.REDBEAMS_TERMINATION_EVENT.selector()), any());
    }

    @Test
    void testTerminateWhenTerminationIsDeletedCompletedAndNOTArchivedShouldCallTermination() {
        when(databaseServerConfigService.getByCrn(SERVER_CRN_STRING)).thenReturn(server);
        dbStack.getDbStackStatus().setStatus(DELETE_COMPLETED);
        server.setArchived(false);
        when(dbStackService.getByCrn(SERVER_CRN_STRING)).thenReturn(dbStack);

        DatabaseServerConfig terminatingServer = underTest.terminateByCrn(SERVER_CRN_STRING, true);

        assertEquals(server, terminatingServer);

        verify(dbStackStatusUpdater).updateStatus(anyLong(), any());
        verify(cancelService).cancelRunningFlows(1L);
        verify(flowManager).notify(eq(RedbeamsTerminationEvent.REDBEAMS_TERMINATION_EVENT.selector()), any());
    }

    @Test
    void testTerminateWhenTerminationTerminationFlowIsRunningAndIsDeletedCompletedAndNotArchivedShouldNotCallTermination() {
        when(databaseServerConfigService.getByCrn(SERVER_CRN_STRING)).thenReturn(server);
        dbStack.getDbStackStatus().setStatus(DELETE_COMPLETED);
        server.setArchived(false);
        when(flowLogService.isFlowConfigAlreadyRunning(1L, RedbeamsTerminationFlowConfig.class)).thenReturn(true);
        when(dbStackService.getByCrn(SERVER_CRN_STRING)).thenReturn(dbStack);

        DatabaseServerConfig terminatingServer = underTest.terminateByCrn(SERVER_CRN_STRING, true);

        assertEquals(server, terminatingServer);

        verify(dbStackStatusUpdater, never()).updateStatus(anyLong(), any());
        verify(cancelService, never()).cancelRunningFlows(1L);
        verify(flowManager, never()).notify(eq(RedbeamsTerminationEvent.REDBEAMS_TERMINATION_EVENT.selector()), any());
    }

    @Test
    void testTerminateMultiple() {
        Set<DatabaseServerConfig> serverSet = Set.of(server, server2);
        when(databaseServerConfigService.getByCrns(any(Set.class))).thenReturn(serverSet);
        mockTerminationByCrn(SERVER_CRN_STRING, dbStack, server);
        mockTerminationByCrn(SERVER_2_CRN_STRING, dbStack2, server2);

        Set<DatabaseServerConfig> terminatingServerSet = underTest.terminateMultipleByCrn(Set.of(SERVER_CRN_STRING, SERVER_2_CRN_STRING), true);

        assertEquals(serverSet, terminatingServerSet);

        verifyTermination(1L);
        verifyTermination(2L);
    }

    private void mockTerminationByCrn(String crn, DBStack dbStack, DatabaseServerConfig server) {
        doReturn(dbStack).when(dbStackService).getByCrn(crn);
        doReturn(server).when(databaseServerConfigService).getByCrn(crn);
        doReturn(Optional.of(dbStack)).when(dbStackStatusUpdater).updateStatus(dbStack.getId(), DetailedDBStackStatus.DELETE_REQUESTED);
    }

    private void verifyTermination(long id) {
        verify(dbStackStatusUpdater).updateStatus(id, DetailedDBStackStatus.DELETE_REQUESTED);
        ArgumentCaptor<RedbeamsEvent> eventCaptor = ArgumentCaptor.forClass(RedbeamsEvent.class);
        InOrder inOrder = inOrder(cancelService, flowManager);
        inOrder.verify(cancelService).cancelRunningFlows(id);
        inOrder.verify(flowManager).notify(eq(RedbeamsTerminationEvent.REDBEAMS_TERMINATION_EVENT.selector()), eventCaptor.capture());
        RedbeamsEvent event = eventCaptor.getValue();
        assertEquals(id, event.getResourceId().longValue());
        assertTrue(event.isForced());
    }
}
