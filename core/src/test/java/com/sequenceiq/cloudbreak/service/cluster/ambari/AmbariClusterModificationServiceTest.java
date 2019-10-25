package com.sequenceiq.cloudbreak.service.cluster.ambari;

import static com.sequenceiq.cloudbreak.service.cluster.ambari.AmbariOperationType.UPSCALE_AMBARI_PROGRESS_STATE;
import static com.sequenceiq.cloudbreak.service.cluster.ambari.HostGroupAssociationBuilder.FQDN;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.PollingResult;
import com.sequenceiq.cloudbreak.service.cluster.AmbariClientRetryer;
import com.sequenceiq.cloudbreak.service.cluster.flow.AmbariOperationService;
import com.sequenceiq.cloudbreak.service.cluster.flow.recipe.RecipeEngine;
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.messages.CloudbreakMessagesService;

@RunWith(MockitoJUnitRunner.class)
public class AmbariClusterModificationServiceTest {

    @Mock
    private AmbariClientFactory clientFactory;

    @Mock
    private AmbariClusterConnectorPollingResultChecker ambariClusterConnectorPollingResultChecker;

    @Mock
    private AmbariOperationService ambariOperationService;

    @Mock
    private RecipeEngine recipeEngine;

    @Mock
    private HostGroupAssociationBuilder hostGroupAssociationBuilder;

    @Mock
    private CloudbreakMessagesService cloudbreakMessagesService;

    @Mock
    private CloudbreakEventService eventService;

    @Spy
    private AmbariRepositoryVersionService ambariRepositoryVersionService;

    @Mock
    private AmbariPollingServiceProvider ambariPollingServiceProvider;

    @Mock
    private AmbariClientRetryer ambariClientRetryer;

    @Mock
    private HostGroupService hostGroupService;

    @InjectMocks
    private AmbariClusterModificationService ambariClusterModificationService;

    @Test
    public void testRackUpdateIfNewAmbari() throws CloudbreakException, IOException, URISyntaxException {
        Stack stack = TestUtil.stack();
        Cluster cluster = TestUtil.cluster();
        stack.setCluster(cluster);

        AmbariClient ambariClient = mock(AmbariClient.class);
        when(ambariClientRetryer.getClusterHosts(any())).thenReturn(stack.getInstanceGroups().stream()
                .flatMap(instanceGroup -> instanceGroup.getAllInstanceMetaData().stream().map(InstanceMetaData::getDiscoveryFQDN))
                .filter(fqdn -> !fqdn.equals("test-3-1"))
                .collect(Collectors.toList()));
        when(clientFactory.getAmbariClient(any(), any())).thenReturn(ambariClient);

        ImmutablePair<PollingResult, Exception> pair = new ImmutablePair<>(PollingResult.SUCCESS, null);
        when(ambariOperationService.waitForOperations(eq(stack), eq(ambariClient), any(), eq(UPSCALE_AMBARI_PROGRESS_STATE))).thenReturn(pair);

        HostGroup hostGroup = cluster.getHostGroups().iterator().next();

        List<Map<String, String>> hostListForAssociation = new ArrayList<>();
        Map<String, String> host1Map = new HashMap<>();
        host1Map.put(FQDN, "test-1-1");
        host1Map.put("rack", "myrack");
        hostListForAssociation.add(host1Map);
        Map<String, String> host2Map = new HashMap<>();
        host2Map.put(FQDN, "test-2-1");
        host2Map.put("rack", "myrack");
        hostListForAssociation.add(host2Map);
        Map<String, String> host3Map = new HashMap<>();
        host3Map.put(FQDN, "test-3-1");
        host3Map.put("rack", "myrack");
        hostListForAssociation.add(host3Map);

        when(hostGroupService.getByClusterIdAndNameWithRecipes(eq(cluster.getId()), eq(hostGroup.getName()))).thenReturn(hostGroup);
        when(hostGroupAssociationBuilder.buildHostGroupAssociation(hostGroup)).thenReturn(hostListForAssociation);

        ambariClusterModificationService.upscaleCluster(stack, hostGroup.getName());

        Map<String, String> rackMap = new HashMap<>();
        rackMap.put("test-3-1", "myrack");

        verify(ambariClientRetryer, times(1))
                .addHostsAndRackInfoWithBlueprint(any(AmbariClient.class), eq(cluster.getBlueprint().getName()), eq(hostGroup.getName()), eq(rackMap));
        verify(ambariClientRetryer, times(0))
                .addHostsWithBlueprint(any(AmbariClient.class), anyString(), anyString(), any());
        verify(ambariClient, never()).updateRack(anyString(), anyString());
    }

