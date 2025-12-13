package com.sequenceiq.redbeams.service.stack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.crn.CrnTestUtil;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.flow.RedbeamsFlowManager;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsEvent;
import com.sequenceiq.redbeams.flow.redbeams.start.RedbeamsStartEvent;

@ExtendWith(MockitoExtension.class)
public class RedbeamsStartServiceTest {

    private static final Crn CRN = CrnTestUtil.getDatabaseServerCrnBuilder()
            .setAccountId("accountId")
            .setResource("resource")
            .build();

    private static final String CRN_STRING = CRN.toString();

    @Mock
    private DBStackService dbStackService;

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
        ArgumentCaptor<RedbeamsEvent> redbeamsEventArgumentCaptor = ArgumentCaptor.forClass(RedbeamsEvent.class);

        victim.startDatabaseServer(CRN_STRING);

        verify(flowManager).notify(eq(RedbeamsStartEvent.REDBEAMS_START_EVENT.selector()), redbeamsEventArgumentCaptor.capture());

        assertEquals(dbStack.getId(), redbeamsEventArgumentCaptor.getValue().getResourceId());
        assertEquals(RedbeamsStartEvent.REDBEAMS_START_EVENT.selector(), redbeamsEventArgumentCaptor.getValue().selector());
    }
}
