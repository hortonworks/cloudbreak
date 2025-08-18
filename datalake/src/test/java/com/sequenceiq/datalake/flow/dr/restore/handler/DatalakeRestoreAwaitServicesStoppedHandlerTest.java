package com.sequenceiq.datalake.flow.dr.restore.handler;

import static com.sequenceiq.datalake.flow.dr.restore.DatalakeRestoreEvent.DATALAKE_DATABASE_RESTORE_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.dyngr.exception.PollerException;
import com.dyngr.exception.PollerStoppedException;
import com.dyngr.exception.UserBreakException;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.datalake.entity.operation.SdxOperation;
import com.sequenceiq.datalake.entity.operation.SdxOperationType;
import com.sequenceiq.datalake.flow.dr.restore.event.DatalakeDatabaseRestoreStartEvent;
import com.sequenceiq.datalake.flow.dr.restore.event.DatalakeRestoreAwaitServicesStoppedRequest;
import com.sequenceiq.datalake.flow.dr.restore.event.DatalakeRestoreFailedEvent;
import com.sequenceiq.datalake.repository.SdxOperationRepository;
import com.sequenceiq.datalake.service.sdx.PollingConfig;
import com.sequenceiq.datalake.service.sdx.dr.SdxBackupRestoreService;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@ExtendWith(MockitoExtension.class)
class DatalakeRestoreAwaitServicesStoppedHandlerTest {

    private static final long SDX_ID = 99L;

    private static final String USER_ID = "userId";

    private static final String BACKUP_ID = "backupId";

    private static final String RESTORE_ID = "restoreId";

    private static final String BACKUP_LOCATION = "s3://loc";

    @Mock
    private SdxBackupRestoreService sdxBackupRestoreService;

    @Mock
    private SdxOperationRepository sdxOperationRepository;

    @InjectMocks
    private DatalakeRestoreAwaitServicesStoppedHandler underTest;

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(underTest, "sleepTimeInSec", 1);
        ReflectionTestUtils.setField(underTest, "durationInMinutes", 2);
    }

    @Test
    void testSelector() {
        assertEquals(EventSelectorUtil.selector(DatalakeRestoreAwaitServicesStoppedRequest.class), underTest.selector());
    }

    private HandlerEvent<DatalakeRestoreAwaitServicesStoppedRequest> buildEvent(boolean validationOnly, int dbMaxMinutes) {
        SdxOperation dr = new SdxOperation();
        dr.setOperationType(SdxOperationType.RESTORE);
        dr.setOperationId(RESTORE_ID);
        DatalakeDatabaseRestoreStartEvent start = new DatalakeDatabaseRestoreStartEvent(
                DATALAKE_DATABASE_RESTORE_EVENT.event(), SDX_ID, dr, USER_ID,
                BACKUP_ID, RESTORE_ID, BACKUP_LOCATION, dbMaxMinutes, validationOnly);
        DatalakeRestoreAwaitServicesStoppedRequest req = DatalakeRestoreAwaitServicesStoppedRequest.from(start);
        return new HandlerEvent<>(new Event<>(req));
    }

    @Test
    void testValidationOnlyReturnsStartEventWithoutPolling() {
        Selectable result = underTest.doAccept(buildEvent(true, 15));

        assertNotNull(result);
        assertTrue(result instanceof DatalakeDatabaseRestoreStartEvent);
        DatalakeDatabaseRestoreStartEvent output = (DatalakeDatabaseRestoreStartEvent) result;
        assertEquals(SDX_ID, output.getResourceId());
        assertEquals(USER_ID, output.getUserId());
        assertEquals(BACKUP_ID, output.getBackupId());
        assertEquals(RESTORE_ID, output.getRestoreId());
        assertEquals(BACKUP_LOCATION, output.getBackupLocation());
        assertEquals(0, output.getDatabaseMaxDurationInMin());
        assertTrue(output.isValidationOnly());

        verifyNoInteractions(sdxOperationRepository, sdxBackupRestoreService);
    }

    @Test
    void testSuccessfulPollingReturnsNextStartEvent() {
        Selectable result = underTest.doAccept(buildEvent(false, 20));

        assertNotNull(result);
        assertTrue(result instanceof DatalakeDatabaseRestoreStartEvent);
        DatalakeDatabaseRestoreStartEvent output = (DatalakeDatabaseRestoreStartEvent) result;
        assertEquals(SDX_ID, output.getResourceId());
        assertEquals(USER_ID, output.getUserId());
        assertEquals(BACKUP_ID, output.getBackupId());
        assertEquals(RESTORE_ID, output.getRestoreId());
        assertEquals(BACKUP_LOCATION, output.getBackupLocation());
        assertEquals(20, output.getDatabaseMaxDurationInMin());
        assertTrue(!output.isValidationOnly());

        verify(sdxOperationRepository, times(1)).save(any());
        verify(sdxBackupRestoreService, times(1))
                .waitForServiceToBeStopped(eq(SDX_ID), eq(RESTORE_ID), eq(USER_ID), any(PollingConfig.class), anyString(), eq(SdxOperationType.RESTORE));
    }

    @Test
    void testUserBreakExceptionWhilePolling() {
        doThrow(new UserBreakException())
                .when(sdxBackupRestoreService)
                .waitForServiceToBeStopped(anyLong(), anyString(), anyString(), any(PollingConfig.class), anyString(), any());

        DatalakeRestoreFailedEvent result = (DatalakeRestoreFailedEvent) underTest.doAccept(buildEvent(false, 10));

        assertEquals(UserBreakException.class, result.getException().getClass());
        assertEquals(SDX_ID, result.getResourceId());
        assertEquals(USER_ID, result.getUserId());
    }

    @Test
    void testPollerStoppedExceptionWhilePolling() {
        doThrow(new PollerStoppedException())
                .when(sdxBackupRestoreService)
                .waitForServiceToBeStopped(anyLong(), anyString(), anyString(), any(PollingConfig.class), anyString(), any());

        DatalakeRestoreFailedEvent result = (DatalakeRestoreFailedEvent) underTest.doAccept(buildEvent(false, 10));

        assertTrue(result.getException() instanceof PollerStoppedException);
        assertTrue(result.getException().getMessage().contains("timed out after 2 minutes"));
        assertEquals(SDX_ID, result.getResourceId());
        assertEquals(USER_ID, result.getUserId());
    }

    @Test
    void testPollerExceptionWhilePolling() {
        doThrow(new PollerException())
                .when(sdxBackupRestoreService)
                .waitForServiceToBeStopped(anyLong(), anyString(), anyString(), any(PollingConfig.class), anyString(), any());

        DatalakeRestoreFailedEvent result = (DatalakeRestoreFailedEvent) underTest.doAccept(buildEvent(false, 10));

        assertTrue(result.getException() instanceof PollerException);
        assertEquals(SDX_ID, result.getResourceId());
        assertEquals(USER_ID, result.getUserId());
    }
}