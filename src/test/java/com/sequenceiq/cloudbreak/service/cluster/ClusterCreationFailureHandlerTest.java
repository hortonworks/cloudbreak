package com.sequenceiq.cloudbreak.service.cluster;

import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.repository.ClusterRepository;
import com.sequenceiq.cloudbreak.repository.RetryingStackUpdater;
import com.sequenceiq.cloudbreak.service.cluster.event.ClusterCreationFailure;
import com.sequenceiq.cloudbreak.service.cluster.handler.ClusterCreationFailureHandler;
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService;

import reactor.event.Event;

public class ClusterCreationFailureHandlerTest {

    @InjectMocks
    private ClusterCreationFailureHandler underTest;

    @Mock
    private ClusterRepository clusterRepository;

    @Mock
    private RetryingStackUpdater stackUpdater;

    @Mock
    private CloudbreakEventService eventService;

    private ClusterCreationFailure clusterCreationFailure;

    private Event<ClusterCreationFailure> event;

    private Cluster cluster;

    @Before
    public void setUp() {
        underTest = new ClusterCreationFailureHandler();
        MockitoAnnotations.initMocks(this);
        clusterCreationFailure = new ClusterCreationFailure(1L, 1L, "dummyMessage");
        event = new Event<>(clusterCreationFailure);
        cluster = new Cluster();
        cluster.setEmailNeeded(false);
        cluster.setOwner("dummy@myemail.com");
        cluster.setAccount("myaccount");
    }

    @Test
    public void testAccept() {
        //GIVEN
        given(clusterRepository.findById(1L)).willReturn(cluster);
        //WHEN
        underTest.accept(event);
        //THEN
        verify(eventService, times(1)).fireCloudbreakEvent(anyLong(), anyString(), anyString());
    }
}
