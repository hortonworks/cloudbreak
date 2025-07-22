package com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.AddVolumesEvent.ADD_VOLUMES_CM_CONFIGURATION_HANDLER_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.AddVolumesEvent.ADD_VOLUMES_HANDLER_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.AddVolumesEvent.ADD_VOLUMES_ORCHESTRATION_HANDLER_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.AddVolumesEvent.ADD_VOLUMES_VALIDATE_HANDLER_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.AddVolumesEvent.ATTACH_VOLUMES_HANDLER_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.AddVolumesEvent.FINALIZED_EVENT;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_ADDING_VOLUMES;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_ATTACHING_VOLUMES;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_CM_CONFIG_CHANGE;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_MOUNTING_VOLUMES;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
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

import com.sequenceiq.cloudbreak.cloud.model.CloudVolumeUsageType;
import com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.actions.AbstractAddVolumesAction;
import com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.actions.AddVolumesActions;
import com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.event.AddVolumesCMConfigFinishedEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.event.AddVolumesCMConfigHandlerEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.event.AddVolumesFinalizedEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.event.AddVolumesFinishedEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.event.AddVolumesHandlerEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.event.AddVolumesOrchestrationFinishedEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.event.AddVolumesOrchestrationHandlerEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.event.AddVolumesRequest;
import com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.event.AddVolumesValidateEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.event.AddVolumesValidationFinishedEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.event.AttachVolumesFinishedEvent;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.flow.core.AbstractActionTestSupport;
import com.sequenceiq.flow.core.CommonContext;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowRegister;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;

@ExtendWith(MockitoExtension.class)
class AddVolumesActionsTest {

    private Map<Object, Object> variables = new HashMap<>();

    @Mock
    private CloudbreakFlowMessageService flowMessageService;

    @Mock
    private StackUpdater stackUpdater;

    @InjectMocks
    private AddVolumesActions underTest;

    @Mock
    private FlowRegister runningFlows;

    @Mock
    private EventBus eventBus;

    @Mock
    private ErrorHandlerAwareReactorEventFactory reactorEventFactory;

    private CommonContext context;

    @Mock
    private FlowParameters flowParameters;

    @Captor
    private ArgumentCaptor<String> captor;

    @BeforeEach
    void setUp() {
        context = new CommonContext(flowParameters);
    }

