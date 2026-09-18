package com.sequenceiq.datalake.flow.encryptionprofile;

import static com.sequenceiq.datalake.entity.DatalakeStatusEnum.DATALAKE_DISABLE_ENCRYPTION_PROFILE_FAILED;
import static com.sequenceiq.datalake.entity.DatalakeStatusEnum.DATALAKE_DISABLE_ENCRYPTION_PROFILE_IN_PROGRESS;
import static com.sequenceiq.datalake.entity.DatalakeStatusEnum.RUNNING;
import static com.sequenceiq.datalake.flow.encryptionprofile.SdxDisableEncryptionProfileEvent.SDX_DISABLE_ENCRYPTION_PROFILE_FAIL_HANDLED_EVENT;
import static com.sequenceiq.datalake.flow.encryptionprofile.SdxDisableEncryptionProfileEvent.SDX_DISABLE_ENCRYPTION_PROFILE_FINALIZED_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
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

import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.eventbus.ErrorHandlerAwareReactorEventFactory;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.datalake.flow.SdxEvent;
import com.sequenceiq.datalake.flow.encryptionprofile.event.SdxDisableEncryptionProfileFailedEvent;
import com.sequenceiq.datalake.flow.encryptionprofile.event.SdxDisableEncryptionProfileHandlerEvent;
import com.sequenceiq.datalake.flow.encryptionprofile.event.SdxDisableEncryptionProfileTriggerEvent;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.flow.core.AbstractActionTestSupport;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowRegister;
import com.sequenceiq.flow.event.EventSelectorUtil;

@ExtendWith(MockitoExtension.class)
class SdxDisableEncryptionProfileActionsTest {

    private static final Long SDX_ID = 1L;

    private static final String USER_ID = "userId";

    private Map<Object, Object> variables;

    @Mock
    private SdxStatusService sdxStatusService;

    @Mock
    private SdxService sdxService;

    @InjectMocks
    private SdxDisableEncryptionProfileActions underTest;

    @Captor
    private ArgumentCaptor<Event<SdxDisableEncryptionProfileHandlerEvent>> eventCaptor;

    @Mock
    private FlowRegister runningFlows;

    @Mock
    private EventBus eventBus;

    @Mock
    private ErrorHandlerAwareReactorEventFactory reactorEventFactory;

    @Mock
    private FlowParameters flowParameters;

    private SdxContext context;

    @BeforeEach
    void setUp() {
        variables = new HashMap<>();
        context = new SdxContext(flowParameters, SDX_ID, USER_ID);
    }

    @Test
    void testDisableEncryptionProfileAction() throws Exception {
        SdxDisableEncryptionProfileTriggerEvent event = SdxDisableEncryptionProfileTriggerEvent.from(SDX_ID, USER_ID);
        doCallRealMethod().when(reactorEventFactory).createEvent(any(), any());
        AbstractSdxDisableEncryptionProfileAction<SdxDisableEncryptionProfileTriggerEvent> action =
                (AbstractSdxDisableEncryptionProfileAction<SdxDisableEncryptionProfileTriggerEvent>) underTest.disableEncryptionProfileAction();
        initActionPrivateFields(action);

        new AbstractActionTestSupport<>(action).doExecute(context, event, variables);

        verify(sdxStatusService).setStatusForDatalakeAndNotify(eq(DATALAKE_DISABLE_ENCRYPTION_PROFILE_IN_PROGRESS),
                eq("Disable Encryption Profile is in progress"), eq(SDX_ID));
        ArgumentCaptor<String> selectorCaptor = ArgumentCaptor.forClass(String.class);
        verify(eventBus).notify(selectorCaptor.capture(), eventCaptor.capture());
        assertEquals(EventSelectorUtil.selector(SdxDisableEncryptionProfileHandlerEvent.class), selectorCaptor.getValue());
        SdxDisableEncryptionProfileHandlerEvent capturedEvent = eventCaptor.getValue().getData();
        assertEquals(SDX_ID, capturedEvent.getResourceId());
        assertEquals(USER_ID, capturedEvent.getUserId());
    }

