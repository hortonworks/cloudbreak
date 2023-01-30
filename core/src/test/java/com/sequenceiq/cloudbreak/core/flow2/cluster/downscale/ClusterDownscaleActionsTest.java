package com.sequenceiq.cloudbreak.core.flow2.cluster.downscale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.statemachine.ExtendedState;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.action.Action;
import org.springframework.statemachine.state.State;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cluster.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.core.flow2.event.ClusterDownscaleDetails;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.CollectDownscaleCandidatesRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.CollectDownscaleCandidatesResult;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.DecommissionRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.DecommissionResult;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.StopCmServicesOnHostsRequest;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowRegister;
import com.sequenceiq.flow.core.MessageFactory;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;

@ExtendWith(MockitoExtension.class)
class ClusterDownscaleActionsTest {

    @Mock
    private EventBus eventBus;

    @Mock
    private ErrorHandlerAwareReactorEventFactory reactorEventFactory;

    @Mock
    private FlowRegister runningFlows;

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private ClusterComponentConfigProvider clusterComponentProvider;

    @InjectMocks
    private ClusterDownscaleActions clusterDownscaleActions;

    @Test
    public void testDecommissionActionWithRepairAndCMGreaterThan790() {
        StateContext stateContext = mock(StateContext.class);
        when(stateContext.getMessageHeader(MessageFactory.HEADERS.FLOW_PARAMETERS.name())).thenReturn(new FlowParameters("1111", "usercrn"));
        CollectDownscaleCandidatesRequest collectDownscaleCandidatesRequest = new CollectDownscaleCandidatesRequest(1L,
                Map.of("compute", 2), Map.of("compute", Set.of(1L, 2L)), new ClusterDownscaleDetails(true, true, true));
        CollectDownscaleCandidatesResult collectDownscaleCandidatesResult =
                new CollectDownscaleCandidatesResult(collectDownscaleCandidatesRequest, Set.of(1L, 2L));
        when(stateContext.getMessageHeader(MessageFactory.HEADERS.DATA.name())).thenReturn(collectDownscaleCandidatesResult);
        ExtendedState extendedState = mock(ExtendedState.class);
        Map<Object, Object> variablesMap = new HashMap<>();
        variablesMap.put("REPAIR", true);
        when(extendedState.getVariables()).thenReturn(variablesMap);
        when(stateContext.getExtendedState()).thenReturn(extendedState);
        StateMachine stateMachine = mock(StateMachine.class);
        State state = mock(State.class);
        when(state.getId()).thenReturn("FlowStateName");
        when(stateMachine.getState()).thenReturn(state);
        when(stateContext.getStateMachine()).thenReturn(stateMachine);
        StackView stackView = mock(StackView.class);
        when(stackView.getId()).thenReturn(1L);
        when(stackDtoService.getStackViewById(1L)).thenReturn(stackView);
        ClusterView clusterView = mock(ClusterView.class);
        when(clusterView.getId()).thenReturn(1L);
        when(stackDtoService.getClusterViewByStackId(1L)).thenReturn(clusterView);
        StackDto stackDto = mock(StackDto.class);
        when(stackDto.hasCustomHostname()).thenReturn(true);
        when(stackDto.getCluster()).thenReturn(clusterView);
        InstanceMetadataView instanceMetadataView1 = mock(InstanceMetadataView.class);
        when(instanceMetadataView1.getDiscoveryFQDN()).thenReturn("compute1.example.com");
        when(stackDto.getInstanceMetadata(1L)).thenReturn(Optional.of(instanceMetadataView1));
        InstanceMetadataView instanceMetadataView2 = mock(InstanceMetadataView.class);
        when(instanceMetadataView2.getDiscoveryFQDN()).thenReturn("compute2.example.com");
        when(stackDto.getInstanceMetadata(2L)).thenReturn(Optional.of(instanceMetadataView2));
        when(stackDtoService.getById(1L)).thenReturn(stackDto);
        ClouderaManagerRepo clouderaManagerRepo = new ClouderaManagerRepo();
        clouderaManagerRepo.setVersion("7.9.6");
        when(clusterComponentProvider.getClouderaManagerRepoDetails(1L)).thenReturn(clouderaManagerRepo);

        Action<?, ?> action = clusterDownscaleActions.decommissionAction();
        ReflectionTestUtils.setField(action, "stackDtoService", stackDtoService);
        ReflectionTestUtils.setField(action, "runningFlows", runningFlows);
        ReflectionTestUtils.setField(action, "reactorEventFactory", reactorEventFactory);
        ReflectionTestUtils.setField(action, "eventBus", eventBus);
        ArgumentCaptor<StopCmServicesOnHostsRequest> payloadArgumentCaptor = ArgumentCaptor.forClass(StopCmServicesOnHostsRequest.class);
        action.execute(stateContext);

        verify(reactorEventFactory, times(1)).createEvent(any(), payloadArgumentCaptor.capture());
        verify(eventBus, times(1)).notify(eq("STOPCMSERVICESONHOSTSREQUEST"), any());
        StopCmServicesOnHostsRequest payload = payloadArgumentCaptor.getValue();
        assertThat(payload.getHostNamesToStop()).contains("compute1.example.com", "compute2.example.com");
        verify(stackDto, times(2)).getInstanceMetadata(any());
    }

