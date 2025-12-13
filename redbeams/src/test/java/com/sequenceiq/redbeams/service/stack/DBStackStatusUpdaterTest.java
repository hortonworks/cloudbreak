package com.sequenceiq.redbeams.service.stack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.redbeams.api.model.common.DetailedDBStackStatus;
import com.sequenceiq.redbeams.api.model.common.Status;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.domain.stack.DBStackStatus;
import com.sequenceiq.redbeams.service.store.RedbeamsInMemoryStateStoreUpdaterService;

@ExtendWith(MockitoExtension.class)
class DBStackStatusUpdaterTest {

    @Mock
    private DBStackService dbStackService;

    @Mock
    private Clock clock;

    @Mock
    private RedbeamsInMemoryStateStoreUpdaterService redbeamsInMemoryStateStoreUpdaterService;

    @InjectMocks
    private DBStackStatusUpdater underTest;

    private DBStack dbStack;

    private Optional<DBStack> dbStackOptional;

    private long now;

    @BeforeEach
    void setUp() {
        dbStack = new DBStack();
        dbStack.setId(1L);
        dbStackOptional = Optional.of(dbStack);

        when(dbStackService.findById(1L)).thenReturn(dbStackOptional);
        when(dbStackService.save(dbStack)).thenReturn(dbStack);

        now = System.currentTimeMillis();
        when(clock.getCurrentTimeMillis()).thenReturn(now);
    }

    @Test
    void testUpdateStatus() {
        dbStack.setDBStackStatus(new DBStackStatus(dbStack, DetailedDBStackStatus.CREATING_INFRASTRUCTURE, now));

        DBStack savedStack = underTest.updateStatus(1L, DetailedDBStackStatus.PROVISIONED, "because").get();

        verify(dbStackService).save(dbStack);
        verify(redbeamsInMemoryStateStoreUpdaterService).update(1L, Status.AVAILABLE);

        DBStackStatus dbStackStatus = savedStack.getDbStackStatus();
        assertEquals(dbStack, dbStackStatus.getDBStack());
        assertEquals(DetailedDBStackStatus.PROVISIONED.getStatus(), dbStackStatus.getStatus());
        assertEquals("because", dbStackStatus.getStatusReason());
        assertEquals(DetailedDBStackStatus.PROVISIONED, dbStackStatus.getDetailedDBStackStatus());
        assertEquals(now, dbStackStatus.getCreated().longValue());
    }

}