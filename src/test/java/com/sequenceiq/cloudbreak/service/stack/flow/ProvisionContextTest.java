package com.sequenceiq.cloudbreak.service.stack.flow;

import static org.mockito.BDDMockito.doNothing;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.times;
import static org.mockito.BDDMockito.verify;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.any;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.conf.ReactorConfig;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.Status;
import com.sequenceiq.cloudbreak.domain.WebsocketEndPoint;
import com.sequenceiq.cloudbreak.repository.RetryingStackUpdater;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.stack.connector.CloudPlatformConnector;
import com.sequenceiq.cloudbreak.service.stack.connector.UserDataBuilder;
import com.sequenceiq.cloudbreak.websocket.WebsocketService;
import com.sequenceiq.cloudbreak.websocket.message.StatusMessage;

import reactor.core.Reactor;
import reactor.event.Event;

public class ProvisionContextTest {

    private static final String DUMMY_USER_DATA = "dummyUserData";
    private static final String DUMMY_NAME = "dummyName";

    @InjectMocks
    private ProvisionContext underTest;

    @Mock
    private StackRepository stackRepository;

    @Mock
    private RetryingStackUpdater stackUpdater;

    @Mock
    private WebsocketService websocketService;

    @Mock
    private Map<CloudPlatform, CloudPlatformConnector> cloudPlatformConnectors;

    @Mock
    private Reactor reactor;

    @Mock
    private UserDataBuilder userDataBuilder;

    @Mock
    private CloudPlatformConnector provisioner;

    private Map<String, Object> setupProperties;

    private Map<String, String> userDataParams;

    private CloudPlatform cloudPlatform;

    private Stack stack;

    @Before
    public void setUp() {
        underTest = new ProvisionContext();
        MockitoAnnotations.initMocks(this);
        setupProperties = new HashMap<>();
        userDataParams = new HashMap<>();
        cloudPlatform = CloudPlatform.AZURE;
        stack = createStack();
    }

    @Test
    public void testBuildStack() {
        // GIVEN
        given(stackRepository.findById(1L)).willReturn(stack);
        given(stackUpdater.updateStackStatus(1L, Status.CREATE_IN_PROGRESS)).willReturn(stack);
        given(stackUpdater.updateStackStatusReason(anyLong(), anyString())).willReturn(stack);
        given(cloudPlatformConnectors.get(any(CloudPlatform.class))).willReturn(provisioner);
        doNothing().when(websocketService).sendToTopicUser(anyString(), any(WebsocketEndPoint.class), any(StatusMessage.class));
        given(userDataBuilder.build(any(CloudPlatform.class), anyString(), anyMap())).willReturn(DUMMY_USER_DATA);
        doNothing().when(provisioner).buildStack(stack, DUMMY_USER_DATA, setupProperties);
        // WHEN
        underTest.buildStack(cloudPlatform, 1L, setupProperties, userDataParams);
        // THEN
        verify(provisioner, times(0)).buildStack(stack, DUMMY_USER_DATA, setupProperties);
    }

    @Test
    public void testBuildStackWhenStatusIsNotRequested() {
        // GIVEN
        stack.setStatus(Status.CREATE_FAILED);
        given(stackRepository.findById(1L)).willReturn(stack);
        // WHEN
        underTest.buildStack(cloudPlatform, 1L, setupProperties, userDataParams);
        // THEN
        verify(reactor, times(0)).notify(any(ReactorConfig.class), any(Event.class));
        verify(websocketService, times(0)).sendToTopicUser(anyString(), any(WebsocketEndPoint.class), any(StatusMessage.class));
    }

    @Test
    public void testBuildStackWhenExceptionOccurs() {
        // GIVEN
        given(stackRepository.findById(1L)).willReturn(stack);
        given(stackRepository.findOneWithLists(1L)).willReturn(stack);
        given(stackUpdater.updateStackStatus(1L, Status.CREATE_IN_PROGRESS)).willThrow(new IllegalStateException());
        // WHEN
        underTest.buildStack(cloudPlatform, 1L, setupProperties, userDataParams);
        // THEN
        verify(provisioner, times(0)).buildStack(stack, DUMMY_USER_DATA, setupProperties);
        verify(reactor, times(1)).notify(any(ReactorConfig.class), any(Event.class));
    }

    private Stack createStack() {
        Stack stack = new Stack();
        stack.setId(1L);
        stack.setStatus(Status.REQUESTED);
        stack.setName(DUMMY_NAME);
        stack.setOwner("gipszjakab@myemail.com");
        return stack;
    }
}
