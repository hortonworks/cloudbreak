package com.sequenceiq.redbeams.service.stack;

import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.redbeams.api.model.common.DetailedDBStackStatus;
import com.sequenceiq.redbeams.api.model.common.Status;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.flow.RedbeamsFlowManager;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsEvent;
import com.sequenceiq.redbeams.flow.redbeams.start.RedbeamsStartEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class RedbeamsStartServiceTest {

    private static final Crn CRN = Crn.builder()
            .setService(Crn.Service.REDBEAMS)
            .setAccountId("accountId")
            .setResourceType(Crn.ResourceType.DATABASE_SERVER)
            .setResource("resource")
            .build();

    private static final String CRN_STRING = CRN.toString();

    @Mock
    private DBStackService dbStackService;

    @Mock
    private DBStackStatusUpdater dbStackStatusUpdater;

    @Mock
    private RedbeamsFlowManager flowManager;

    @Mock
    private DBStack dbStack;

    @InjectMocks
    private RedbeamsStartService victim;

    @BeforeEach
    public void initTest() {
        when(dbStackService.getByCrn(CRN_STRING)).thenReturn(dbStack);
    }

    @Test
    public void shouldSetStartRequestedStatusAndNotifyTheFlowManager() {
        when(dbStack.getId()).thenReturn(1L);
        when(dbStack.getStatus()).thenReturn(Status.STOPPED);
        when(dbStackStatusUpdater.updateStatus(dbStack.getId(), DetailedDBStackStatus.START_REQUESTED)).thenReturn(dbStack);
        ArgumentCaptor<RedbeamsEvent> redbeamsEventArgumentCaptor = ArgumentCaptor.forClass(RedbeamsEvent.class);

        victim.startDatabaseServer(CRN_STRING);

        verify(flowManager).notify(eq(RedbeamsStartEvent.REDBEAMS_START_EVENT.selector()), redbeamsEventArgumentCaptor.capture());

        assertEquals(dbStack.getId(), redbeamsEventArgumentCaptor.getValue().getResourceId());
        assertEquals(RedbeamsStartEvent.REDBEAMS_START_EVENT.selector(), redbeamsEventArgumentCaptor.getValue().selector());
    }

    @Test
    public void shouldReturnWithoutAnyActionWhenStartInProgress() {
        when(dbStack.getStatus()).thenReturn(Status.START_IN_PROGRESS);

        victim.startDatabaseServer(CRN_STRING);

        verifyZeroInteractions(dbStackStatusUpdater, flowManager);
    }
}