    @Test
    public void testDecommissionActionWithRepairAndCMNotGreaterThan790() {
        StateContext stateContext = mock(StateContext.class);
        when(stateContext.getMessageHeader(MessageFactory.HEADERS.FLOW_PARAMETERS.name())).thenReturn(new FlowParameters("1111", "usercrn"));
        CollectDownscaleCandidatesRequest collectDownscaleCandidatesRequest = new CollectDownscaleCandidatesRequest(1L,
                Map.of("compute", 2), Map.of("compute", Set.of(1L, 2L)), new ClusterDownscaleDetails(true, true, true));
        CollectDownscaleCandidatesResult collectDownscaleCandidatesResult =
                new CollectDownscaleCandidatesResult(collectDownscaleCandidatesRequest, Set.of(1L, 2L));
        when(stateContext.getMessageHeader(MessageFactory.HEADERS.DATA.name())).thenReturn(collectDownscaleCandidatesResult);
        ExtendedState extendedState = mock(ExtendedState.class);
        Map<Object, Object> variablesMap = new HashMap<>();
        variablesMap.put("REPAIR", true);
        when(extendedState.getVariables()).thenReturn(variablesMap);
        when(stateContext.getExtendedState()).thenReturn(extendedState);
        StateMachine stateMachine = mock(StateMachine.class);
        State state = mock(State.class);
        when(state.getId()).thenReturn("FlowStateName");
        when(stateMachine.getState()).thenReturn(state);
        when(stateContext.getStateMachine()).thenReturn(stateMachine);
        StackView stackView = mock(StackView.class);
        when(stackView.getId()).thenReturn(1L);
        when(stackDtoService.getStackViewById(1L)).thenReturn(stackView);
        ClusterView clusterView = mock(ClusterView.class);
        when(clusterView.getId()).thenReturn(1L);
        when(stackDtoService.getClusterViewByStackId(1L)).thenReturn(clusterView);
        StackDto stackDto = mock(StackDto.class);
        when(stackDto.hasCustomHostname()).thenReturn(true);
        when(stackDto.getCluster()).thenReturn(clusterView);
        InstanceMetadataView instanceMetadataView1 = mock(InstanceMetadataView.class);
        when(instanceMetadataView1.getDiscoveryFQDN()).thenReturn("compute1.example.com");
        when(stackDto.getInstanceMetadata(1L)).thenReturn(Optional.of(instanceMetadataView1));
        InstanceMetadataView instanceMetadataView2 = mock(InstanceMetadataView.class);
        when(instanceMetadataView2.getDiscoveryFQDN()).thenReturn("compute2.example.com");
        when(stackDto.getInstanceMetadata(2L)).thenReturn(Optional.of(instanceMetadataView2));
        when(stackDtoService.getById(1L)).thenReturn(stackDto);
        ClouderaManagerRepo clouderaManagerRepo = new ClouderaManagerRepo();
        clouderaManagerRepo.setVersion("7.2.1");
        when(clusterComponentProvider.getClouderaManagerRepoDetails(1L)).thenReturn(clouderaManagerRepo);

        Action<?, ?> action = clusterDownscaleActions.decommissionAction();
        ReflectionTestUtils.setField(action, "stackDtoService", stackDtoService);
        ReflectionTestUtils.setField(action, "runningFlows", runningFlows);
        ReflectionTestUtils.setField(action, "reactorEventFactory", reactorEventFactory);
        ReflectionTestUtils.setField(action, "eventBus", eventBus);
        ArgumentCaptor<DecommissionResult> payloadArgumentCaptor = ArgumentCaptor.forClass(DecommissionResult.class);
        action.execute(stateContext);

        verify(reactorEventFactory, times(1)).createEvent(any(), payloadArgumentCaptor.capture());
        verify(eventBus, times(1)).notify(eq("DECOMMISSIONRESULT"), any());
        DecommissionResult payload = payloadArgumentCaptor.getValue();
        assertThat(payload.getHostNames()).contains("compute1.example.com", "compute2.example.com");
        verify(stackDto, times(2)).getInstanceMetadata(any());
    }

