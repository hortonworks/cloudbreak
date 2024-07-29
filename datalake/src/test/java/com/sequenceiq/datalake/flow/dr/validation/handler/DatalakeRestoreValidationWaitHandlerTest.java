package com.sequenceiq.datalake.flow.dr.validation.handler;

import static com.sequenceiq.datalake.flow.dr.validation.DatalakeRestoreValidationEvent.DATALAKE_RESTORE_VALIDATION_SUCCESS_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.dyngr.exception.UserBreakException;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.datalake.flow.SdxEvent;
import com.sequenceiq.datalake.flow.dr.validation.event.DatalakeRestoreValidationFailedEvent;
import com.sequenceiq.datalake.flow.dr.validation.event.DatalakeRestoreValidationWaitRequest;
import com.sequenceiq.datalake.service.sdx.PollingConfig;
import com.sequenceiq.datalake.service.sdx.dr.SdxBackupRestoreService;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@ExtendWith(MockitoExtension.class)
public class DatalakeRestoreValidationWaitHandlerTest {

    private static final long SDX_ID = 1L;

    private static final String USER_ID = "userId";

    private static final String OPERATION_ID = "restoreId";

    @Mock
    private SdxBackupRestoreService sdxBackupRestoreService;

    @InjectMocks
    private DatalakeRestoreValidationWaitHandler underTest;

    @Test
    public void testSelector() {
        assertEquals(EventSelectorUtil.selector(DatalakeRestoreValidationWaitRequest.class), underTest.selector());
        assertEquals(EventSelectorUtil.selector(DatalakeRestoreValidationFailedEvent.class),
                underTest.defaultFailureEvent(null, null, null).getSelector());
    }

    @Test
    public void testSuccessfulPolling() {
        DatalakeRestoreValidationWaitRequest request =
                new DatalakeRestoreValidationWaitRequest(SDX_ID, USER_ID, OPERATION_ID);
        SdxEvent result = (SdxEvent) underTest.doAccept(new HandlerEvent<>(new Event<>(request)));
        verify(sdxBackupRestoreService).waitForDatalakeDrRestoreToComplete(eq(SDX_ID), eq(OPERATION_ID), eq(USER_ID), any(PollingConfig.class),
                eq("Restore validation"));
        assertEquals(SDX_ID, result.getResourceId());
        assertEquals(USER_ID, result.getUserId());
        assertEquals(DATALAKE_RESTORE_VALIDATION_SUCCESS_EVENT.name(), result.getSelector());
    }

    @Test
    public void testUserBreakExceptionWhilePolling() {
        DatalakeRestoreValidationWaitRequest request =
                new DatalakeRestoreValidationWaitRequest(SDX_ID, USER_ID, OPERATION_ID);
        doThrow(new UserBreakException()).
                when(sdxBackupRestoreService).waitForDatalakeDrRestoreToComplete(eq(SDX_ID), eq(OPERATION_ID), eq(USER_ID), any(PollingConfig.class),
                        eq("Restore validation"));
        DatalakeRestoreValidationFailedEvent result =
                (DatalakeRestoreValidationFailedEvent) underTest.doAccept(
                        new HandlerEvent<>(new Event<>(request)));

        assertEquals(UserBreakException.class, result.getException().getClass());
        assertEquals(SDX_ID, result.getResourceId());
        assertEquals(USER_ID, result.getUserId());
    }
}
