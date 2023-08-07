package com.sequenceiq.datalake.flow.dr.restore.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.dyngr.exception.PollerStoppedException;
import com.dyngr.exception.UserBreakException;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.datalake.flow.SdxEvent;
import com.sequenceiq.datalake.flow.dr.restore.event.DatalakeDatabaseRestoreFailedEvent;
import com.sequenceiq.datalake.flow.dr.restore.event.DatalakeDatabaseRestoreWaitRequest;
import com.sequenceiq.datalake.service.sdx.PollingConfig;
import com.sequenceiq.datalake.service.sdx.dr.SdxBackupRestoreService;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@ExtendWith(MockitoExtension.class)
public class DatalakeDatabaseRestoreWaitHandlerTest {

    private static long sdxId = 1L;

    private static String userId = "userId";

    private static String operationId = "operationId";

    private static Integer duration = 120;

    @Mock
    private SdxBackupRestoreService sdxBackupRestoreService;

    @InjectMocks
    private DatalakeDatabaseRestoreWaitHandler underTest;

    @Test
    public void testSelector() {
        assertEquals(EventSelectorUtil.selector(DatalakeDatabaseRestoreWaitRequest.class), underTest.selector());
    }

    @Test
    public void testSuccessfulPolling() {
        DatalakeDatabaseRestoreWaitRequest datalakeDatabaseRestoreWaitRequest =
                new DatalakeDatabaseRestoreWaitRequest(sdxId, userId, operationId, duration);
        SdxEvent result = (SdxEvent) underTest.doAccept(new HandlerEvent<>(new Event<>(datalakeDatabaseRestoreWaitRequest)));
        verify(sdxBackupRestoreService).waitCloudbreakFlow(eq(sdxId), any(PollingConfig.class), eq("Database restore"));
        assertEquals(sdxId, result.getResourceId());
        assertEquals(userId, result.getUserId());
    }

    @Test
    public void testUserBreakExceptionWhilePolling() {
        DatalakeDatabaseRestoreWaitRequest datalakeDatabaseRestoreWaitRequest =
                new DatalakeDatabaseRestoreWaitRequest(sdxId, userId, operationId, duration);
        doThrow(new UserBreakException()).
                when(sdxBackupRestoreService).waitCloudbreakFlow(eq(sdxId), any(PollingConfig.class),
                        eq("Database restore"));
        DatalakeDatabaseRestoreFailedEvent result =
                (DatalakeDatabaseRestoreFailedEvent) underTest.doAccept(
                        new HandlerEvent<>(new Event<>(datalakeDatabaseRestoreWaitRequest)));

        assertEquals(UserBreakException.class, result.getException().getClass());
        assertEquals(sdxId, result.getResourceId());
        assertEquals(userId, result.getUserId());
    }

    @Test
    public void testPollerStoppedExceptionWhilePolling() {
        DatalakeDatabaseRestoreWaitRequest datalakeDatabaseRestoreWaitRequest =
                new DatalakeDatabaseRestoreWaitRequest(sdxId, userId, operationId, duration);
        doThrow(new PollerStoppedException()).
                when(sdxBackupRestoreService).waitCloudbreakFlow(eq(sdxId), any(PollingConfig.class),
                        eq("Database restore"));
        DatalakeDatabaseRestoreFailedEvent result =
                (DatalakeDatabaseRestoreFailedEvent) underTest.doAccept(
                        new HandlerEvent<>(new Event<>(datalakeDatabaseRestoreWaitRequest)));

        assertTrue(result.getException().getMessage().contains("Database restore timed out after 120 minutes"));
        assertEquals(PollerStoppedException.class, result.getException().getClass());
        assertEquals(sdxId, result.getResourceId());
        assertEquals(userId, result.getUserId());
    }
}