    @Test
    public void testDecommissionActionWithNoRepair() {
        StateContext stateContext = mock(StateContext.class);
        when(stateContext.getMessageHeader(MessageFactory.HEADERS.FLOW_PARAMETERS.name())).thenReturn(new FlowParameters("1111", "usercrn"));
        CollectDownscaleCandidatesRequest collectDownscaleCandidatesRequest = new CollectDownscaleCandidatesRequest(1L,
                Map.of("compute", 2), Map.of("compute", Set.of(1L, 2L)), new ClusterDownscaleDetails(true, true, true));
        CollectDownscaleCandidatesResult collectDownscaleCandidatesResult =
                new CollectDownscaleCandidatesResult(collectDownscaleCandidatesRequest, Set.of(1L, 2L));
        when(stateContext.getMessageHeader(MessageFactory.HEADERS.DATA.name())).thenReturn(collectDownscaleCandidatesResult);
        ExtendedState extendedState = mock(ExtendedState.class);
        Map<Object, Object> variablesMap = new HashMap<>();
        variablesMap.put("REPAIR", false);
        when(extendedState.getVariables()).thenReturn(variablesMap);
        when(stateContext.getExtendedState()).thenReturn(extendedState);
        StateMachine stateMachine = mock(StateMachine.class);
        State state = mock(State.class);
        when(state.getId()).thenReturn("FlowStateName");
        when(stateMachine.getState()).thenReturn(state);
        when(stateContext.getStateMachine()).thenReturn(stateMachine);
        StackView stackView = mock(StackView.class);
        when(stackView.getId()).thenReturn(1L);
        when(stackDtoService.getStackViewById(1L)).thenReturn(stackView);
        ClusterView clusterView = mock(ClusterView.class);
        when(stackDtoService.getClusterViewByStackId(1L)).thenReturn(clusterView);
        StackDto stackDto = mock(StackDto.class);
        ClouderaManagerRepo clouderaManagerRepo = new ClouderaManagerRepo();
        clouderaManagerRepo.setVersion("7.2.1");

        Action<?, ?> action = clusterDownscaleActions.decommissionAction();
        ReflectionTestUtils.setField(action, "stackDtoService", stackDtoService);
        ReflectionTestUtils.setField(action, "runningFlows", runningFlows);
        ReflectionTestUtils.setField(action, "reactorEventFactory", reactorEventFactory);
        ReflectionTestUtils.setField(action, "eventBus", eventBus);
        ArgumentCaptor<DecommissionRequest> payloadArgumentCaptor = ArgumentCaptor.forClass(DecommissionRequest.class);
        action.execute(stateContext);

        verify(reactorEventFactory, times(1)).createEvent(any(), payloadArgumentCaptor.capture());
        verify(eventBus, times(1)).notify(eq("DECOMMISSIONREQUEST"), any());
        DecommissionRequest payload = payloadArgumentCaptor.getValue();
        assertThat(payload.getPrivateIds()).contains(1L, 2L);
        verify(stackDto, times(0)).getInstanceMetadata(any());
    }

}