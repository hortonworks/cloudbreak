package com.sequenceiq.cloudbreak.service.cluster;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.conf.ReactorConfig;
import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.controller.InternalServerException;
import com.sequenceiq.cloudbreak.controller.NotFoundException;
import com.sequenceiq.cloudbreak.controller.json.ClusterRequest;
import com.sequenceiq.cloudbreak.controller.json.ClusterResponse;
import com.sequenceiq.cloudbreak.converter.ClusterConverter;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.User;
import com.sequenceiq.cloudbreak.repository.ClusterRepository;
import com.sequenceiq.cloudbreak.repository.RetryingStackUpdater;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import groovyx.net.http.HttpResponseException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import reactor.core.Reactor;
import reactor.event.Event;

import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class AmbariClusterServiceTest {

    public static final String DUMMY_CLUSTER_JSON = "dummyClusterJson";
    public static final String NOT_FOUND = "Not Found";

    @InjectMocks
    @Spy
    private AmbariClusterService underTest;

    @Mock
    private StackRepository stackRepository;

    @Mock
    private ClusterConverter clusterConverter;

    @Mock
    private ClusterRepository clusterRepository;

    @Mock
    private RetryingStackUpdater stackUpdater;

    @Mock
    private Reactor reactor;

    @Mock
    private AmbariClient ambariClient;

    @Mock
    private HttpResponseException mockedException;

    private Stack stack;

    private ClusterRequest clusterRequest;

    private ClusterResponse clusterResponse;

    private Cluster cluster;

    @Before
    public void setUp() {
        underTest = new AmbariClusterService();
        MockitoAnnotations.initMocks(this);
        cluster = createCluster();
        stack = createStack(cluster);
        clusterRequest = new ClusterRequest();
        clusterResponse = new ClusterResponse();
    }

    @Test
    public void testCreateCluster() {
        //GIVEN
        stack.setCluster(null);
        given(stackRepository.findOne(anyLong())).willReturn(stack);
        given(clusterConverter.convert(clusterRequest)).willReturn(cluster);
        given(stackUpdater.updateStackCluster(anyLong(), any(Cluster.class))).willReturn(stack);
        //WHEN
        underTest.createCluster(new User(), 1L, clusterRequest);
        //THEN
        verify(reactor, times(1)).notify(any(ReactorConfig.class), any(Event.class));
        verify(stackUpdater, times(1)).updateStackCluster(anyLong(), any(Cluster.class));
    }

    @Test(expected = BadRequestException.class)
    public void testCreateClusterWhenClusterAlreadyExists() {
        //GIVEN
        given(stackRepository.findOne(anyLong())).willReturn(stack);
        //WHEN
        underTest.createCluster(new User(), 1L, clusterRequest);
    }

    @Test
    public void testRetrieveCluster() throws HttpResponseException {
        //GIVEN
        given(stackRepository.findOne(anyLong())).willReturn(stack);
        doReturn(ambariClient).when(underTest).createAmbariClient(anyString());
        given(ambariClient.getClusterAsJson()).willReturn(DUMMY_CLUSTER_JSON);
        given(clusterConverter.convert(cluster, DUMMY_CLUSTER_JSON)).willReturn(clusterResponse);
        //WHEN
        underTest.retrieveCluster(new User(), 1L);
        //THEN
        verify(clusterConverter, times(1)).convert(cluster, DUMMY_CLUSTER_JSON);
    }

    @Test(expected = InternalServerException.class)
    public void testRetrieveClusterWhenClusterJsonIsNull() throws HttpResponseException {
        //GIVEN
        given(stackRepository.findOne(anyLong())).willReturn(stack);
        doReturn(ambariClient).when(underTest).createAmbariClient(anyString());
        given(ambariClient.getClusterAsJson()).willReturn(null);
        //WHEN
        underTest.retrieveCluster(new User(), 1L);
    }

    @Test(expected = NotFoundException.class)
    public void testRetrieveClusterThrowsHttpResponseNotFoundException() throws HttpResponseException {
        //GIVEN
        given(stackRepository.findOne(anyLong())).willReturn(stack);
        doReturn(ambariClient).when(underTest).createAmbariClient(anyString());
        given(ambariClient.getClusterAsJson()).willThrow(mockedException);
        given(mockedException.getMessage()).willReturn(NOT_FOUND);
        //WHEN
        underTest.retrieveCluster(new User(), 1L);
    }

    @Test(expected = InternalServerException.class)
    public void testRetrieveClusterThrowsHttpResponseException() throws HttpResponseException {
        //GIVEN
        given(stackRepository.findOne(anyLong())).willReturn(stack);
        doReturn(ambariClient).when(underTest).createAmbariClient(anyString());
        given(ambariClient.getClusterAsJson()).willThrow(mockedException);
        given(mockedException.getMessage()).willReturn(null);
        //WHEN
        underTest.retrieveCluster(new User(), 1L);
    }

    private Stack createStack(Cluster cluster) {
        Stack stack = new Stack();
        stack.setId(1L);
        stack.setCluster(cluster);
        return stack;
    }

    private Cluster createCluster() {
        Cluster cluster = new Cluster();
        cluster.setId(1L);
        cluster.setName("dummyCluster");
        return cluster;
    }
}
