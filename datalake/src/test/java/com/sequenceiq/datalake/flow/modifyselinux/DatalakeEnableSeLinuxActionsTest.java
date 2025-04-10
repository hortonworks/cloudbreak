package com.sequenceiq.datalake.flow.modifyselinux;

import static com.sequenceiq.datalake.entity.DatalakeStatusEnum.DATALAKE_MODIFY_SELINUX_ON_DATALAKE_FAILED;
import static com.sequenceiq.datalake.entity.DatalakeStatusEnum.RUNNING;
import static com.sequenceiq.datalake.flow.modifyselinux.event.DatalakeModifySeLinuxStateSelectors.FAILED_MODIFY_SELINUX_DATALAKE_EVENT;
import static com.sequenceiq.datalake.flow.modifyselinux.event.DatalakeModifySeLinuxStateSelectors.FINALIZE_MODIFY_SELINUX_DATALAKE_EVENT;
import static com.sequenceiq.datalake.flow.modifyselinux.event.DatalakeModifySeLinuxStateSelectors.FINISH_MODIFY_SELINUX_DATALAKE_EVENT;
import static com.sequenceiq.datalake.flow.modifyselinux.event.DatalakeModifySeLinuxStateSelectors.HANDLED_FAILED_MODIFY_SELINUX_DATALAKE_EVENT;
import static com.sequenceiq.datalake.flow.modifyselinux.event.DatalakeModifySeLinuxStateSelectors.MODIFY_SELINUX_DATALAKE_EVENT;
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
import com.sequenceiq.datalake.flow.modifyselinux.event.DatalakeModifySeLinuxEvent;
import com.sequenceiq.datalake.flow.modifyselinux.event.DatalakeModifySeLinuxFailedEvent;
import com.sequenceiq.datalake.flow.modifyselinux.event.DatalakeModifySeLinuxHandlerEvent;
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
    private ArgumentCaptor<Event<DatalakeModifySeLinuxHandlerEvent>> eventCaptor;

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
        DatalakeModifySeLinuxEvent event = DatalakeModifySeLinuxEvent.builder().withSelector(MODIFY_SELINUX_DATALAKE_EVENT.event())
                .withResourceId(1L).withResourceCrn("testCrn").withResourceName("test").build();
        doCallRealMethod().when(reactorEventFactory).createEvent(any(), any());
        AbstractDatalakeEnableSeLinuxAction<DatalakeModifySeLinuxEvent> action =
                (AbstractDatalakeEnableSeLinuxAction<DatalakeModifySeLinuxEvent>) underTest.enableSeLinuxInDatalakeAction();
        initActionPrivateFields(action);
        new AbstractActionTestSupport<>(action).doExecute(context, event, variables);
        ArgumentCaptor<String> selectorCaptor = ArgumentCaptor.forClass(String.class);
        verify(eventBus).notify(selectorCaptor.capture(), eventCaptor.capture());
        String selector = EventSelectorUtil.selector(DatalakeModifySeLinuxHandlerEvent.class);
        assertEquals(selector, selectorCaptor.getValue());
        DatalakeModifySeLinuxHandlerEvent capturedEvent = eventCaptor.getValue().getData();
        assertEquals(1L, capturedEvent.getResourceId());
        assertEquals("testCrn", capturedEvent.getResourceCrn());
        assertEquals("test", capturedEvent.getResourceName());
    }

    @Test
    void testFinishedAction() throws Exception {
        DatalakeModifySeLinuxEvent event = DatalakeModifySeLinuxEvent.builder().withSelector(FINISH_MODIFY_SELINUX_DATALAKE_EVENT.event())
                .withResourceId(1L).withResourceCrn("testCrn").withResourceName("test").build();
        doReturn(new Event<>(new Event.Headers(new HashMap<>()), event)).when(reactorEventFactory).createEvent(any(), any());
        SdxCluster sdxCluster = mock(SdxCluster.class);
        when(sdxStatusService.setStatusForDatalakeAndNotify(eq(RUNNING), eq("Data Lake SELinux set to 'ENFORCING' complete."), eq(1L)))
                .thenReturn(sdxCluster);
        AbstractDatalakeEnableSeLinuxAction<DatalakeModifySeLinuxEvent> action =
                (AbstractDatalakeEnableSeLinuxAction<DatalakeModifySeLinuxEvent>) underTest.finishedAction();
        initActionPrivateFields(action);
        new AbstractActionTestSupport<>(action).doExecute(context, event, variables);
        verify(sdxStatusService).setStatusForDatalakeAndNotify(eq(RUNNING), eq("Data Lake SELinux set to 'ENFORCING' complete."), eq(1L));
        ArgumentCaptor<String> selectorCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Event<DatalakeModifySeLinuxEvent>> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventBus).notify(selectorCaptor.capture(), eventCaptor.capture());
        verify(sdxMetricService).incrementMetricCounter(eq(SDX_ENABLE_SELINUX_FINISHED), eq(sdxCluster));
        DatalakeModifySeLinuxEvent capturedEvent = eventCaptor.getValue().getData();
        String selector = FINALIZE_MODIFY_SELINUX_DATALAKE_EVENT.event();
        assertEquals(selector, selectorCaptor.getValue());
        assertEquals(1L, capturedEvent.getResourceId());
        assertEquals("testCrn", capturedEvent.getResourceCrn());
        assertEquals("test", capturedEvent.getResourceName());
    }

    @Test
    void testFailedAction() throws Exception {
        DatalakeModifySeLinuxEvent failedEvent = DatalakeModifySeLinuxEvent.builder().withSelector(FAILED_MODIFY_SELINUX_DATALAKE_EVENT.event())
                .withResourceId(1L).withResourceCrn("testCrn").withResourceName("test").build();
        DatalakeModifySeLinuxFailedEvent event = new DatalakeModifySeLinuxFailedEvent(failedEvent,
                new CloudbreakServiceException("test"), DatalakeStatusEnum.DATALAKE_MODIFY_SELINUX_FAILED);
        doCallRealMethod().when(reactorEventFactory).createEvent(any(), any());
        SdxCluster sdxCluster = mock(SdxCluster.class);
        when(sdxStatusService.setStatusForDatalakeAndNotifyWithStatusReason(eq(DATALAKE_MODIFY_SELINUX_ON_DATALAKE_FAILED),
                eq("Enable SELinux failed on the Data Lake."), eq(1L))).thenReturn(sdxCluster);
        AbstractDatalakeEnableSeLinuxAction<DatalakeModifySeLinuxFailedEvent> action =
                (AbstractDatalakeEnableSeLinuxAction<DatalakeModifySeLinuxFailedEvent>) underTest.failedAction();
        initActionPrivateFields(action);
        new AbstractActionTestSupport<>(action).doExecute(context, event, variables);
        ArgumentCaptor<String> selectorCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventBus).notify(selectorCaptor.capture(), eventCaptor.capture());
        verify(sdxMetricService).incrementMetricCounter(eq(SDX_ENABLE_SELINUX_FAILED), eq(sdxCluster));
        String selector = HANDLED_FAILED_MODIFY_SELINUX_DATALAKE_EVENT.event();
        DatalakeModifySeLinuxFailedEvent capturedEvent = (DatalakeModifySeLinuxFailedEvent) eventCaptor.getValue().getData();
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