    @Test
    void testAddVolumesValidateAction() throws Exception {
        AddVolumesRequest addVolumesRequest = AddVolumesRequest.Builder.builder().withCloudVolumeUsageType(CloudVolumeUsageType.GENERAL)
                .withInstanceGroup("test").withSize(400L).withStackId(1L).withNumberOfDisks(2L).withType("gp2").build();
        AddVolumesValidateEvent event = new AddVolumesValidateEvent(1L, 2L, "gp2", 400L,
                CloudVolumeUsageType.GENERAL, "test");
        when(reactorEventFactory.createEvent(any(), any())).thenReturn(new Event<>(new Event.Headers(new HashMap<>()), event));

        AbstractAddVolumesAction<AddVolumesRequest> action = (AbstractAddVolumesAction<AddVolumesRequest>) underTest.addVolumesValidateAction();
        initActionPrivateFields(action);
        new AbstractActionTestSupport<>(action).doExecute(context, addVolumesRequest, variables);

        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventBus).notify(captor.capture(), eventCaptor.capture());
        assertEquals(ADD_VOLUMES_VALIDATE_HANDLER_EVENT.event(), captor.getValue());
        assertEvent(eventCaptor);
    }

    @Test
    void testAddVolumesAction() throws Exception {
        AddVolumesValidationFinishedEvent addVolumesValidationFinishedEvent =
                new AddVolumesValidationFinishedEvent(1L, 2L, "gp2", 400L, CloudVolumeUsageType.GENERAL, "test");
        AddVolumesHandlerEvent event = new AddVolumesHandlerEvent(1L, 2L, "gp2", 400L,
                CloudVolumeUsageType.GENERAL, "test");
        doReturn(new Event<>(new Event.Headers(new HashMap<>()), event)).when(reactorEventFactory).createEvent(any(), any());

        AbstractAddVolumesAction<AddVolumesValidationFinishedEvent> action =
                (AbstractAddVolumesAction<AddVolumesValidationFinishedEvent>) underTest.addVolumesAction();
        initActionPrivateFields(action);
        new AbstractActionTestSupport<>(action).doExecute(context, addVolumesValidationFinishedEvent, variables);

        verify(flowMessageService).fireEventAndLog(anyLong(), captor.capture(), any(), anyString(), anyString(), anyString());
        assertEquals(CLUSTER_ADDING_VOLUMES.name(), captor.getValue());
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventBus).notify(captor.capture(), eventCaptor.capture());
        assertEquals(ADD_VOLUMES_HANDLER_EVENT.event(), captor.getValue());
        assertEvent(eventCaptor);
    }

    @Test
    void testAttachVolumesAction() throws Exception {
        AddVolumesFinishedEvent event = new AddVolumesFinishedEvent(1L, 2L, "gp2", 400L,
                CloudVolumeUsageType.GENERAL, "test");
        doReturn(new Event<>(new Event.Headers(new HashMap<>()), event)).when(reactorEventFactory).createEvent(any(), any());
        AbstractAddVolumesAction<AddVolumesFinishedEvent> action = (AbstractAddVolumesAction<AddVolumesFinishedEvent>) underTest.attachVolumesAction();
        initActionPrivateFields(action);
        new AbstractActionTestSupport<>(action).doExecute(context, event, variables);
        verify(flowMessageService).fireEventAndLog(anyLong(), captor.capture(), any(), anyString(), anyString(), anyString());
        assertEquals(CLUSTER_ATTACHING_VOLUMES.name(), captor.getValue());
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventBus).notify(captor.capture(), eventCaptor.capture());
        assertEquals(ATTACH_VOLUMES_HANDLER_EVENT.event(), captor.getValue());
        assertEvent(eventCaptor);
    }

    @Test
    void testAddVolumesOrchestrationAction() throws Exception {
        AttachVolumesFinishedEvent attachVolumesFinishedRequest = new AttachVolumesFinishedEvent(1L, 2L, "gp2", 400L,
                CloudVolumeUsageType.GENERAL, "test");
        AddVolumesOrchestrationHandlerEvent event = new AddVolumesOrchestrationHandlerEvent(1L, 2L, "gp2", 400L,
                CloudVolumeUsageType.GENERAL, "test");
        doReturn(new Event<>(new Event.Headers(new HashMap<>()), event)).when(reactorEventFactory).createEvent(any(), any());
        AbstractAddVolumesAction<AttachVolumesFinishedEvent> action = (AbstractAddVolumesAction<AttachVolumesFinishedEvent>)
                underTest.addVolumesOrchestrationAction();
        initActionPrivateFields(action);
        new AbstractActionTestSupport<>(action).doExecute(context, attachVolumesFinishedRequest, variables);
        verify(flowMessageService).fireEventAndLog(anyLong(), captor.capture(), any(), anyString(), anyString(), anyString());
        assertEquals(CLUSTER_MOUNTING_VOLUMES.name(), captor.getValue());
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventBus).notify(captor.capture(), eventCaptor.capture());
        assertEquals(ADD_VOLUMES_ORCHESTRATION_HANDLER_EVENT.event(), captor.getValue());
        assertEvent(eventCaptor);
    }

    @Test
    void testAddVolumesCMConfigAction() throws Exception {
        AddVolumesOrchestrationFinishedEvent addVolumesFinishedRequest = new AddVolumesOrchestrationFinishedEvent(1L, 2L, "gp2", 400L,
                CloudVolumeUsageType.GENERAL, "test");
        AddVolumesCMConfigHandlerEvent event = new AddVolumesCMConfigHandlerEvent(1L, "test", 2L, "gp2", 400L,
                CloudVolumeUsageType.GENERAL);
        doReturn(new Event<>(new Event.Headers(new HashMap<>()), event)).when(reactorEventFactory).createEvent(any(), any());
        AbstractAddVolumesAction<AddVolumesOrchestrationFinishedEvent> action = (AbstractAddVolumesAction<AddVolumesOrchestrationFinishedEvent>)
                underTest.addVolumesCMConfigAction();
        initActionPrivateFields(action);
        new AbstractActionTestSupport<>(action).doExecute(context, addVolumesFinishedRequest, variables);
        verify(flowMessageService).fireEventAndLog(anyLong(), captor.capture(), any(), anyString(), anyString());
        assertEquals(CLUSTER_CM_CONFIG_CHANGE.name(), captor.getValue());
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventBus).notify(captor.capture(), eventCaptor.capture());
        assertEquals(ADD_VOLUMES_CM_CONFIGURATION_HANDLER_EVENT.event(), captor.getValue());
        assertEvent(eventCaptor);
    }

    @Test
    void testAddVolumesFinishedAction() throws Exception {
        AddVolumesCMConfigFinishedEvent addVolumesFinishedRequest = new AddVolumesCMConfigFinishedEvent(1L, "test", 2L, "gp2", 400L,
                CloudVolumeUsageType.GENERAL);
        AddVolumesFinalizedEvent event = new AddVolumesFinalizedEvent(1L);
        doReturn(new Event<>(new Event.Headers(new HashMap<>()), event)).when(reactorEventFactory).createEvent(any(), any());
        AbstractAddVolumesAction<AddVolumesCMConfigFinishedEvent> action = (AbstractAddVolumesAction<AddVolumesCMConfigFinishedEvent>)
                underTest.addVolumesFinishedAction();
        initActionPrivateFields(action);
        new AbstractActionTestSupport<>(action).doExecute(context, addVolumesFinishedRequest, variables);
        verify(flowMessageService).fireEventAndLog(anyLong(), captor.capture(), any(), anyString(), anyString(), anyString());
        assertEquals(AVAILABLE.name(), captor.getValue());
        ArgumentCaptor<Event<AddVolumesFinalizedEvent>> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventBus).notify(captor.capture(), eventCaptor.capture());
        assertEquals(FINALIZED_EVENT.event(), captor.getValue());
        assertEquals(1L, eventCaptor.getValue().getData().getResourceId());
    }

    private void initActionPrivateFields(Action<?, ?> action) {
        ReflectionTestUtils.setField(action, null, runningFlows, FlowRegister.class);
        ReflectionTestUtils.setField(action, null, eventBus, EventBus.class);
        ReflectionTestUtils.setField(action, null, reactorEventFactory, ErrorHandlerAwareReactorEventFactory.class);
    }

    private void assertEvent(ArgumentCaptor<Event> eventCaptor) {
        assertEquals("test", ReflectionTestUtils.getField(eventCaptor.getValue().getData(), "instanceGroup"));
        assertEquals(400L, ReflectionTestUtils.getField(eventCaptor.getValue().getData(), "size"));
        assertEquals(2L, ReflectionTestUtils.getField(eventCaptor.getValue().getData(), "numberOfDisks"));
        assertEquals("gp2", ReflectionTestUtils.getField(eventCaptor.getValue().getData(), "type"));
    }
}