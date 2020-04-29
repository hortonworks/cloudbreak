package com.sequenceiq.datalake.flow.start.handler;

import com.dyngr.exception.PollerException;
import com.dyngr.exception.PollerStoppedException;
import com.dyngr.exception.UserBreakException;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.start.event.RdsStartSuccessEvent;
import com.sequenceiq.datalake.flow.start.event.RdsWaitingToStartRequest;
import com.sequenceiq.datalake.flow.start.event.SdxStartFailedEvent;
import com.sequenceiq.datalake.repository.SdxClusterRepository;
import com.sequenceiq.datalake.service.sdx.database.DatabaseService;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
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
public class RdsStartHandlerTest {

    private static final Long SDX_ID = 123L;

    private static final String USER_ID = "user";

    private static final String DATABASE_CRN = "db crn";

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

    @InjectMocks
    private RdsStartHandler victim;

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
        when(sdxCluster.hasExternalDatabase()).thenReturn(false);

        victim.accept(event);

        verify(eventBus).notify(eq(RdsStartSuccessEvent.class.getSimpleName()), any(Event.class));
        verifyZeroInteractions(databaseService);
        verify(sdxStatusService).setStatusForDatalakeAndNotify(eq(DatalakeStatusEnum.EXTERNAL_DATABASE_STARTED), anyString(), eq(sdxCluster));
    }

    @Test
    public void shouldCallStartInCaseExistingExternalDatabaseAndSetStartedStatus() {
        when(sdxCluster.hasExternalDatabase()).thenReturn(true);
        when(sdxCluster.getDatabaseCrn()).thenReturn(DATABASE_CRN);

        victim.accept(event);

        verify(eventBus).notify(eq(RdsStartSuccessEvent.class.getSimpleName()), any(Event.class));
        verify(databaseService).start(sdxCluster);
        verify(sdxStatusService).setStatusForDatalakeAndNotify(eq(DatalakeStatusEnum.EXTERNAL_DATABASE_START_IN_PROGRESS), anyString(), eq(sdxCluster));
        verify(sdxStatusService).setStatusForDatalakeAndNotify(eq(DatalakeStatusEnum.EXTERNAL_DATABASE_STARTED), anyString(), eq(sdxCluster));
    }

    @Test
    public void shouldHandleUserBreakExceptionWithSdxStartFailedEvent() {
        when(sdxCluster.hasExternalDatabase()).thenReturn(true);
        when(sdxCluster.getDatabaseCrn()).thenReturn(DATABASE_CRN);
        doThrow(UserBreakException.class).when(databaseService).start(sdxCluster);

        victim.accept(event);

        verify(eventBus).notify(eq(SdxStartFailedEvent.class.getSimpleName()), any(Event.class));
    }

    @Test
    public void shouldHandlePollerStoppedExceptionWithSdxStartFailedEvent() {
        when(sdxCluster.hasExternalDatabase()).thenReturn(true);
        when(sdxCluster.getDatabaseCrn()).thenReturn(DATABASE_CRN);
        doThrow(PollerStoppedException.class).when(databaseService).start(sdxCluster);

        victim.accept(event);

        verify(eventBus).notify(eq(SdxStartFailedEvent.class.getSimpleName()), any(Event.class));
    }

    @Test
    public void shouldHandlePollerExceptionWithSdxStartFailedEvent() {
        when(sdxCluster.hasExternalDatabase()).thenReturn(true);
        when(sdxCluster.getDatabaseCrn()).thenReturn(DATABASE_CRN);
        doThrow(PollerException.class).when(databaseService).start(sdxCluster);

        victim.accept(event);

        verify(eventBus).notify(eq(SdxStartFailedEvent.class.getSimpleName()), any(Event.class));
    }

    @Test
    public void shouldHandleExceptionWithSdxStartFailedEvent() {
        when(sdxCluster.hasExternalDatabase()).thenReturn(true);
        when(sdxCluster.getDatabaseCrn()).thenReturn(DATABASE_CRN);
        doThrow(RuntimeException.class).when(databaseService).start(sdxCluster);

        victim.accept(event);

        verify(eventBus).notify(eq(SdxStartFailedEvent.class.getSimpleName()), any(Event.class));
    }
}