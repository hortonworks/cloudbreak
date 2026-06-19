package com.sequenceiq.datalake.flow.verticalscale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.statemachine.action.Action;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.flow.verticalscale.event.DatalakeVerticalScaleEvent;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.flow.core.CommonContext;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowRegister;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;

@ExtendWith(MockitoExtension.class)
class DatalakeVerticalScaleActionsTest {

    private static final String DATALAKE_CRN = "crn:cdp:datalake:us-west-1:123456789012:datalake:sample";

    private static final long RESOURCE_ID = 42L;

    private final Map<Object, Object> variables = new HashMap<>();

    @Mock
    private SdxStatusService sdxStatusService;

    @Mock
    private FlowRegister runningFlows;

    @Mock
    private EventBus eventBus;

    @Mock
    private ErrorHandlerAwareReactorEventFactory reactorEventFactory;

    @Mock
    private FlowParameters flowParameters;

    @Captor
    private ArgumentCaptor<DatalakeStatusEnum> statusCaptor;

    @Captor
    private ArgumentCaptor<Event<DatalakeVerticalScaleEvent>> eventCaptor;

    @Spy
    @InjectMocks
    private DatalakeVerticalScaleActions underTest;

    private CommonContext context;

    @BeforeEach
    void setUp() {
        context = new CommonContext(flowParameters);
    }

    @Test
    void testFinishedActionSetsRunningStatusWhenPreOperationStatusWasRunning() throws Exception {
        DatalakeVerticalScaleEvent event = createEvent();
        doReturn(new Event<>(new Event.Headers(new HashMap<>()), event)).when(reactorEventFactory).createEvent(any(), eq(event));
        variables.put("preOperationDlStatus", DatalakeStatusEnum.RUNNING);

        Action<?, ?> action = underTest.finishedAction();
        initActionPrivateFields(action);
        ReflectionTestUtils.invokeMethod(action, "doExecute", context, event, variables);

        verify(sdxStatusService, times(1)).setStatusForDatalakeAndNotifyWithStatusReason(statusCaptor.capture(), anyString(), eq(RESOURCE_ID));
        assertEquals(DatalakeStatusEnum.RUNNING, statusCaptor.getValue());
        verify(eventBus, times(1)).notify(anyString(), eventCaptor.capture());
        assertEquals(event, eventCaptor.getValue().getData());
    }

    @Test
    void testFinishedActionSetsStoppedStatusWhenPreOperationStatusWasStopped() throws Exception {
        DatalakeVerticalScaleEvent event = createEvent();
        doReturn(new Event<>(new Event.Headers(new HashMap<>()), event)).when(reactorEventFactory).createEvent(any(), eq(event));
        variables.put("preOperationDlStatus", DatalakeStatusEnum.STOPPED);

        Action<?, ?> action = underTest.finishedAction();
        initActionPrivateFields(action);
        ReflectionTestUtils.invokeMethod(action, "doExecute", context, event, variables);

        verify(sdxStatusService, times(1)).setStatusForDatalakeAndNotifyWithStatusReason(statusCaptor.capture(), anyString(), eq(RESOURCE_ID));
        assertEquals(DatalakeStatusEnum.STOPPED, statusCaptor.getValue());
        verify(eventBus, times(1)).notify(anyString(), eventCaptor.capture());
        assertEquals(event, eventCaptor.getValue().getData());
    }

    private DatalakeVerticalScaleEvent createEvent() {
        return DatalakeVerticalScaleEvent.builder()
                .withResourceCrn(DATALAKE_CRN)
                .withResourceId(RESOURCE_ID)
                .build();
    }

    private void initActionPrivateFields(Action<?, ?> action) {
        ReflectionTestUtils.setField(action, null, runningFlows, FlowRegister.class);
        ReflectionTestUtils.setField(action, null, eventBus, EventBus.class);
        ReflectionTestUtils.setField(action, null, reactorEventFactory, ErrorHandlerAwareReactorEventFactory.class);
    }
}
