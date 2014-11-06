package com.sequenceiq.cloudbreak.service.cluster;

import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;

import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.controller.InternalServerException;
import com.sequenceiq.cloudbreak.controller.json.ClusterRequest;
import com.sequenceiq.cloudbreak.controller.json.ClusterResponse;
import com.sequenceiq.cloudbreak.controller.json.HostGroupAdjustmentJson;
import com.sequenceiq.cloudbreak.converter.ClusterConverter;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.ClusterRepository;
import com.sequenceiq.cloudbreak.repository.RetryingStackUpdater;
import com.sequenceiq.cloudbreak.repository.StackRepository;

import groovyx.net.http.HttpResponseException;
import reactor.core.Reactor;

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
        given(stackRepository.findById(anyLong())).willReturn(stack);
        given(stackRepository.findOneWithLists(anyLong())).willReturn(stack);
    }

    @Test(expected = InternalServerException.class)
    public void testRetrieveClusterJsonWhenClusterJsonIsNull() throws HttpResponseException {
        // GIVEN
        doReturn(ambariClient).when(underTest).createAmbariClient(anyString());
        given(ambariClient.getClusterAsJson()).willReturn(null);
        // WHEN
        underTest.getClusterJson("123.12.3.4", 1L);
    }

    @Test(expected = InternalServerException.class)
    public void testRetrieveClusterJsonThrowsHttpResponseException() throws HttpResponseException {
        // GIVEN
        doReturn(ambariClient).when(underTest).createAmbariClient(anyString());
        given(ambariClient.getClusterAsJson()).willThrow(mockedException);
        given(mockedException.getMessage()).willReturn(null);
        // WHEN
        underTest.getClusterJson("127.0.0.1", 1L);
    }

    @Test(expected = BadRequestException.class)
    public void testUpdateHostsDoesntAcceptMultipleListingOfTheSameHostGroup() throws HttpResponseException {
        // GIVEN
        Set<HostGroupAdjustmentJson> hostGroupAdjustments = new HashSet<>();
        HostGroupAdjustmentJson hga1 = new HostGroupAdjustmentJson();
        hga1.setHostGroup("slave_1");
        hga1.setScalingAdjustment(1);
        HostGroupAdjustmentJson hga2 = new HostGroupAdjustmentJson();
        hga2.setHostGroup("slave_1");
        hga2.setScalingAdjustment(2);
        hostGroupAdjustments.add(hga1);
        hostGroupAdjustments.add(hga2);
        // WHEN
        underTest.updateHosts(stack.getId(), hostGroupAdjustments);
    }

    @Test(expected = BadRequestException.class)
    public void testUpdateHostsDoesntAcceptZeroScalingAdjustments() throws HttpResponseException {
        // GIVEN
        Set<HostGroupAdjustmentJson> hostGroupAdjustments = new HashSet<>();
        HostGroupAdjustmentJson hga1 = new HostGroupAdjustmentJson();
        hga1.setHostGroup("slave_1");
        hga1.setScalingAdjustment(0);
        HostGroupAdjustmentJson hga2 = new HostGroupAdjustmentJson();
        hga2.setHostGroup("master");
        hga2.setScalingAdjustment(0);
        hostGroupAdjustments.add(hga1);
        hostGroupAdjustments.add(hga2);
        // WHEN
        underTest.updateHosts(stack.getId(), hostGroupAdjustments);
    }

    @Test(expected = BadRequestException.class)
    public void testUpdateHostsDoesntAcceptScalingAdjustmentsWithDifferentSigns() throws HttpResponseException {
        // GIVEN
        Set<HostGroupAdjustmentJson> hostGroupAdjustments = new HashSet<>();
        HostGroupAdjustmentJson hga1 = new HostGroupAdjustmentJson();
        hga1.setHostGroup("slave_1");
        hga1.setScalingAdjustment(-2);
        HostGroupAdjustmentJson hga2 = new HostGroupAdjustmentJson();
        hga2.setHostGroup("master");
        hga2.setScalingAdjustment(3);
        hostGroupAdjustments.add(hga1);
        hostGroupAdjustments.add(hga2);
        // WHEN
        underTest.updateHosts(stack.getId(), hostGroupAdjustments);
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
        Blueprint blueprint = new Blueprint();
        blueprint.setId(1L);
        cluster.setBlueprint(blueprint);
        return cluster;
    }
}
