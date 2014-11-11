package com.sequenceiq.cloudbreak.service.cluster.flow;

import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.conf.ReactorConfig;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.ClusterRepository;

import reactor.core.Reactor;
import reactor.event.Event;

public class AmbariClusterInstallerTest {

    @InjectMocks
    @Spy
    private AmbariClusterConnector underTest;

    @Mock
    private ClusterRepository clusterRepository;

    @Mock
    private Reactor reactor;

    @Mock
    private AmbariClient ambariClient;

    private Stack stack;

    private Cluster cluster;

    private Blueprint blueprint;

    @Before
    public void setUp() {
        underTest = new AmbariClusterConnector();
        MockitoAnnotations.initMocks(this);
        blueprint = createBlueprint();
        cluster = createCluster(blueprint);
        stack = createStack(cluster);
    }

    @Test
    public void testInstallAmbari() throws Exception {
        // GIVEN
        Map<String, List<String>> strListMap = createStringListMap();
        given(clusterRepository.save(cluster)).willReturn(cluster);
        doReturn(ambariClient).when(underTest).createAmbariClient(stack.getAmbariIp());
        given(ambariClient.recommendAssignments(anyString())).willReturn(strListMap);
        doNothing().when(ambariClient).createCluster(cluster.getName(), blueprint.getBlueprintName(), strListMap);
        given(ambariClient.getRequestProgress()).willReturn(new BigDecimal(100.0));
        // WHEN
        underTest.installAmbariCluster(stack);
        // THEN
        verify(reactor, times(1)).notify(any(ReactorConfig.class), any(Event.class));
    }

    @Test
    public void testInstallAmbariWhenInstallFailed() throws Exception {
        // GIVEN
        Map<String, List<String>> strListMap = createStringListMap();
        given(clusterRepository.save(cluster)).willReturn(cluster);
        doReturn(ambariClient).when(underTest).createAmbariClient(stack.getAmbariIp());
        given(ambariClient.recommendAssignments(anyString())).willReturn(strListMap);
        doNothing().when(ambariClient).createCluster(cluster.getName(), blueprint.getBlueprintName(), strListMap);
        // WHEN
        underTest.installAmbariCluster(stack);
        // THEN
        verify(reactor, times(1)).notify(any(ReactorConfig.class), any(Event.class));
    }

    @Test
    public void testInstallAmbariWhenExceptionOccursShouldInstallationFailed() throws Exception {
        // GIVEN
        Map<String, List<String>> strListMap = createStringListMap();
        given(clusterRepository.save(cluster)).willReturn(cluster);
        doReturn(ambariClient).when(underTest).createAmbariClient(stack.getAmbariIp());
        given(ambariClient.recommendAssignments(anyString())).willReturn(strListMap);
        doThrow(new IllegalArgumentException()).when(ambariClient).createCluster(cluster.getName(), blueprint.getBlueprintName(), strListMap);
        // WHEN
        underTest.installAmbariCluster(stack);
        // THEN
        verify(reactor, times(1)).notify(any(ReactorConfig.class), any(Event.class));
    }

    @Test
    public void testInstallAmbariWhenReachedMaxPollingEventsShouldInstallationFailed() throws Exception {
        // GIVEN
        Map<String, List<String>> strListMap = createStringListMap();
        stack.setNodeCount(0);
        given(clusterRepository.save(cluster)).willReturn(cluster);
        doReturn(ambariClient).when(underTest).createAmbariClient(stack.getAmbariIp());
        given(ambariClient.recommendAssignments(anyString())).willReturn(strListMap);
        // WHEN
        underTest.installAmbariCluster(stack);
        // THEN
        verify(reactor, times(1)).notify(any(ReactorConfig.class), any(Event.class));
    }

    private Stack createStack(Cluster cluster) {
        Stack stack = new Stack();
        stack.setId(1L);
        stack.setAmbariIp("172.17.0.2");
        stack.setNodeCount(2);
        stack.setCluster(cluster);
        return stack;
    }

    private Cluster createCluster(Blueprint blueprint) {
        Cluster cluster = new Cluster();
        cluster.setId(1L);
        cluster.setName("dummyCluster");
        cluster.setBlueprint(blueprint);
        return cluster;
    }

    private Blueprint createBlueprint() {
        Blueprint blueprint = new Blueprint();
        blueprint.setId(1L);
        blueprint.setBlueprintName("single-node-yarn");
        return blueprint;
    }

    private Map<String, List<String>> createStringListMap() {
        Map<String, List<String>> stringListMap = new HashMap<>();
        stringListMap.put("a1", Arrays.asList("assignment1", "assignment2"));
        return stringListMap;
    }
}
