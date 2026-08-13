package com.sequenceiq.datalake.flow.encryptionprofile;

import static com.sequenceiq.datalake.entity.DatalakeStatusEnum.DATALAKE_ENABLE_ENCRYPTION_PROFILE_FAILED;
import static com.sequenceiq.datalake.entity.DatalakeStatusEnum.DATALAKE_ENABLE_ENCRYPTION_PROFILE_IN_PROGRESS;
import static com.sequenceiq.datalake.entity.DatalakeStatusEnum.RUNNING;
import static com.sequenceiq.datalake.flow.encryptionprofile.SdxEnableEncryptionProfileActions.ENCRYPTION_PROFILE_CRN_VARIABLE;
import static com.sequenceiq.datalake.flow.encryptionprofile.SdxEnableEncryptionProfileEvent.SDX_ENABLE_ENCRYPTION_PROFILE_FAIL_HANDLED_EVENT;
import static com.sequenceiq.datalake.flow.encryptionprofile.SdxEnableEncryptionProfileEvent.SDX_ENABLE_ENCRYPTION_PROFILE_FINALIZED_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
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
import java.util.concurrent.ConcurrentHashMap;

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
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.datalake.flow.SdxEvent;
import com.sequenceiq.datalake.flow.encryptionprofile.event.SdxEnableEncryptionProfileFailedEvent;
import com.sequenceiq.datalake.flow.encryptionprofile.event.SdxEnableEncryptionProfileHandlerEvent;
import com.sequenceiq.datalake.flow.encryptionprofile.event.SdxEnableEncryptionProfileTriggerEvent;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.flow.core.AbstractActionTestSupport;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowRegister;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;

@ExtendWith(MockitoExtension.class)
class SdxEnableEncryptionProfileActionsTest {

    private static final Long SDX_ID = 1L;

    private static final String USER_ID = "userId";

    private static final String ENCRYPTION_PROFILE_CRN = "crn:cdp:environments:us-west-1:1234:encryptionProfile:ep";

    private Map<Object, Object> variables;

    @Mock
    private SdxStatusService sdxStatusService;

    @Mock
    private SdxService sdxService;

    @InjectMocks
    private SdxEnableEncryptionProfileActions underTest;

    @Captor
    private ArgumentCaptor<Event<SdxEnableEncryptionProfileHandlerEvent>> eventCaptor;

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
    void testEnableEncryptionProfileAction() throws Exception {
        SdxEnableEncryptionProfileTriggerEvent event =
                SdxEnableEncryptionProfileTriggerEvent.from(SDX_ID, USER_ID, ENCRYPTION_PROFILE_CRN);
        doCallRealMethod().when(reactorEventFactory).createEvent(any(), any());
        AbstractSdxEnableEncryptionProfileAction<SdxEnableEncryptionProfileTriggerEvent> action =
                (AbstractSdxEnableEncryptionProfileAction<SdxEnableEncryptionProfileTriggerEvent>) underTest.enableEncryptionProfileAction();
        initActionPrivateFields(action);

        new AbstractActionTestSupport<>(action).doExecute(context, event, variables);

        verify(sdxStatusService).setStatusForDatalakeAndNotify(eq(DATALAKE_ENABLE_ENCRYPTION_PROFILE_IN_PROGRESS),
                eq("Enable Encryption Profile is in progress"), eq(SDX_ID));
        assertEquals(ENCRYPTION_PROFILE_CRN, variables.get(ENCRYPTION_PROFILE_CRN_VARIABLE));
        ArgumentCaptor<String> selectorCaptor = ArgumentCaptor.forClass(String.class);
        verify(eventBus).notify(selectorCaptor.capture(), eventCaptor.capture());
        assertEquals(EventSelectorUtil.selector(SdxEnableEncryptionProfileHandlerEvent.class), selectorCaptor.getValue());
        SdxEnableEncryptionProfileHandlerEvent capturedEvent = eventCaptor.getValue().getData();
        assertEquals(SDX_ID, capturedEvent.getResourceId());
        assertEquals(USER_ID, capturedEvent.getUserId());
        assertEquals(ENCRYPTION_PROFILE_CRN, capturedEvent.getEncryptionProfileCrn());
    }

