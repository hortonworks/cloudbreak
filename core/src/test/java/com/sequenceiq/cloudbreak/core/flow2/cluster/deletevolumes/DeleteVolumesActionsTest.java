package com.sequenceiq.cloudbreak.core.flow2.cluster.deletevolumes;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.deletevolumes.DeleteVolumesEvent.DELETE_VOLUMES_CM_CONFIG_HANDLER_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.deletevolumes.DeleteVolumesEvent.DELETE_VOLUMES_HANDLER_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.deletevolumes.DeleteVolumesEvent.DELETE_VOLUMES_ORCHESTRATION_HANDLER_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.deletevolumes.DeleteVolumesEvent.DELETE_VOLUMES_VALIDATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.deletevolumes.DeleteVolumesEvent.DELETE_VOLUMES_VALIDATION_HANDLER_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.deletevolumes.DeleteVolumesEvent.FINALIZED_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackDeleteVolumesRequest;
import com.sequenceiq.cloudbreak.cloud.Authenticator;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.converter.spi.ResourceToCloudResourceConverter;
import com.sequenceiq.cloudbreak.converter.spi.StackToCloudStackConverter;
import com.sequenceiq.cloudbreak.core.flow2.cluster.AbstractClusterAction;
import com.sequenceiq.cloudbreak.core.flow2.cluster.ClusterViewContext;
import com.sequenceiq.cloudbreak.core.flow2.event.DeleteVolumesTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.DeleteVolumesFinishedEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.DeleteVolumesRequest;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.util.StackUtil;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.flow.core.AbstractActionTestSupport;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowRegister;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;

@ExtendWith(MockitoExtension.class)
public class DeleteVolumesActionsTest {

    @Mock
    private DeleteVolumesService deleteVolumesService;

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private CmTemplateProcessorFactory cmTemplateProcessorFactory;

    @Mock
    private ResourceToCloudResourceConverter cloudResourceConverter;

    @Mock
    private CloudbreakFlowMessageService flowMessageService;

    @Mock
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Mock
    private StackUtil stackUtil;

    @Mock
    private StackToCloudStackConverter cloudStackConverter;

    @Mock
    private CloudCredential cloudCredential;

    @InjectMocks
    @Spy
    private DeleteVolumesActions underTest;

    private ClusterViewContext context;

    @Mock
    private FlowParameters flowParameters;

    @Mock
    private StackView stackView;

    @Mock
    private ClusterView clusterView;

    @Mock
    private StackDeleteVolumesRequest stackDeleteVolumesRequest;

    @Mock
    private FlowRegister runningFlows;

    @Mock
    private EventBus eventBus;

    @Mock
    private CmTemplateProcessor cmTemplateProcessor;

    @Mock
    private ErrorHandlerAwareReactorEventFactory reactorEventFactory;

    @Mock
    private Authenticator authenticator;

    @Mock
    private CloudConnector cloudConnector;

    @Mock
    private AuthenticatedContext authenticatedContext;

    @Captor
    private ArgumentCaptor<String> captor;

    @BeforeEach
    void setUp() {
        context = new ClusterViewContext(flowParameters, stackView, clusterView);
    }

    @Test
    public void testDeleteVolumesValidationAction() throws Exception {
        String selector = DELETE_VOLUMES_VALIDATION_EVENT.event();
        DeleteVolumesTriggerEvent event = new DeleteVolumesTriggerEvent(selector, 1L, stackDeleteVolumesRequest);
        Map<Object, Object> variables = new HashMap<>();
        new AbstractActionTestSupport<>(getTriggerAction()).doExecute(context, event, variables);
        verify(eventBus).notify(captor.capture(), any());
        assertEquals(DELETE_VOLUMES_VALIDATION_HANDLER_EVENT.event(), captor.getValue());
    }

