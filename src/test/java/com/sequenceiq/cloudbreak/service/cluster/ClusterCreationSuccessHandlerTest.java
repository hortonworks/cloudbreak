package com.sequenceiq.cloudbreak.service.cluster;

import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.User;
import com.sequenceiq.cloudbreak.domain.WebsocketEndPoint;
import com.sequenceiq.cloudbreak.repository.ClusterRepository;
import com.sequenceiq.cloudbreak.websocket.WebsocketService;
import com.sequenceiq.cloudbreak.websocket.message.StatusMessage;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.event.Event;

import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class ClusterCreationSuccessHandlerTest {

    @InjectMocks
    private ClusterCreationSuccessHandler underTest;

    @Mock
    private WebsocketService websocketService;

    @Mock
    private ClusterRepository clusterRepository;

    private ClusterCreationSuccess clusterCreationSuccess;

    private Event<ClusterCreationSuccess> event;

    private Cluster cluster;

    @Before
    public void setUp() {
        underTest = new ClusterCreationSuccessHandler();
        MockitoAnnotations.initMocks(this);
        clusterCreationSuccess = new ClusterCreationSuccess(1L, 20L);
        event = new Event<>(clusterCreationSuccess);
        cluster = new Cluster();
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