    @Test
    void testEnableEncryptionProfileActionWithNullCrnDoesNotStoreVariableAndForwardsNull() throws Exception {
        SdxEnableEncryptionProfileTriggerEvent event =
                SdxEnableEncryptionProfileTriggerEvent.from(SDX_ID, USER_ID, null);
        doCallRealMethod().when(reactorEventFactory).createEvent(any(), any());
        AbstractSdxEnableEncryptionProfileAction<SdxEnableEncryptionProfileTriggerEvent> action =
                (AbstractSdxEnableEncryptionProfileAction<SdxEnableEncryptionProfileTriggerEvent>) underTest.enableEncryptionProfileAction();
        initActionPrivateFields(action);
        Map<Object, Object> concurrentVariables = new ConcurrentHashMap<>();

        new AbstractActionTestSupport<>(action).doExecute(context, event, concurrentVariables);

        assertFalse(concurrentVariables.containsKey(ENCRYPTION_PROFILE_CRN_VARIABLE));
        ArgumentCaptor<String> selectorCaptor = ArgumentCaptor.forClass(String.class);
        verify(eventBus).notify(selectorCaptor.capture(), eventCaptor.capture());
        SdxEnableEncryptionProfileHandlerEvent capturedEvent = eventCaptor.getValue().getData();
        assertNull(capturedEvent.getEncryptionProfileCrn());
    }

    @Test
    void testFinishedAction() throws Exception {
        SdxEvent event = new SdxEvent(SDX_ID, USER_ID);
        doReturn(new Event<>(new Event.Headers(new HashMap<>()), event)).when(reactorEventFactory).createEvent(any(), any());
        SdxCluster sdxCluster = mock(SdxCluster.class);
        when(sdxService.getById(SDX_ID)).thenReturn(sdxCluster);
        AbstractSdxEnableEncryptionProfileAction<SdxEvent> action =
                (AbstractSdxEnableEncryptionProfileAction<SdxEvent>) underTest.finishedAction();
        initActionPrivateFields(action);

        new AbstractActionTestSupport<>(action).doExecute(context, event, variables);

        verify(sdxStatusService).setStatusForDatalakeAndNotify(eq(RUNNING), eq(ResourceEvent.DATALAKE_ENABLE_ENCRYPTION_PROFILE_FINISHED),
                eq("Enable Encryption Profile completed successfully"), eq(sdxCluster));
        ArgumentCaptor<String> selectorCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Event<SdxEvent>> finishedEventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventBus).notify(selectorCaptor.capture(), finishedEventCaptor.capture());
        assertEquals(SDX_ENABLE_ENCRYPTION_PROFILE_FINALIZED_EVENT.event(), selectorCaptor.getValue());
    }

    @Test
    void testFailedAction() throws Exception {
        SdxEnableEncryptionProfileFailedEvent event =
                new SdxEnableEncryptionProfileFailedEvent(SDX_ID, USER_ID, new RuntimeException("boom"));
        doCallRealMethod().when(reactorEventFactory).createEvent(any(), any());
        SdxCluster sdxCluster = mock(SdxCluster.class);
        when(sdxCluster.getName()).thenReturn("my-datalake");
        when(sdxService.getById(SDX_ID)).thenReturn(sdxCluster);
        AbstractSdxEnableEncryptionProfileAction<SdxEnableEncryptionProfileFailedEvent> action =
                (AbstractSdxEnableEncryptionProfileAction<SdxEnableEncryptionProfileFailedEvent>) underTest.failedAction();
        initActionPrivateFields(action);

        new AbstractActionTestSupport<>(action).doExecute(context, event, variables);

        verify(sdxStatusService).setStatusForDatalakeAndNotify(eq(DATALAKE_ENABLE_ENCRYPTION_PROFILE_FAILED),
                eq(List.of("my-datalake")), eq("Enable Encryption Profile failed"), eq(sdxCluster));
        verify(sdxStatusService).setStatusForDatalake(eq(RUNNING), any(String.class), eq(sdxCluster));
        ArgumentCaptor<String> selectorCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Event> failedEventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventBus).notify(selectorCaptor.capture(), failedEventCaptor.capture());
        assertEquals(SDX_ENABLE_ENCRYPTION_PROFILE_FAIL_HANDLED_EVENT.event(), selectorCaptor.getValue());
        SdxEnableEncryptionProfileFailedEvent capturedEvent = (SdxEnableEncryptionProfileFailedEvent) failedEventCaptor.getValue().getData();
        assertEquals(SDX_ID, capturedEvent.getResourceId());
        assertEquals("boom", capturedEvent.getException().getMessage());
    }

    private void initActionPrivateFields(Action<?, ?> action) {
        ReflectionTestUtils.setField(action, null, runningFlows, FlowRegister.class);
        ReflectionTestUtils.setField(action, null, eventBus, EventBus.class);
        ReflectionTestUtils.setField(action, null, reactorEventFactory, ErrorHandlerAwareReactorEventFactory.class);
    }
}
