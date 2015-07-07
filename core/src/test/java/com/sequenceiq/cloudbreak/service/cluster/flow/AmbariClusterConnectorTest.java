package com.sequenceiq.cloudbreak.service.cluster.flow;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyCollection;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.ambari.client.InvalidHostGroupHostAssociation;
import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.core.CloudbreakSecuritySetupException;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.ClusterRepository;
import com.sequenceiq.cloudbreak.repository.HostGroupRepository;
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository;
import com.sequenceiq.cloudbreak.service.PollingResult;
import com.sequenceiq.cloudbreak.service.PollingService;
import com.sequenceiq.cloudbreak.service.StatusCheckerTask;
import com.sequenceiq.cloudbreak.service.TlsSecurityService;
import com.sequenceiq.cloudbreak.service.cluster.AmbariClientProvider;
import com.sequenceiq.cloudbreak.service.cluster.AmbariOperationFailedException;
import com.sequenceiq.cloudbreak.service.cluster.HadoopConfigurationService;
import com.sequenceiq.cloudbreak.service.stack.flow.TLSClientConfig;

import groovyx.net.http.HttpResponseException;
import reactor.bus.EventBus;

@RunWith(MockitoJUnitRunner.class)
public class AmbariClusterConnectorTest {

    @Mock
    private TlsSecurityService tlsSecurityService;

    @Mock
    private EventBus reactor;

    @Mock
    private AmbariClient ambariClient;

    @Mock
    private TLSClientConfig tlsClientConfig;

    @Mock
    private AmbariClientProvider ambariClientProvider;

    @Mock
    private HadoopConfigurationService hadoopConfigurationService;

    @Mock
    private PollingService<AmbariHostsCheckerContext> hostsPollingService;

    @Mock
    private AmbariHostsStatusCheckerTask ambariHostsStatusCheckerTask;

    @Mock
    private HostGroupRepository hostGroupRepository;

    @Mock
    private AmbariOperationService ambariOperationService;

    @Mock
    private ClusterRepository clusterRepository;

    @Mock
    private InstanceMetaDataRepository instanceMetadataRepository;

    @InjectMocks
    @Spy
    private AmbariClusterConnector underTest = new AmbariClusterConnector();

    private Stack stack;

    private Cluster cluster;

    private Blueprint blueprint;

    @Before
    public void setUp() throws CloudbreakSecuritySetupException, HttpResponseException, InvalidHostGroupHostAssociation {
        stack = TestUtil.stack();
        blueprint = TestUtil.blueprint();
        cluster = TestUtil.cluster(blueprint, stack, 1L);
        stack.setCluster(cluster);
        when(tlsSecurityService.buildTLSClientConfig(anyLong(), anyString())).thenReturn(tlsClientConfig);
        when(ambariClient.extendBlueprintGlobalConfiguration(anyString(), anyMap())).thenReturn("");
        when(ambariClient.extendBlueprintHostGroupConfiguration(anyString(), anyMap())).thenReturn("");
        when(ambariClient.addBlueprint(anyString())).thenReturn("");
        when(hadoopConfigurationService.getHostGroupConfiguration(any(Cluster.class))).thenReturn(new HashMap<String, Map<String, Map<String, String>>>());
        when(ambariClientProvider.getAmbariClient(any(TLSClientConfig.class), anyString(), anyString())).thenReturn(ambariClient);
        when(hostsPollingService.pollWithTimeout(any(AmbariHostsStatusCheckerTask.class), any(AmbariHostsCheckerContext.class), anyInt(), anyInt()))
                .thenReturn(PollingResult.SUCCESS);
        when(hostGroupRepository.findHostGroupsInCluster(anyLong())).thenReturn(cluster.getHostGroups());
        when(ambariOperationService.waitForAmbariOperations(any(Stack.class), any(AmbariClient.class), anyMap())).thenReturn(PollingResult.SUCCESS);
        when(ambariOperationService.waitForAmbariOperations(any(Stack.class), any(AmbariClient.class), any(StatusCheckerTask.class), anyMap()))
                .thenReturn(PollingResult.SUCCESS);
        when(clusterRepository.save(any(Cluster.class))).thenReturn(cluster);
        when(instanceMetadataRepository.save(anyCollection())).thenReturn(stack.getRunningInstanceMetaData());
        when(ambariClient.recommendAssignments(anyString())).thenReturn(createStringListMap());

    }

    @Test
    public void testInstallAmbari() throws Exception {
        Cluster result = underTest.buildAmbariCluster(stack);
        assertEquals(cluster, result);
    }

    @Test(expected = AmbariOperationFailedException.class)
    public void testInstallAmbariWhenExceptionOccursShouldInstallationFailed() throws Exception {
        doThrow(new IllegalArgumentException()).when(ambariClient).createCluster(anyString(), anyString(), anyMap());
        underTest.buildAmbariCluster(stack);
    }

    @Test(expected = AmbariOperationFailedException.class)
    public void testInstallAmbariWhenReachedMaxPollingEventsShouldInstallationFailed() throws Exception {
        when(ambariOperationService.waitForAmbariOperations(any(Stack.class), any(AmbariClient.class), anyMap())).thenReturn(PollingResult.TIMEOUT);
        underTest.buildAmbariCluster(stack);
    }

    private Map<String, List<String>> createStringListMap() {
        Map<String, List<String>> stringListMap = new HashMap<>();
        stringListMap.put("a1", Arrays.asList("assignment1", "assignment2"));
        return stringListMap;
    }
}
