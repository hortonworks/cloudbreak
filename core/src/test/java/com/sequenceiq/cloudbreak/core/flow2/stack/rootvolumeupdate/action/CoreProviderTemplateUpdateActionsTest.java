package com.sequenceiq.cloudbreak.core.flow2.stack.rootvolumeupdate.action;

import static com.sequenceiq.cloudbreak.core.flow2.stack.rootvolumeupdate.CoreProviderTemplateUpdateFlowEvent.CORE_PROVIDER_TEMPLATE_UPDATE_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.rootvolumeupdate.CoreProviderTemplateUpdateFlowEvent.CORE_PROVIDER_TEMPLATE_UPDATE_FAIL_HANDLED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.rootvolumeupdate.CoreProviderTemplateUpdateFlowEvent.CORE_PROVIDER_TEMPLATE_UPDATE_FINALIZED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.rootvolumeupdate.CoreProviderTemplateUpdateFlowEvent.CORE_PROVIDER_TEMPLATE_UPDATE_FINISHED_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.DiskType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.DiskUpdateRequest;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackContext;
import com.sequenceiq.cloudbreak.core.flow2.stack.rootvolumeupdate.event.CoreProviderTemplateUpdateEvent;
import com.sequenceiq.cloudbreak.core.flow2.stack.rootvolumeupdate.event.CoreProviderTemplateUpdateFailureEvent;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.flow.core.AbstractActionTestSupport;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowRegister;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;

@ExtendWith(MockitoExtension.class)
public class CoreProviderTemplateUpdateActionsTest {

    private Map<Object, Object> variables;

    @Mock
    private CloudbreakFlowMessageService flowMessageService;

    @Mock
    private StackUpdater stackUpdater;

    @InjectMocks
    private CoreProviderTemplateUpdateActions underTest;

    @Mock
    private FlowRegister runningFlows;

    @Mock
    private EventBus eventBus;

    @Mock
    private ErrorHandlerAwareReactorEventFactory reactorEventFactory;

    private StackContext context;

    @Mock
    private FlowParameters flowParameters;

    @Captor
    private ArgumentCaptor<String> captor;

    @Mock
    private CloudContext cloudContext;

    @Mock
    private CloudCredential cloudCredential;

    @Mock
    private CloudStack cloudStack;

    @BeforeEach
    void setUp() {
        variables = new HashMap<>();
        StackDto stackDto = mock(StackDto.class);
        when(stackDto.getId()).thenReturn(1L);
        context = new StackContext(flowParameters, stackDto, cloudContext, cloudCredential, cloudStack);
    }

    @Test
    void testLaunchTemplateUpdateAction() throws Exception {
        CoreProviderTemplateUpdateEvent launchTemplateUpdateEvent = new CoreProviderTemplateUpdateEvent(
                CORE_PROVIDER_TEMPLATE_UPDATE_EVENT.event(),
                1L,
                "gp2",
                100,
                "executor",
                DiskType.ADDITIONAL_DISK.name()
        );
        doReturn(new Event<>(new Event.Headers(new HashMap<>()), launchTemplateUpdateEvent)).when(reactorEventFactory).createEvent(any(), any());
        CoreAbstractProviderTemplateUpdateAction<CoreProviderTemplateUpdateEvent> action =
                (CoreAbstractProviderTemplateUpdateAction<CoreProviderTemplateUpdateEvent>) underTest.launchTemplateUpdateAction();
        initActionPrivateFields(action);
        new AbstractActionTestSupport<>(action).doExecute(context, launchTemplateUpdateEvent, variables);
        verify(stackUpdater).updateStackStatus(eq(1L), eq(DetailedStackStatus.PROVIDER_TEMPLATE_UPDATE_IN_PROGRESS), eq("Starting to update launch template."));
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventBus).notify(captor.capture(), eventCaptor.capture());
        assertEquals(CORE_PROVIDER_TEMPLATE_UPDATE_EVENT.event(), captor.getValue());
        assertEquals(1L, ReflectionTestUtils.getField(eventCaptor.getValue().getData(), "stackId"));
    }

    @Test
    void testLaunchTemplateUpdateFinishedAction() throws Exception {
        DiskUpdateRequest diskUpdateRequest = new DiskUpdateRequest();
        diskUpdateRequest.setDiskType(DiskType.ADDITIONAL_DISK);
        diskUpdateRequest.setGroup("executor");
        diskUpdateRequest.setVolumeType("gp2");
        diskUpdateRequest.setSize(100);
        CoreProviderTemplateUpdateEvent launchTemplateUpdateEvent =
                new CoreProviderTemplateUpdateEvent(
                        CORE_PROVIDER_TEMPLATE_UPDATE_FINISHED_EVENT.event(),
                        1L,
                        "gp2",
                        100,
                        "executor",
                        DiskType.ADDITIONAL_DISK.name()
                );
        doReturn(new Event<>(new Event.Headers(new HashMap<>()), launchTemplateUpdateEvent)).when(reactorEventFactory).createEvent(any(), any());
        CoreAbstractProviderTemplateUpdateAction<CoreProviderTemplateUpdateEvent> action =
                (CoreAbstractProviderTemplateUpdateAction<CoreProviderTemplateUpdateEvent>) underTest.launchTemplateUpdateFinishedAction();
        initActionPrivateFields(action);
        new AbstractActionTestSupport<>(action).doExecute(context, launchTemplateUpdateEvent, variables);
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(stackUpdater).updateStackStatus(eq(1L), eq(DetailedStackStatus.PROVIDER_TEMPLATE_UPDATE_COMPLETE), eq("Updating Launch Template complete."));
        verify(eventBus).notify(captor.capture(), eventCaptor.capture());
        assertEquals(CORE_PROVIDER_TEMPLATE_UPDATE_FINALIZED_EVENT.event(), captor.getValue());
        assertEquals(1L, ReflectionTestUtils.getField(eventCaptor.getValue().getData(), "stackId"));
    }

    @Test
    void testLaunchTemplateUpdateFailureAction() throws Exception {
        CoreProviderTemplateUpdateFailureEvent launchTemplateFailureEvent = new CoreProviderTemplateUpdateFailureEvent(1L, "test", new Exception("test"));
        doReturn(new Event<>(new Event.Headers(new HashMap<>()), launchTemplateFailureEvent)).when(reactorEventFactory).createEvent(any(), any());
        CoreAbstractProviderTemplateUpdateAction<CoreProviderTemplateUpdateFailureEvent> action =
                (CoreAbstractProviderTemplateUpdateAction<CoreProviderTemplateUpdateFailureEvent>) underTest.failureAction();
        initActionPrivateFields(action);
        new AbstractActionTestSupport<>(action).doExecute(context, launchTemplateFailureEvent, variables);
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(stackUpdater).updateStackStatus(eq(1L), eq(DetailedStackStatus.PROVIDER_TEMPLATE_UPDATE_FAILED), eq("test"));
        verify(eventBus).notify(captor.capture(), eventCaptor.capture());
        assertEquals(CORE_PROVIDER_TEMPLATE_UPDATE_FAIL_HANDLED_EVENT.event(), captor.getValue());
        assertEquals(1L, ReflectionTestUtils.getField(eventCaptor.getValue().getData(), "stackId"));
    }

    private void initActionPrivateFields(Action<?, ?> action) {
        ReflectionTestUtils.setField(action, null, runningFlows, FlowRegister.class);
        ReflectionTestUtils.setField(action, null, eventBus, EventBus.class);
        ReflectionTestUtils.setField(action, null, reactorEventFactory, ErrorHandlerAwareReactorEventFactory.class);
    }
}
