package com.sequenceiq.datalake.flow.start.handler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import com.dyngr.exception.PollerException;
import com.dyngr.exception.PollerStoppedException;
import com.dyngr.exception.UserBreakException;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.start.event.RdsStartSuccessEvent;
import com.sequenceiq.datalake.flow.start.event.RdsWaitingToStartRequest;
import com.sequenceiq.datalake.flow.start.event.SdxStartFailedEvent;
import com.sequenceiq.datalake.repository.SdxClusterRepository;
import com.sequenceiq.datalake.service.pause.DatabasePauseSupportService;
import com.sequenceiq.datalake.service.sdx.database.DatabaseService;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;

@ExtendWith(MockitoExtension.class)
public class RdsStartHandlerTest {

    private static final Long SDX_ID = 123L;

    private static final String USER_ID = "user";

    @Mock
    private SdxClusterRepository sdxClusterRepository;

    @Mock
    private DatabaseService databaseService;

    @Mock
    private SdxStatusService sdxStatusService;

    @Mock
    private Event<RdsWaitingToStartRequest> event;

    @Mock
    private RdsWaitingToStartRequest rdsWaitingToStartRequest;

    @Mock
    private SdxCluster sdxCluster;

    @Mock
    private EventBus eventBus;

    @Mock
    private DatabasePauseSupportService databasePauseSupportService;

    @InjectMocks
    private RdsStartHandler victim;

    @Captor
    private ArgumentCaptor<Event<SdxStartFailedEvent>> sdxStartFailedEventCaptor;

    @BeforeEach
    public void initMocks() {
        MockitoAnnotations.initMocks(this);

        when(event.getData()).thenReturn(rdsWaitingToStartRequest);
        when(rdsWaitingToStartRequest.getResourceId()).thenReturn(SDX_ID);
        when(rdsWaitingToStartRequest.getUserId()).thenReturn(USER_ID);
        when(sdxClusterRepository.findById(SDX_ID)).thenReturn(Optional.of(sdxCluster));
    }

    @Test
    public void shouldNotCallStartInCaseNoExternalDatabaseButSetStartedStatus() {
        when(databasePauseSupportService.isDatabasePauseSupported(sdxCluster)).thenReturn(false);

        victim.accept(event);

        verify(eventBus).notify(eq(RdsStartSuccessEvent.class.getSimpleName()), any(Event.class));
        verifyNoInteractions(databaseService);
        verify(sdxStatusService).setStatusForDatalakeAndNotify(eq(DatalakeStatusEnum.EXTERNAL_DATABASE_STARTED), anyString(), eq(sdxCluster));
    }

    @Test
    public void shouldCallStartInCaseExistingExternalDatabaseAndSetStartedStatus() {
        when(databasePauseSupportService.isDatabasePauseSupported(sdxCluster)).thenReturn(true);

        victim.accept(event);

        verify(eventBus).notify(eq(RdsStartSuccessEvent.class.getSimpleName()), any(Event.class));
        verify(databaseService).start(sdxCluster);
        verify(sdxStatusService).setStatusForDatalakeAndNotify(eq(DatalakeStatusEnum.EXTERNAL_DATABASE_START_IN_PROGRESS), anyString(), eq(sdxCluster));
        verify(sdxStatusService).setStatusForDatalakeAndNotify(eq(DatalakeStatusEnum.EXTERNAL_DATABASE_STARTED), anyString(), eq(sdxCluster));
    }

    @Test
    public void shouldHandleUserBreakExceptionWithSdxStartFailedEvent() {
        when(databasePauseSupportService.isDatabasePauseSupported(sdxCluster)).thenReturn(true);
        UserBreakException userBreakException = new UserBreakException("userBreakException");
        doThrow(userBreakException).when(databaseService).start(sdxCluster);

        victim.accept(event);

        verifySdxStartFailedEvent(userBreakException, true);
    }

    @Test
    public void shouldHandlePollerStoppedExceptionWithSdxStartFailedEvent() {
        when(databasePauseSupportService.isDatabasePauseSupported(sdxCluster)).thenReturn(true);
        doThrow(PollerStoppedException.class).when(databaseService).start(sdxCluster);

        victim.accept(event);

        verifySdxStartFailedEvent(new PollerStoppedException("Database start timed out after 0 minutes"), true);
    }

    @Test
    public void shouldHandlePollerExceptionWithSdxStartFailedEvent() {
        when(databasePauseSupportService.isDatabasePauseSupported(sdxCluster)).thenReturn(true);
        PollerException pollerException = new PollerException("pollerException");
        doThrow(pollerException).when(databaseService).start(sdxCluster);

        victim.accept(event);

        verifySdxStartFailedEvent(pollerException, false);
    }

    @Test
    public void shouldHandleExceptionWithSdxStartFailedEvent() {
        when(databasePauseSupportService.isDatabasePauseSupported(sdxCluster)).thenReturn(true);
        RuntimeException runtimeException = new RuntimeException("");
        doThrow(runtimeException).when(databaseService).start(sdxCluster);

        victim.accept(event);

        verifySdxStartFailedEvent(runtimeException, false);
    }

    private void verifySdxStartFailedEvent(Exception ex, boolean includeExceptionDetails) {
        verify(eventBus).notify(eq(SdxStartFailedEvent.class.getSimpleName()), sdxStartFailedEventCaptor.capture());
        SdxStartFailedEvent event = sdxStartFailedEventCaptor.getValue().getData();
        Assertions.assertThat(event)
                .returns(SDX_ID, SdxStartFailedEvent::getResourceId)
                .returns(USER_ID, SdxStartFailedEvent::getUserId)
                .returns(includeExceptionDetails, SdxStartFailedEvent::isIncludeExceptionDetailsInNotification);
        // PollerStoppedException.equals() is not implemented so just check for class and message
        Assertions.assertThat(event.getException())
                .hasSameClassAs(ex)
                .hasMessage(ex.getMessage());
    }
}
