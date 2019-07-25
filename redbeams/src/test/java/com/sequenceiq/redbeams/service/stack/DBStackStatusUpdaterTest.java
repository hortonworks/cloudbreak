package com.sequenceiq.redbeams.service.stack;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.redbeams.api.model.common.DetailedDBStackStatus;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.domain.stack.DBStackStatus;

public class DBStackStatusUpdaterTest {

    @Mock
    private DBStackService dbStackService;

    @Mock
    private Clock clock;

    @InjectMocks
    private DBStackStatusUpdater underTest;

    private DBStack dbStack;

    private long now;

    @Before
    public void setUp() {
        initMocks(this);

        dbStack = new DBStack();
        dbStack.setId(1L);

        when(dbStackService.getById(1L)).thenReturn(dbStack);
        when(dbStackService.save(dbStack)).thenReturn(dbStack);

        now = System.currentTimeMillis();
        when(clock.getCurrentTimeMillis()).thenReturn(now);
    }

    @Test
    public void testUpdateStatus() {
        dbStack.setDBStackStatus(new DBStackStatus(dbStack, DetailedDBStackStatus.CREATING_INFRASTRUCTURE, now));

        DBStack savedStack = underTest.updateStatus(1L, DetailedDBStackStatus.PROVISIONED, "because");

        verify(dbStackService).save(dbStack);

        DBStackStatus dbStackStatus = savedStack.getDbStackStatus();
        assertEquals(dbStack, dbStackStatus.getDBStack());
        assertEquals(DetailedDBStackStatus.PROVISIONED.getStatus(), dbStackStatus.getStatus());
        assertEquals("because", dbStackStatus.getStatusReason());
        assertEquals(DetailedDBStackStatus.PROVISIONED, dbStackStatus.getDetailedDBStackStatus());
        assertEquals(now, dbStackStatus.getCreated().longValue());
    }

    @Test
    public void testUpdateStatusSkippedWhenDeleteCompleted() {
        dbStack.setDBStackStatus(new DBStackStatus(dbStack, DetailedDBStackStatus.DELETE_COMPLETED, now));

        underTest.updateStatus(1L, DetailedDBStackStatus.PROVISIONED, "because");

        verify(dbStackService, never()).save(dbStack);
    }

}
