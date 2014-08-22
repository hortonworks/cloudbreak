package com.sequenceiq.cloudbreak.service.cluster;

import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.User;
import com.sequenceiq.cloudbreak.domain.WebsocketEndPoint;
import com.sequenceiq.cloudbreak.repository.ClusterRepository;
import com.sequenceiq.cloudbreak.service.cluster.event.ClusterCreationFailure;
import com.sequenceiq.cloudbreak.service.cluster.handler.ClusterCreationFailureHandler;
import com.sequenceiq.cloudbreak.websocket.WebsocketService;
import com.sequenceiq.cloudbreak.websocket.message.StatusMessage;

import reactor.event.Event;

public class ClusterCreationFailureHandlerTest {

    @InjectMocks
    private ClusterCreationFailureHandler underTest;

    @Mock
    private WebsocketService websocketService;

    @Mock
    private ClusterRepository clusterRepository;

    private ClusterCreationFailure clusterCreationFailure;

    private Event<ClusterCreationFailure> event;

    private Cluster cluster;

    @Before
    public void setUp() {
        underTest = new ClusterCreationFailureHandler();
        MockitoAnnotations.initMocks(this);
        clusterCreationFailure = new ClusterCreationFailure(1L, "dummyMessage");
        event = new Event<>(clusterCreationFailure);
        cluster = new Cluster();
        cluster.setEmailNeeded(false);
        User user = new User();
        user.setEmail("dummy@myemail.com");
        cluster.setUser(user);
    }

    @Test
    public void testAccept() {
        //GIVEN
        given(clusterRepository.findById(1L)).willReturn(cluster);
        doNothing().when(websocketService).sendToTopicUser(anyString(), any(WebsocketEndPoint.class), any(StatusMessage.class));
        //WHEN
        underTest.accept(event);
        //THEN
        verify(websocketService, times(1)).sendToTopicUser(anyString(), any(WebsocketEndPoint.class), any(StatusMessage.class));
    }
}
