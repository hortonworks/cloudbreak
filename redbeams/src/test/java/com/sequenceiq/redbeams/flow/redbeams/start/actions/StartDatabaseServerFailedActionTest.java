package com.sequenceiq.redbeams.flow.redbeams.start.actions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowRegister;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;
import com.sequenceiq.redbeams.api.model.common.DetailedDBStackStatus;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsContext;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsEvent;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsFailureEvent;
import com.sequenceiq.redbeams.flow.redbeams.start.RedbeamsStartEvent;
import com.sequenceiq.redbeams.metrics.MetricType;
import com.sequenceiq.redbeams.metrics.RedbeamsMetricService;
import com.sequenceiq.redbeams.service.stack.DBStackStatusUpdater;

@ExtendWith(MockitoExtension.class)
public class StartDatabaseServerFailedActionTest {

    private static final Long RESOURCE_ID = 123L;

    @Mock
    private DBStackStatusUpdater dbStackStatusUpdater;

    @Mock
    private RedbeamsMetricService metricService;

    @Mock
    private DBStack dbStack;

    @Mock
    private Exception exception;

    @Mock
    private FlowParameters flowParameters;

    @Mock
    private CloudCredential cloudCredential;

    @Mock
    private DatabaseStack databaseStack;

    @Mock
    private CloudContext cloudContext;

    @Mock
    private FlowRegister flowRegister;

    @Mock
    private EventBus eventBus;

    @Mock
    private ErrorHandlerAwareReactorEventFactory reactorEventFactory;

    @InjectMocks
    private StartDatabaseServerFailedAction victim;

    @Test
    public void shouldUpdateStatusAndIncrementMetricOnPrepare() {
        RedbeamsFailureEvent event = new RedbeamsFailureEvent(RESOURCE_ID, exception);
        Optional<DBStack> dbStackOptional = Optional.of(dbStack);
        when(dbStackStatusUpdater.updateStatus(RESOURCE_ID, DetailedDBStackStatus.START_FAILED, null)).thenReturn(dbStackOptional);

        victim.prepareExecution(event, null);

        verify(metricService).incrementMetricCounter(MetricType.DB_START_FAILED, dbStackOptional);
    }

    @Test
    public void shouldUpdateStatusWithUknownErrorAndIncrementMetricOnPrepare() {
        RedbeamsFailureEvent event = new RedbeamsFailureEvent(RESOURCE_ID, null);
        Optional<DBStack> dbStackOptional = Optional.of(dbStack);
        when(dbStackStatusUpdater.updateStatus(RESOURCE_ID, DetailedDBStackStatus.START_FAILED, "Unknown error")).thenReturn(dbStackOptional);

        victim.prepareExecution(event, null);

        verify(metricService).incrementMetricCounter(MetricType.DB_START_FAILED, dbStackOptional);
    }

    @Test
    public void shouldCreateRequest() throws Exception {
        RedbeamsContext context = new RedbeamsContext(flowParameters, cloudContext, cloudCredential, databaseStack, dbStack);
        RedbeamsFailureEvent event = new RedbeamsFailureEvent(RESOURCE_ID, null);

        victim.doExecute(context, event, null);

        ArgumentCaptor<Object> payloadCapture = ArgumentCaptor.forClass(Object.class);
        verify(reactorEventFactory).createEvent(anyMap(), payloadCapture.capture());
        RedbeamsEvent request = (RedbeamsEvent) payloadCapture.getValue();
        assertEquals(RedbeamsStartEvent.REDBEAMS_START_FAILURE_HANDLED_EVENT.event(), request.selector());
    }
}