    @Test
    void testFinishedAction() throws Exception {
        SdxEvent event = new SdxEvent(SDX_ID, USER_ID);
        doReturn(new Event<>(new Event.Headers(new HashMap<>()), event)).when(reactorEventFactory).createEvent(any(), any());
        SdxCluster sdxCluster = mock(SdxCluster.class);
        when(sdxService.getById(SDX_ID)).thenReturn(sdxCluster);
        AbstractSdxDisableEncryptionProfileAction<SdxEvent> action =
                (AbstractSdxDisableEncryptionProfileAction<SdxEvent>) underTest.finishedAction();
        initActionPrivateFields(action);

        new AbstractActionTestSupport<>(action).doExecute(context, event, variables);

        verify(sdxStatusService).setStatusForDatalakeAndNotify(eq(RUNNING), eq(ResourceEvent.DATALAKE_DISABLE_ENCRYPTION_PROFILE_FINISHED),
                eq("Disable Encryption Profile completed successfully"), eq(sdxCluster));
        ArgumentCaptor<String> selectorCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Event<SdxEvent>> finishedEventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventBus).notify(selectorCaptor.capture(), finishedEventCaptor.capture());
        assertEquals(SDX_DISABLE_ENCRYPTION_PROFILE_FINALIZED_EVENT.event(), selectorCaptor.getValue());
    }

    @Test
    void testFailedAction() throws Exception {
        SdxDisableEncryptionProfileFailedEvent event =
                new SdxDisableEncryptionProfileFailedEvent(SDX_ID, USER_ID, new RuntimeException("boom"));
        doCallRealMethod().when(reactorEventFactory).createEvent(any(), any());
        SdxCluster sdxCluster = mock(SdxCluster.class);
        when(sdxCluster.getName()).thenReturn("my-datalake");
        when(sdxService.getById(SDX_ID)).thenReturn(sdxCluster);
        AbstractSdxDisableEncryptionProfileAction<SdxDisableEncryptionProfileFailedEvent> action =
                (AbstractSdxDisableEncryptionProfileAction<SdxDisableEncryptionProfileFailedEvent>) underTest.failedAction();
        initActionPrivateFields(action);

        new AbstractActionTestSupport<>(action).doExecute(context, event, variables);

        verify(sdxStatusService).setStatusForDatalakeAndNotify(eq(DATALAKE_DISABLE_ENCRYPTION_PROFILE_FAILED),
                eq(List.of("my-datalake", "boom")), eq("Disable Encryption Profile failed: boom"), eq(sdxCluster));
        verify(sdxStatusService).setStatusForDatalake(eq(RUNNING), any(String.class), eq(sdxCluster));
        ArgumentCaptor<String> selectorCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Event> failedEventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventBus).notify(selectorCaptor.capture(), failedEventCaptor.capture());
        assertEquals(SDX_DISABLE_ENCRYPTION_PROFILE_FAIL_HANDLED_EVENT.event(), selectorCaptor.getValue());
        SdxDisableEncryptionProfileFailedEvent capturedEvent = (SdxDisableEncryptionProfileFailedEvent) failedEventCaptor.getValue().getData();
        assertEquals(SDX_ID, capturedEvent.getResourceId());
        assertEquals("boom", capturedEvent.getException().getMessage());
    }

    @Test
    void testFailedActionWithoutExceptionMessage() throws Exception {
        SdxDisableEncryptionProfileFailedEvent event =
                new SdxDisableEncryptionProfileFailedEvent(SDX_ID, USER_ID, new RuntimeException());
        doCallRealMethod().when(reactorEventFactory).createEvent(any(), any());
        SdxCluster sdxCluster = mock(SdxCluster.class);
        when(sdxCluster.getName()).thenReturn("my-datalake");
        when(sdxService.getById(SDX_ID)).thenReturn(sdxCluster);
        AbstractSdxDisableEncryptionProfileAction<SdxDisableEncryptionProfileFailedEvent> action =
                (AbstractSdxDisableEncryptionProfileAction<SdxDisableEncryptionProfileFailedEvent>) underTest.failedAction();
        initActionPrivateFields(action);

        new AbstractActionTestSupport<>(action).doExecute(context, event, variables);

        verify(sdxStatusService).setStatusForDatalakeAndNotify(eq(DATALAKE_DISABLE_ENCRYPTION_PROFILE_FAILED),
                eq(List.of("my-datalake", "Unknown error")), eq("Disable Encryption Profile failed: Unknown error"), eq(sdxCluster));
        verify(sdxStatusService).setStatusForDatalake(eq(RUNNING), eq("Disable Encryption Profile failed: Unknown error"), eq(sdxCluster));
    }

    private void initActionPrivateFields(Action<?, ?> action) {
        ReflectionTestUtils.setField(action, null, runningFlows, FlowRegister.class);
        ReflectionTestUtils.setField(action, null, eventBus, EventBus.class);
        ReflectionTestUtils.setField(action, null, reactorEventFactory, ErrorHandlerAwareReactorEventFactory.class);
    }
}