    @Test
    public void testRackUpdateIfRackIsNullForAllCandidates() throws CloudbreakException, IOException, URISyntaxException {
        Stack stack = TestUtil.stack();
        Cluster cluster = TestUtil.cluster();
        stack.setCluster(cluster);

        AmbariClient ambariClient = mock(AmbariClient.class);
        when(ambariClientRetryer.getClusterHosts(any())).thenReturn(stack.getInstanceGroups().stream()
                .flatMap(instanceGroup -> instanceGroup.getAllInstanceMetaData().stream().map(InstanceMetaData::getDiscoveryFQDN))
                .filter(fqdn -> !fqdn.equals("test-2-1"))
                .filter(fqdn -> !fqdn.equals("test-3-1"))
                .collect(Collectors.toList()));
        when(clientFactory.getAmbariClient(any(), any())).thenReturn(ambariClient);

        ImmutablePair<PollingResult, Exception> pair = new ImmutablePair<>(PollingResult.SUCCESS, null);
        when(ambariOperationService.waitForOperations(eq(stack), eq(ambariClient), any(), eq(UPSCALE_AMBARI_PROGRESS_STATE))).thenReturn(pair);

        HostGroup hostGroup = cluster.getHostGroups().iterator().next();

        List<Map<String, String>> hostListForAssociation = new ArrayList<>();
        Map<String, String> host1Map = new HashMap<>();
        host1Map.put(FQDN, "test-1-1");
        hostListForAssociation.add(host1Map);
        Map<String, String> host2Map = new HashMap<>();
        host2Map.put(FQDN, "test-2-1");
        hostListForAssociation.add(host2Map);
        Map<String, String> host3Map = new HashMap<>();
        host3Map.put(FQDN, "test-3-1");
        hostListForAssociation.add(host3Map);

        when(hostGroupService.getByClusterIdAndNameWithRecipes(eq(cluster.getId()), eq(hostGroup.getName()))).thenReturn(hostGroup);
        when(hostGroupAssociationBuilder.buildHostGroupAssociation(hostGroup)).thenReturn(hostListForAssociation);

        ambariClusterModificationService.upscaleCluster(stack, hostGroup.getName());

        List<String> candidateHostNames = List.of("test-2-1", "test-3-1");

        verify(ambariClientRetryer, times(0))
                .addHostsAndRackInfoWithBlueprint(any(AmbariClient.class), anyString(), anyString(), any());
        verify(ambariClientRetryer, times(1))
                .addHostsWithBlueprint(any(AmbariClient.class), eq(cluster.getBlueprint().getName()), eq(hostGroup.getName()), eq(candidateHostNames));
        verify(ambariClient, never()).updateRack(anyString(), anyString());
    }

    @Test
    public void testRackUpdateIfRackIsNullForSomeOfTheCandidates() throws CloudbreakException, IOException, URISyntaxException {
        Stack stack = TestUtil.stack();
        Cluster cluster = TestUtil.cluster();
        stack.setCluster(cluster);

        AmbariClient ambariClient = mock(AmbariClient.class);
        when(ambariClientRetryer.getClusterHosts(any())).thenReturn(stack.getInstanceGroups().stream()
                .flatMap(instanceGroup -> instanceGroup.getAllInstanceMetaData().stream().map(InstanceMetaData::getDiscoveryFQDN))
                .filter(fqdn -> !fqdn.equals("test-2-1"))
                .filter(fqdn -> !fqdn.equals("test-3-1"))
                .collect(Collectors.toList()));
        when(clientFactory.getAmbariClient(any(), any())).thenReturn(ambariClient);

        ImmutablePair<PollingResult, Exception> pair = new ImmutablePair<>(PollingResult.SUCCESS, null);
        when(ambariOperationService.waitForOperations(eq(stack), eq(ambariClient), any(), eq(UPSCALE_AMBARI_PROGRESS_STATE))).thenReturn(pair);

        HostGroup hostGroup = cluster.getHostGroups().iterator().next();

        List<Map<String, String>> hostListForAssociation = new ArrayList<>();
        Map<String, String> host1Map = new HashMap<>();
        host1Map.put(FQDN, "test-1-1");
        host1Map.put("rack", "myrack");
        hostListForAssociation.add(host1Map);
        Map<String, String> host2Map = new HashMap<>();
        host2Map.put(FQDN, "test-2-1");
        hostListForAssociation.add(host2Map);
        Map<String, String> host3Map = new HashMap<>();
        host3Map.put(FQDN, "test-3-1");
        host3Map.put("rack", "myrack");
        hostListForAssociation.add(host3Map);

        when(hostGroupService.getByClusterIdAndNameWithRecipes(eq(cluster.getId()), eq(hostGroup.getName()))).thenReturn(hostGroup);
        when(hostGroupAssociationBuilder.buildHostGroupAssociation(hostGroup)).thenReturn(hostListForAssociation);

        ambariClusterModificationService.upscaleCluster(stack, hostGroup.getName());

        Map<String, String> rackMap = new HashMap<>();
        rackMap.put("test-2-1", "/default-rack");
        rackMap.put("test-3-1", "myrack");

        verify(ambariClientRetryer, times(1))
                .addHostsAndRackInfoWithBlueprint(any(AmbariClient.class), eq(cluster.getBlueprint().getName()), eq(hostGroup.getName()), eq(rackMap));
        verify(ambariClientRetryer, times(0))
                .addHostsWithBlueprint(any(AmbariClient.class), anyString(), anyString(), any());
        verify(ambariClient, never()).updateRack(anyString(), anyString());
    }
}