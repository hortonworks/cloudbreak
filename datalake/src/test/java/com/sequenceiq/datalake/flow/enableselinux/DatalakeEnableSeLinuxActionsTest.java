package com.sequenceiq.datalake.flow.enableselinux;

import static com.sequenceiq.datalake.entity.DatalakeStatusEnum.DATALAKE_ENABLE_SELINUX_ON_DATALAKE_FAILED;
import static com.sequenceiq.datalake.entity.DatalakeStatusEnum.RUNNING;
import static com.sequenceiq.datalake.flow.enableselinux.event.DatalakeEnableSeLinuxStateSelectors.ENABLE_SELINUX_DATALAKE_EVENT;
import static com.sequenceiq.datalake.flow.enableselinux.event.DatalakeEnableSeLinuxStateSelectors.FAILED_ENABLE_SELINUX_DATALAKE_EVENT;
import static com.sequenceiq.datalake.flow.enableselinux.event.DatalakeEnableSeLinuxStateSelectors.FINALIZE_ENABLE_SELINUX_DATALAKE_EVENT;
import static com.sequenceiq.datalake.flow.enableselinux.event.DatalakeEnableSeLinuxStateSelectors.FINISH_ENABLE_SELINUX_DATALAKE_EVENT;
import static com.sequenceiq.datalake.flow.enableselinux.event.DatalakeEnableSeLinuxStateSelectors.HANDLED_FAILED_ENABLE_SELINUX_DATALAKE_EVENT;
import static com.sequenceiq.datalake.metric.MetricType.SDX_ENABLE_SELINUX_FAILED;
import static com.sequenceiq.datalake.metric.MetricType.SDX_ENABLE_SELINUX_FINISHED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.statemachine.action.Action;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.enableselinux.event.DatalakeEnableSeLinuxEvent;
import com.sequenceiq.datalake.flow.enableselinux.event.DatalakeEnableSeLinuxFailedEvent;
import com.sequenceiq.datalake.flow.enableselinux.event.DatalakeEnableSeLinuxHandlerEvent;
import com.sequenceiq.datalake.metric.SdxMetricService;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.flow.core.AbstractActionTestSupport;
import com.sequenceiq.flow.core.CommonContext;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowRegister;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;

@ExtendWith(MockitoExtension.class)
class DatalakeEnableSeLinuxActionsTest {

    private Map<Object, Object> variables;

    @Mock
    private SdxStatusService sdxStatusService;

    @Mock
    private SdxMetricService sdxMetricService;

    @InjectMocks
    private DatalakeEnableSeLinuxActions underTest;

    @Captor
    private ArgumentCaptor<DatalakeStatusEnum> statusCaptor;

    @Captor
    private ArgumentCaptor<Event<DatalakeEnableSeLinuxHandlerEvent>> eventCaptor;

    @Mock
    private FlowRegister runningFlows;

    @Mock
    private EventBus eventBus;

    @Mock
    private ErrorHandlerAwareReactorEventFactory reactorEventFactory;

    private CommonContext context;

    @Mock
    private FlowParameters flowParameters;

    @BeforeEach
    void setUp() {
        variables  = new HashMap<>();
        context = new CommonContext(flowParameters);
    }

    @Test
    void testEnableSeLinuxInDatalakeAction() throws Exception {
        DatalakeEnableSeLinuxEvent event = DatalakeEnableSeLinuxEvent.builder().withSelector(ENABLE_SELINUX_DATALAKE_EVENT.event())
                .withResourceId(1L).withResourceCrn("testCrn").withResourceName("test").build();
        doCallRealMethod().when(reactorEventFactory).createEvent(any(), any());
        AbstractDatalakeEnableSeLinuxAction<DatalakeEnableSeLinuxEvent> action =
                (AbstractDatalakeEnableSeLinuxAction<DatalakeEnableSeLinuxEvent>) underTest.enableSeLinuxInDatalakeAction();
        initActionPrivateFields(action);
        new AbstractActionTestSupport<>(action).doExecute(context, event, variables);
        ArgumentCaptor<String> selectorCaptor = ArgumentCaptor.forClass(String.class);
        verify(eventBus).notify(selectorCaptor.capture(), eventCaptor.capture());
        String selector = EventSelectorUtil.selector(DatalakeEnableSeLinuxHandlerEvent.class);
        assertEquals(selector, selectorCaptor.getValue());
        DatalakeEnableSeLinuxHandlerEvent capturedEvent = eventCaptor.getValue().getData();
        assertEquals(1L, capturedEvent.getResourceId());
        assertEquals("testCrn", capturedEvent.getResourceCrn());
        assertEquals("test", capturedEvent.getResourceName());
    }

