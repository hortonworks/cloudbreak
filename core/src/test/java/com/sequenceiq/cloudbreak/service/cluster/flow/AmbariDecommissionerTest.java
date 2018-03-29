package com.sequenceiq.cloudbreak.service.cluster.flow;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.collect.Sets;
import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.cloudbreak.common.type.HostMetadataState;
import com.sequenceiq.cloudbreak.core.CloudbreakSecuritySetupException;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.HostGroup;
import com.sequenceiq.cloudbreak.domain.HostMetadata;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.HostMetadataRepository;
import com.sequenceiq.cloudbreak.service.TlsSecurityService;
import com.sequenceiq.cloudbreak.service.cluster.AmbariClientProvider;
import com.sequenceiq.cloudbreak.service.cluster.NotEnoughNodeException;
import com.sequenceiq.cloudbreak.service.cluster.ambari.AmbariConfigurationService;
import com.sequenceiq.cloudbreak.service.cluster.ambari.AmbariDecommissioner;
import com.sequenceiq.cloudbreak.service.cluster.filter.ConfigParam;
import com.sequenceiq.cloudbreak.service.cluster.NotEnoughNodeException;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;

@RunWith(MockitoJUnitRunner.class)
public class AmbariDecommissionerTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @InjectMocks
    private AmbariDecommissioner underTest = new AmbariDecommissioner();

    @Mock
    private TlsSecurityService tlsSecurityService;

    @Mock
    private AmbariClientProvider ambariClientProvider;

    @Mock
    private HostGroupService hostGroupService;

    @Mock
    private HostMetadataRepository hostMetadataRepository;

    @Mock
    private AmbariConfigurationService configurationService;

    @Test
    public void testSelectNodesWhenHasOneUnhealthyNodeAndShouldSelectOne() {

        String hostname1 = "10.0.0.1";
        String hostname2 = "10.0.0.2";

        HostMetadata unhealhtyNode = getHostMetadata(hostname1, HostMetadataState.UNHEALTHY);
        HostMetadata healhtyNode = getHostMetadata(hostname2, HostMetadataState.HEALTHY);

        Collection<HostMetadata> nodes = Arrays.asList(unhealhtyNode, healhtyNode);

        Map<String, Long> ascendingNodes = new LinkedHashMap<>();
        ascendingNodes.put(hostname1, 100L);
        ascendingNodes.put(hostname2, 110L);

        Map<String, Long> selectedNodes = underTest.selectNodes(ascendingNodes, nodes, 1);

        Assert.assertEquals(1, selectedNodes.size());
        Assert.assertEquals(hostname1, selectedNodes.keySet().stream().findFirst().get());
    }

    @Test
    public void testSelectNodesWhenHasOneUnhealthyNodeAndShouldSelectTwo() {

        String hostname1 = "10.0.0.1";
        String hostname2 = "10.0.0.2";
        String hostname3 = "10.0.0.3";

        HostMetadata unhealhtyNode = getHostMetadata(hostname1, HostMetadataState.UNHEALTHY);
        HostMetadata healhtyNode1 = getHostMetadata(hostname2, HostMetadataState.HEALTHY);
        HostMetadata healhtyNode2 = getHostMetadata(hostname3, HostMetadataState.HEALTHY);

        List<HostMetadata> nodes = Arrays.asList(unhealhtyNode, healhtyNode1, healhtyNode2);

        Map<String, Long> ascendingNodes = new LinkedHashMap<>();
        ascendingNodes.put(hostname1, 100L);
        ascendingNodes.put(hostname2, 110L);
        ascendingNodes.put(hostname3, 120L);

        Map<String, Long> selectedNodes = underTest.selectNodes(ascendingNodes, nodes, 2);

        Assert.assertEquals(2, selectedNodes.size());
        Assert.assertTrue(selectedNodes.keySet().containsAll(Arrays.asList(hostname1, hostname2)));
    }

    @Test
    public void testSelectNodesWhenHasThreeUnhealthyNodeAndShouldSelectTwo() {

        String hostname1 = "10.0.0.1";
        String hostname2 = "10.0.0.2";
        String hostname3 = "10.0.0.3";
        String hostname4 = "10.0.0.4";
        String hostname5 = "10.0.0.5";

        HostMetadata unhealhtyNode1 = getHostMetadata(hostname1, HostMetadataState.UNHEALTHY);
        HostMetadata unhealhtyNode2 = getHostMetadata(hostname2, HostMetadataState.UNHEALTHY);
        HostMetadata unhealhtyNode3 = getHostMetadata(hostname3, HostMetadataState.UNHEALTHY);
        HostMetadata healhtyNode1 = getHostMetadata(hostname4, HostMetadataState.HEALTHY);
        HostMetadata healhtyNode2 = getHostMetadata(hostname5, HostMetadataState.HEALTHY);

        List<HostMetadata> nodes = Arrays.asList(unhealhtyNode1, unhealhtyNode2, unhealhtyNode3, healhtyNode1, healhtyNode2);

        Map<String, Long> ascendingNodes = new LinkedHashMap<>();
        ascendingNodes.put(hostname1, 100L);
        ascendingNodes.put(hostname2, 110L);
        ascendingNodes.put(hostname3, 120L);
        ascendingNodes.put(hostname4, 130L);
        ascendingNodes.put(hostname5, 140L);

        Map<String, Long> selectedNodes = underTest.selectNodes(ascendingNodes, nodes, 2);

        Assert.assertEquals(2, selectedNodes.size());
        Assert.assertTrue(selectedNodes.keySet().containsAll(Arrays.asList(hostname1, hostname2)));
    }

    @Test
    public void testSelectNodesWhenHasOneUnhealthyNodeButNotInAscendingList() {

        String hostname1 = "10.0.0.1";
        String hostname2 = "10.0.0.2";
        String hostname3 = "10.0.0.3";

        HostMetadata unhealhtyNode1 = getHostMetadata(hostname1, HostMetadataState.UNHEALTHY);
        HostMetadata healhtyNode1 = getHostMetadata(hostname2, HostMetadataState.HEALTHY);
        HostMetadata healhtyNode2 = getHostMetadata(hostname3, HostMetadataState.HEALTHY);

        List<HostMetadata> nodes = Arrays.asList(unhealhtyNode1, healhtyNode1, healhtyNode2);

        Map<String, Long> ascendingNodes = new LinkedHashMap<>();
        ascendingNodes.put(hostname2, 110L);
        ascendingNodes.put(hostname3, 120L);

        Map<String, Long> selectedNodes = underTest.selectNodes(ascendingNodes, nodes, 1);

        Assert.assertEquals(1, selectedNodes.size());
        Assert.assertTrue(selectedNodes.keySet().contains(hostname2));
    }

    @Test
    public void testSelectNodesWhenHostNameShouldContainsInAscNodesAndNodes() {

        String hostname1 = "10.0.0.1";

        HostMetadata healhtyNode1 = getHostMetadata(hostname1, HostMetadataState.HEALTHY);

        List<HostMetadata> nodes = Collections.singletonList(healhtyNode1);

        Map<String, Long> ascendingNodes = new LinkedHashMap<>();
        ascendingNodes.put(hostname1, 100L);

        Map<String, Long> selectedNodes = underTest.selectNodes(ascendingNodes, nodes, 1);

        Assert.assertEquals(1, selectedNodes.size());
        Assert.assertTrue(selectedNodes.keySet().contains(hostname1));
    }

    @Test
    public void testVerifyNodeCountWithReplicationFactory() throws CloudbreakSecuritySetupException {

        String hostGroupName = "hostGroupName";
        String hostname = "hostname";
        String ipAddress = "192.18.256.1";
        int gatewayPort = 1234;
        String ambariName = "ambari-name";

        Map<String, List<String>> blueprintMap = new HashMap<>();
        blueprintMap.put(hostGroupName, Collections.singletonList("DATANODE"));

        Blueprint blueprint = new Blueprint();
        blueprint.setName(ambariName);
        blueprint.setAmbariName(ambariName);

        Cluster cluster = new Cluster();
        cluster.setAmbariIp(ipAddress);
        cluster.setBlueprint(blueprint);

        Stack stack = new Stack();
        stack.setGatewayPort(gatewayPort);
        stack.setId(100L);

        HostGroup hostGroup = new HostGroup();
        hostGroup.setName(hostGroupName);
        hostGroup.setHostMetadata(Sets.newHashSet(getHostMetadata(0L), getHostMetadata(1L), getHostMetadata(2L), getHostMetadata(3L)));

        AmbariClient ambariClient = mock(AmbariClient.class);

        HttpClientConfig config = new HttpClientConfig(ipAddress);
        when(tlsSecurityService.buildTLSClientConfigForPrimaryGateway(stack.getId(), cluster.getAmbariIp())).thenReturn(config);
        when(ambariClientProvider.getAmbariClient(config, stack.getGatewayPort(), cluster)).thenReturn(ambariClient);
        when(hostGroupService.getByClusterAndHostName(cluster, hostname)).thenReturn(hostGroup);
        when(ambariClient.getBlueprintMap(ambariName)).thenReturn(blueprintMap);
        when(configurationService.getConfiguration(ambariClient, hostGroupName)).thenReturn(Collections.singletonMap(ConfigParam.DFS_REPLICATION.key(), "3"));

        underTest.verifyNodeCount(stack, cluster, hostname);

        verify(configurationService, times(1)).getConfiguration(ambariClient, hostGroupName);
    }

    @Test
    public void testVerifyNodeCountWithoutReplicationFactory() throws CloudbreakSecuritySetupException {

        String hostGroupName = "hostGroupName";
        String hostname = "hostname";
        String ipAddress = "192.18.256.1";
        int gatewayPort = 1234;
        String ambariName = "ambari-name";

        Map<String, List<String>> blueprintMap = new HashMap<>();
        blueprintMap.put(hostGroupName, Collections.singletonList("NODEMANAGER"));

        Blueprint blueprint = new Blueprint();
        blueprint.setName(ambariName);
        blueprint.setAmbariName(ambariName);

        Cluster cluster = new Cluster();
        cluster.setAmbariIp(ipAddress);
        cluster.setBlueprint(blueprint);

        Stack stack = new Stack();
        stack.setGatewayPort(gatewayPort);
        stack.setId(100L);

        HostGroup hostGroup = new HostGroup();
        hostGroup.setName(hostGroupName);
        hostGroup.setHostMetadata(Collections.singleton(getHostMetadata(0L)));

        AmbariClient ambariClient = mock(AmbariClient.class);

        HttpClientConfig config = new HttpClientConfig(ipAddress);
        when(tlsSecurityService.buildTLSClientConfigForPrimaryGateway(stack.getId(), cluster.getAmbariIp())).thenReturn(config);
        when(ambariClientProvider.getAmbariClient(config, stack.getGatewayPort(), cluster)).thenReturn(ambariClient);
        when(hostGroupService.getByClusterAndHostName(cluster, hostname)).thenReturn(hostGroup);
        when(ambariClient.getBlueprintMap(ambariName)).thenReturn(blueprintMap);

        underTest.verifyNodeCount(stack, cluster, hostname);

        verify(configurationService, times(0)).getConfiguration(ambariClient, hostGroupName);
    }

    @Test
    public void testVerifyNodeCountWithValidationException() throws CloudbreakSecuritySetupException {

        String hostGroupName = "hostGroupName";
        String hostname = "hostname";
        String ipAddress = "192.18.256.1";
        int gatewayPort = 1234;
        String ambariName = "ambari-name";

        Map<String, List<String>> blueprintMap = new HashMap<>();
        blueprintMap.put(hostGroupName, Collections.singletonList("DATANODE"));

        Blueprint blueprint = new Blueprint();
        blueprint.setName(ambariName);
        blueprint.setAmbariName(ambariName);

        Cluster cluster = new Cluster();
        cluster.setAmbariIp(ipAddress);
        cluster.setBlueprint(blueprint);

        Stack stack = new Stack();
        stack.setGatewayPort(gatewayPort);
        stack.setId(100L);

        HostGroup hostGroup = new HostGroup();
        hostGroup.setName(hostGroupName);
        hostGroup.setHostMetadata(Collections.singleton(getHostMetadata(0L)));

        AmbariClient ambariClient = mock(AmbariClient.class);

        HttpClientConfig config = new HttpClientConfig(ipAddress);
        when(tlsSecurityService.buildTLSClientConfigForPrimaryGateway(stack.getId(), cluster.getAmbariIp())).thenReturn(config);
        when(ambariClientProvider.getAmbariClient(config, stack.getGatewayPort(), cluster)).thenReturn(ambariClient);
        when(hostGroupService.getByClusterAndHostName(cluster, hostname)).thenReturn(hostGroup);
        when(ambariClient.getBlueprintMap(ambariName)).thenReturn(blueprintMap);
        when(configurationService.getConfiguration(ambariClient, hostGroupName)).thenReturn(Collections.singletonMap(ConfigParam.DFS_REPLICATION.key(), "3"));

        thrown.expect(NotEnoughNodeException.class);
        thrown.expectMessage("There is not enough node to downscale. Check the replication factor and the ApplicationMaster occupation.");

        underTest.verifyNodeCount(stack, cluster, hostname);
    }

    protected HostMetadata getHostMetadata(Long id) {
        HostMetadata hostMetadata = new HostMetadata();
        hostMetadata.setId(id);
        return hostMetadata;
    }

    private HostMetadata getHostMetadata(String hostname2, HostMetadataState state) {
        HostMetadata healhtyNode = new HostMetadata();
        healhtyNode.setHostName(hostname2);
        healhtyNode.setHostMetadataState(state);
        return healhtyNode;
    }
}