    @Test
    public void testDeleteVolumesAction() throws Exception {
        doReturn(1L).when(stackDeleteVolumesRequest).getStackId();
        CloudResource resource = mock(CloudResource.class);
        DeleteVolumesRequest event = new DeleteVolumesRequest(List.of(resource), stackDeleteVolumesRequest, "MOCK", Set.of());
        Map<Object, Object> variables = new HashMap<>();
        new AbstractActionTestSupport<>(getDetachAndDeleteAction()).doExecute(context, event, variables);
        verify(eventBus).notify(captor.capture(), any());
        assertEquals(DELETE_VOLUMES_HANDLER_EVENT.event(), captor.getValue());
    }

    @Test
    public void testDeleteVolumesFinishedAction() throws Exception {
        doReturn(1L).when(stackDeleteVolumesRequest).getStackId();
        doReturn("Test").when(stackDeleteVolumesRequest).getGroup();
        DeleteVolumesFinishedEvent event = new DeleteVolumesFinishedEvent(stackDeleteVolumesRequest);
        AbstractClusterAction<DeleteVolumesFinishedEvent> action = (AbstractClusterAction<DeleteVolumesFinishedEvent>) underTest.deleteVolumesFinishedAction();
        initActionPrivateFields(action);
        Map<Object, Object> variables = new HashMap<>();
        new AbstractActionTestSupport<>(action).doExecute(context, event, variables);
        verify(eventBus).notify(captor.capture(), any());
        assertEquals(DELETE_VOLUMES_ORCHESTRATION_HANDLER_EVENT.event(), captor.getValue());
    }

    @Test
    public void testDeleteVolumesOrchestrationFinishedAction() throws Exception {
        DeleteVolumesOrchestrationFinishedEvent event = new DeleteVolumesOrchestrationFinishedEvent(1L, "Test");
        AbstractClusterAction<DeleteVolumesOrchestrationFinishedEvent> action =
                (AbstractClusterAction<DeleteVolumesOrchestrationFinishedEvent>) underTest.deleteVolumesOrchestrationFinishedAction();
        initActionPrivateFields(action);
        Map<Object, Object> variables = new HashMap<>();
        new AbstractActionTestSupport<>(action).doExecute(context, event, variables);
        verify(eventBus).notify(captor.capture(), any());
        assertEquals(DELETE_VOLUMES_CM_CONFIG_HANDLER_EVENT.event(), captor.getValue());
    }

    @Test
    public void testDeleteVolumesCMConfigFinishedAction() throws Exception {
        DeleteVolumesCMConfigFinishedEvent event = new DeleteVolumesCMConfigFinishedEvent(1L, "Test");
        AbstractClusterAction<DeleteVolumesCMConfigFinishedEvent> action =
                (AbstractClusterAction<DeleteVolumesCMConfigFinishedEvent>) underTest.deleteVolumesCMConfigFinishedAction();
        initActionPrivateFields(action);
        Map<Object, Object> variables = new HashMap<>();
        new AbstractActionTestSupport<>(action).doExecute(context, event, variables);
        verify(eventBus).notify(captor.capture(), any());
        assertEquals(FINALIZED_EVENT.event(), captor.getValue());
    }

    private AbstractClusterAction<DeleteVolumesTriggerEvent> getTriggerAction() {
        AbstractClusterAction<DeleteVolumesTriggerEvent> action = (AbstractClusterAction<DeleteVolumesTriggerEvent>) underTest.deleteVolumesValidationAction();
        initActionPrivateFields(action);
        return action;
    }

    private AbstractClusterAction<DeleteVolumesRequest> getDetachAndDeleteAction() {
        AbstractClusterAction<DeleteVolumesRequest> action = (AbstractClusterAction<DeleteVolumesRequest>) underTest.deleteVolumesAction();
        initActionPrivateFields(action);
        return action;
    }

    private void initActionPrivateFields(Action<?, ?> action) {
        ReflectionTestUtils.setField(action, null, runningFlows, FlowRegister.class);
        ReflectionTestUtils.setField(action, null, eventBus, EventBus.class);
        ReflectionTestUtils.setField(action, null, reactorEventFactory, ErrorHandlerAwareReactorEventFactory.class);
    }
}