package com.sequenceiq.cloudbreak.service.cluster;

import static org.mockito.BDDMockito.given;

import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.ClusterRepository;
import com.sequenceiq.cloudbreak.repository.RetryingStackUpdater;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.cluster.event.ClusterCreationSuccess;
import com.sequenceiq.cloudbreak.service.cluster.handler.ClusterCreationSuccessHandler;

import reactor.event.Event;

public class ClusterCreationSuccessHandlerTest {

    @InjectMocks
    private ClusterCreationSuccessHandler underTest;

    @Mock
    private ClusterRepository clusterRepository;

    @Mock
    private StackRepository stackRepository;

    @Mock
    private RetryingStackUpdater stackUpdater;

    private ClusterCreationSuccess clusterCreationSuccess;

    private Event<ClusterCreationSuccess> event;

    private Cluster cluster;

    private Stack stack;

    @Before
    public void setUp() {
        underTest = new ClusterCreationSuccessHandler();
        MockitoAnnotations.initMocks(this);
        clusterCreationSuccess = new ClusterCreationSuccess(1L, 20L, "1.1.1.1");
        event = new Event<>(clusterCreationSuccess);
        cluster = new Cluster();
        cluster.setEmailNeeded(false);
        stack = new Stack();
        Set<InstanceGroup> instanceMetaData = new HashSet<>();
        stack.setInstanceGroups(instanceMetaData);
        cluster.setOwner("John");
        cluster.setAccount("Acme");
    }

    @Test
    public void testAccept() {
        // GIVEN
        given(clusterRepository.findById(1L)).willReturn(cluster);
        given(stackRepository.findStackWithListsForCluster(1L)).willReturn(stack);
        // WHEN
        underTest.accept(event);
    }
}