    @Test
    void testFinishedAction() throws Exception {
        DatalakeEnableSeLinuxEvent event = DatalakeEnableSeLinuxEvent.builder().withSelector(FINISH_ENABLE_SELINUX_DATALAKE_EVENT.event())
                .withResourceId(1L).withResourceCrn("testCrn").withResourceName("test").build();
        doReturn(new Event<>(new Event.Headers(new HashMap<>()), event)).when(reactorEventFactory).createEvent(any(), any());
        SdxCluster sdxCluster = mock(SdxCluster.class);
        when(sdxStatusService.setStatusForDatalakeAndNotify(eq(RUNNING), eq("Data Lake SELinux set to 'ENFORCING' complete."), eq(1L)))
                .thenReturn(sdxCluster);
        AbstractDatalakeEnableSeLinuxAction<DatalakeEnableSeLinuxEvent> action =
                (AbstractDatalakeEnableSeLinuxAction<DatalakeEnableSeLinuxEvent>) underTest.finishedAction();
        initActionPrivateFields(action);
        new AbstractActionTestSupport<>(action).doExecute(context, event, variables);
        verify(sdxStatusService).setStatusForDatalakeAndNotify(eq(RUNNING), eq("Data Lake SELinux set to 'ENFORCING' complete."), eq(1L));
        ArgumentCaptor<String> selectorCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Event<DatalakeEnableSeLinuxEvent>> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventBus).notify(selectorCaptor.capture(), eventCaptor.capture());
        verify(sdxMetricService).incrementMetricCounter(eq(SDX_ENABLE_SELINUX_FINISHED), eq(sdxCluster));
        DatalakeEnableSeLinuxEvent capturedEvent = eventCaptor.getValue().getData();
        String selector = FINALIZE_ENABLE_SELINUX_DATALAKE_EVENT.event();
        assertEquals(selector, selectorCaptor.getValue());
        assertEquals(1L, capturedEvent.getResourceId());
        assertEquals("testCrn", capturedEvent.getResourceCrn());
        assertEquals("test", capturedEvent.getResourceName());
    }

    @Test
    void testFailedAction() throws Exception {
        DatalakeEnableSeLinuxEvent failedEvent = DatalakeEnableSeLinuxEvent.builder().withSelector(FAILED_ENABLE_SELINUX_DATALAKE_EVENT.event())
                .withResourceId(1L).withResourceCrn("testCrn").withResourceName("test").build();
        DatalakeEnableSeLinuxFailedEvent event = new DatalakeEnableSeLinuxFailedEvent(failedEvent,
                new CloudbreakServiceException("test"), DatalakeStatusEnum.DATALAKE_ENABLE_SELINUX_FAILED);
        doCallRealMethod().when(reactorEventFactory).createEvent(any(), any());
        SdxCluster sdxCluster = mock(SdxCluster.class);
        when(sdxStatusService.setStatusForDatalakeAndNotifyWithStatusReason(eq(DATALAKE_ENABLE_SELINUX_ON_DATALAKE_FAILED),
                eq("Enable SELinux failed on the Data Lake."), eq(1L))).thenReturn(sdxCluster);
        AbstractDatalakeEnableSeLinuxAction<DatalakeEnableSeLinuxFailedEvent> action =
                (AbstractDatalakeEnableSeLinuxAction<DatalakeEnableSeLinuxFailedEvent>) underTest.failedAction();
        initActionPrivateFields(action);
        new AbstractActionTestSupport<>(action).doExecute(context, event, variables);
        ArgumentCaptor<String> selectorCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventBus).notify(selectorCaptor.capture(), eventCaptor.capture());
        verify(sdxMetricService).incrementMetricCounter(eq(SDX_ENABLE_SELINUX_FAILED), eq(sdxCluster));
        String selector = HANDLED_FAILED_ENABLE_SELINUX_DATALAKE_EVENT.event();
        DatalakeEnableSeLinuxFailedEvent capturedEvent = (DatalakeEnableSeLinuxFailedEvent) eventCaptor.getValue().getData();
        assertEquals(selector, selectorCaptor.getValue());
        assertEquals(selector, capturedEvent.getSelector());
        assertEquals(1L, capturedEvent.getResourceId());
        assertEquals("testCrn", capturedEvent.getResourceCrn());
        assertEquals("test", capturedEvent.getResourceName());
    }

    private void initActionPrivateFields(Action<?, ?> action) {
        ReflectionTestUtils.setField(action, null, runningFlows, FlowRegister.class);
        ReflectionTestUtils.setField(action, null, eventBus, EventBus.class);
        ReflectionTestUtils.setField(action, null, reactorEventFactory, ErrorHandlerAwareReactorEventFactory.class);
    }
}
