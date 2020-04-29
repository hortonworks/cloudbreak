package com.sequenceiq.datalake.flow.stop.handler;

import com.dyngr.exception.PollerException;
import com.dyngr.exception.PollerStoppedException;
import com.dyngr.exception.UserBreakException;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.stop.event.RdsStopSuccessEvent;
import com.sequenceiq.datalake.flow.stop.event.RdsWaitingToStopRequest;
import com.sequenceiq.datalake.flow.stop.event.SdxStopFailedEvent;
import com.sequenceiq.datalake.repository.SdxClusterRepository;
import com.sequenceiq.datalake.service.sdx.database.DatabaseService;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.datalake.service.pause.DatabasePauseSupportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.bus.Event;
import reactor.bus.EventBus;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class RdsStopHandlerTest {

    private static final Long SDX_ID = 123L;

    private static final String USER_ID = "user";

    @Mock
    private SdxClusterRepository sdxClusterRepository;

    @Mock
    private DatabaseService databaseService;

    @Mock
    private SdxStatusService sdxStatusService;

    @Mock
    private Event<RdsWaitingToStopRequest> event;

    @Mock
    private RdsWaitingToStopRequest rdsWaitingToStopRequest;

    @Mock
    private SdxCluster sdxCluster;

    @Mock
    private EventBus eventBus;

    @Mock
    private DatabasePauseSupportService databasePauseSupportService;

    @InjectMocks
    private RdsStopHandler victim;

    @BeforeEach
    public void initMocks() {
        MockitoAnnotations.initMocks(this);

        when(event.getData()).thenReturn(rdsWaitingToStopRequest);
        when(rdsWaitingToStopRequest.getResourceId()).thenReturn(SDX_ID);
        when(rdsWaitingToStopRequest.getUserId()).thenReturn(USER_ID);
        when(sdxClusterRepository.findById(SDX_ID)).thenReturn(Optional.of(sdxCluster));
    }

    @Test
    public void shouldNotCallStopInCaseNoExternalDatabaseButSetStopedStatus() {
        when(databasePauseSupportService.isDatabasePauseSupported(sdxCluster)).thenReturn(false);

        victim.accept(event);

        verify(eventBus).notify(eq(RdsStopSuccessEvent.class.getSimpleName()), any(Event.class));
        verifyZeroInteractions(databaseService);
        verify(sdxStatusService).setStatusForDatalakeAndNotify(eq(DatalakeStatusEnum.EXTERNAL_DATABASE_STOPPED), anyString(), eq(sdxCluster));
    }

    @Test
    public void shouldCallStopInCaseExistingExternalDatabaseAndSetStopedStatus() {
        when(databasePauseSupportService.isDatabasePauseSupported(sdxCluster)).thenReturn(true);

        victim.accept(event);

        verify(eventBus).notify(eq(RdsStopSuccessEvent.class.getSimpleName()), any(Event.class));
        verify(databaseService).stop(sdxCluster);
        verify(sdxStatusService).setStatusForDatalakeAndNotify(eq(DatalakeStatusEnum.EXTERNAL_DATABASE_STOP_IN_PROGRESS), anyString(), eq(sdxCluster));
        verify(sdxStatusService).setStatusForDatalakeAndNotify(eq(DatalakeStatusEnum.EXTERNAL_DATABASE_STOPPED), anyString(), eq(sdxCluster));
    }

    @Test
    public void shouldHandleUserBreakExceptionWithSdxStopFailedEvent() {
        when(databasePauseSupportService.isDatabasePauseSupported(sdxCluster)).thenReturn(true);
        doThrow(UserBreakException.class).when(databaseService).stop(sdxCluster);

        victim.accept(event);

        verify(eventBus).notify(eq(SdxStopFailedEvent.class.getSimpleName()), any(Event.class));
    }

    @Test
    public void shouldHandlePollerStoppedExceptionWithSdxStopFailedEvent() {
        when(databasePauseSupportService.isDatabasePauseSupported(sdxCluster)).thenReturn(true);
        doThrow(PollerStoppedException.class).when(databaseService).stop(sdxCluster);

        victim.accept(event);

        verify(eventBus).notify(eq(SdxStopFailedEvent.class.getSimpleName()), any(Event.class));
    }

    @Test
    public void shouldHandlePollerExceptionWithSdxStopFailedEvent() {
        when(databasePauseSupportService.isDatabasePauseSupported(sdxCluster)).thenReturn(true);
        doThrow(PollerException.class).when(databaseService).stop(sdxCluster);

        victim.accept(event);

        verify(eventBus).notify(eq(SdxStopFailedEvent.class.getSimpleName()), any(Event.class));
    }

    @Test
    public void shouldHandleExceptionWithSdxStopFailedEvent() {
        when(databasePauseSupportService.isDatabasePauseSupported(sdxCluster)).thenReturn(true);
        doThrow(RuntimeException.class).when(databaseService).stop(sdxCluster);

        victim.accept(event);

        verify(eventBus).notify(eq(SdxStopFailedEvent.class.getSimpleName()), any(Event.class));
    }
}