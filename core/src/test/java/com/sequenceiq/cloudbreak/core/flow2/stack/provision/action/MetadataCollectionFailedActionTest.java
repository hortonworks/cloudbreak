package com.sequenceiq.cloudbreak.core.flow2.stack.provision.action;

import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.action.MetadataCollectionFailedAction.TRIES;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.statemachine.ExtendedState;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.state.State;

import com.sequenceiq.cloudbreak.cloud.event.instance.CollectMetadataResult;
import com.sequenceiq.cloudbreak.converter.spi.CredentialToCloudCredentialConverter;
import com.sequenceiq.cloudbreak.converter.spi.StackToCloudStackConverter;
import com.sequenceiq.cloudbreak.core.flow2.MessageFactory;
import com.sequenceiq.cloudbreak.core.flow2.stack.FlowMessageService;
import com.sequenceiq.cloudbreak.core.flow2.stack.Msg;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackContext;
import com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationEvent;
import com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationState;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.stack.StackService;

import reactor.bus.Event;
import reactor.bus.EventBus;

public class MetadataCollectionFailedActionTest {

    @InjectMocks
    private MetadataCollectionFailedAction underTest;

    @Mock
    private StackCreationService stackCreationService;

    @Mock
    private FlowMessageService flowMessageService;

    @Mock
    private StateContext stateContext;

    @Mock
    private ExtendedState extendedState;

    @Mock
    private StateMachine<StackCreationState, StackCreationEvent> stateMachine;

    @Mock
    private State<StackCreationState, StackCreationEvent> state;

    @Mock
    private Stack stack;

    @Mock
    private CollectMetadataResult collectMetadataResult;

    @Mock
    private StackService stackService;

    @Mock
    private StackToCloudStackConverter cloudStackConverter;

    @Mock
    private CredentialToCloudCredentialConverter credentialConverter;

    @Mock
    private EventBus eventBus;

    @Before
    public void setUp() {
        underTest = new MetadataCollectionFailedAction();
        MockitoAnnotations.initMocks(this);

        given(stateContext.getMessageHeader(eq(MessageFactory.HEADERS.DATA.name()))).willReturn(collectMetadataResult);
        given(stateContext.getExtendedState()).willReturn(extendedState);
        given(stateContext.getStateMachine()).willReturn(stateMachine);
        given(stateMachine.getState()).willReturn(state);
        given(state.getId()).willReturn(StackCreationState.INIT_STATE);
        given(collectMetadataResult.getStackId()).willReturn(1L);
        given(stackService.getById(1L)).willReturn(stack);
        given(collectMetadataResult.getErrorDetails()).willReturn(new RuntimeException("Error"));
    }

    @Test
    public void testMaxRetry() {
        Map<Object, Object> variables = new HashMap<>(Collections.<Object, Object>singletonMap(TRIES, 3));
        given(extendedState.getVariables()).willReturn(variables);
        underTest.execute(stateContext);
        verify(flowMessageService, times(1)).fireEventAndLog(anyLong(), any(Msg.class), anyString(), any());
        Assert.assertEquals("Tries must be 4", 4, variables.get(TRIES));
        verify(eventBus, times(1)).notify(eq("LAUNCHSTACKRESULT"), any(Event.class));
    }

    @Test
    public void testOverRetry() {
        Map<Object, Object> variables = new HashMap<>(Collections.<Object, Object>singletonMap(TRIES, 4));
        given(extendedState.getVariables()).willReturn(variables);
        underTest.execute(stateContext);
        verify(stackCreationService, times(1)).handleStackCreationFailure(any(StackContext.class), any(Exception.class));
        verify(eventBus, times(1)).notify(eq("FAILHANDLED"), any(Event.class));
    }
}
