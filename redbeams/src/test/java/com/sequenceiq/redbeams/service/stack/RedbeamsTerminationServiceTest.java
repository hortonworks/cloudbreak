package com.sequenceiq.redbeams.service.stack;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.redbeams.api.model.common.DetailedDBStackStatus;
import com.sequenceiq.redbeams.api.model.common.Status;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.domain.stack.DBStackStatus;
import com.sequenceiq.redbeams.flow.RedbeamsFlowManager;
import com.sequenceiq.redbeams.flow.redbeams.termination.RedbeamsTerminationEvent;

@ExtendWith(MockitoExtension.class)
class RedbeamsTerminationServiceTest {

    private static final long DBSTACK_ID = 1L;

    private static final String DATABASE_SERVER_CRN = "databaseServerCrn";

    @Mock
    private DBStackService dbStackService;

    @Mock
    private DBStackStatusUpdater dbStackStatusUpdater;

    @Mock
    private RedbeamsFlowManager flowManager;

    @InjectMocks
    private RedbeamsTerminationService underTest;

    private DBStack dbStack;

    @BeforeEach
    void setup() {
        dbStack = new DBStack();
        dbStack.setId(DBSTACK_ID);

        DBStackStatus dbStackStatus = new DBStackStatus();
        dbStack.setDBStackStatus(dbStackStatus);
        dbStack.getDbStackStatus().setStatus(Status.AVAILABLE);
    }

    @Test
    void testTerminateDatabaseServer() {
        when(dbStackService.getByCrn(anyString())).thenReturn(dbStack);
        when(dbStackStatusUpdater.updateStatus(DBSTACK_ID, DetailedDBStackStatus.DELETE_REQUESTED)).thenReturn(dbStack);

        underTest.terminateDatabaseServer(DATABASE_SERVER_CRN, true);

        verify(dbStackStatusUpdater).updateStatus(DBSTACK_ID, DetailedDBStackStatus.DELETE_REQUESTED);
        InOrder inOrder = Mockito.inOrder(flowManager);
        inOrder.verify(flowManager).cancelRunningFlows(DBSTACK_ID);
        inOrder.verify(flowManager).notify(eq(RedbeamsTerminationEvent.REDBEAMS_TERMINATION_EVENT.selector()), any());
    }

    @ParameterizedTest
    @EnumSource(value = Status.class, names = {"DELETE_REQUESTED", "PRE_DELETE_IN_PROGRESS", "DELETE_IN_PROGRESS", "DELETE_COMPLETED"})
    void testTerminateDatabaseServerWhenTerminationIsInProgress(Status status) {
        dbStack.getDbStackStatus().setStatus(status);
        when(dbStackService.getByCrn(anyString())).thenReturn(dbStack);

        DBStack dbStack = underTest.terminateDatabaseServer(DATABASE_SERVER_CRN, true);

        Assertions.assertEquals(status, dbStack.getStatus());

        verify(dbStackStatusUpdater, never()).updateStatus(anyLong(), any());
        verify(flowManager, never()).cancelRunningFlows(DBSTACK_ID);
        verify(flowManager, never()).notify(eq(RedbeamsTerminationEvent.REDBEAMS_TERMINATION_EVENT.selector()), any());
    }
}
