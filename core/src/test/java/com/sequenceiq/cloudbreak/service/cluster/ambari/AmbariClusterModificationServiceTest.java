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

import java.util.ArrayList;
import java.util.Arrays;
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
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostMetadata;
import com.sequenceiq.cloudbreak.repository.HostMetadataRepository;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.PollingResult;
import com.sequenceiq.cloudbreak.service.cluster.ClusterConnectorPollingResultChecker;
import com.sequenceiq.cloudbreak.service.cluster.flow.AmbariOperationService;
import com.sequenceiq.cloudbreak.service.cluster.flow.recipe.RecipeEngine;
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService;
import com.sequenceiq.cloudbreak.service.messages.CloudbreakMessagesService;

import groovyx.net.http.HttpResponseException;

@RunWith(MockitoJUnitRunner.class)
public class AmbariClusterModificationServiceTest {

    @Mock
    private AmbariClientFactory clientFactory;

    @Mock
    private ClusterConnectorPollingResultChecker clusterConnectorPollingResultChecker;

    @Mock
    private AmbariOperationService ambariOperationService;

    @Mock
    private RecipeEngine recipeEngine;

    @Mock
    private HostMetadataRepository hostMetadataRepository;

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

    @InjectMocks
    private AmbariClusterModificationService ambariClusterModificationService;

    @Test
    public void testRackUpdate() throws CloudbreakException, HttpResponseException {
        Stack stack = TestUtil.stack();
        Cluster cluster = TestUtil.cluster();
        stack.setCluster(cluster);

        AmbariClient ambariClient = mock(AmbariClient.class);
        when(ambariClient.getClusterHosts()).thenReturn(cluster.getHostGroups().stream()
                .flatMap(hostGroup -> hostGroup.getHostNames().stream())
                .collect(Collectors.toList()));
        when(clientFactory.getAmbariClient(any(Stack.class), any())).thenReturn(ambariClient);

        ImmutablePair<PollingResult, Exception> pair = new ImmutablePair<>(PollingResult.SUCCESS, null);
        when(ambariOperationService.waitForOperations(eq(stack), eq(ambariClient), any(), eq(UPSCALE_AMBARI_PROGRESS_STATE))).thenReturn(pair);

        HostGroup hostGroup = cluster.getHostGroups().iterator().next();

        HostMetadata firstHostMetadata = mock(HostMetadata.class);
        when(firstHostMetadata.getHostName()).thenReturn("host1");
        HostMetadata secondHostMetadata = mock(HostMetadata.class);
        when(secondHostMetadata.getHostName()).thenReturn("host2");
        List<HostMetadata> hostMetadataList = Arrays.asList(firstHostMetadata, secondHostMetadata);

        List<Map<String, String>> hostListForAssociation = new ArrayList<>();
        Map<String, String> host1Map = new HashMap<>();
        host1Map.put(FQDN, "host1");
        host1Map.put("rack", "myrack");
        hostListForAssociation.add(host1Map);
        Map<String, String> host2Map = new HashMap<>();
        host2Map.put(FQDN, "host2");
        host2Map.put("rack", "myrack");
        hostListForAssociation.add(host2Map);

        when(hostGroupAssociationBuilder.buildHostGroupAssociation(hostGroup)).thenReturn(hostListForAssociation);

        ambariClusterModificationService.upscaleCluster(stack, hostGroup, hostMetadataList);

        Map<String, String> rackMap = new HashMap<>();
        rackMap.put("host1", "myrack");
        rackMap.put("host2", "myrack");

        verify(ambariClient, times(1)).addHostsAndRackInfoWithBlueprint(eq(cluster.getClusterDefinition().getName()), eq(hostGroup.getName()), eq(rackMap));
        verify(ambariClient, never()).updateRack(anyString(), anyString());
    }

    @Test
    public void testRackUpdateIfNewAmbari() throws CloudbreakException, HttpResponseException {
        Stack stack = TestUtil.stack();
        Cluster cluster = TestUtil.cluster();
        stack.setCluster(cluster);

        AmbariClient ambariClient = mock(AmbariClient.class);
        when(ambariClient.getClusterHosts()).thenReturn(cluster.getHostGroups().stream()
                .flatMap(hostGroup -> hostGroup.getHostNames().stream())
                .collect(Collectors.toList()));
        when(clientFactory.getAmbariClient(any(Stack.class), any())).thenReturn(ambariClient);

        ImmutablePair<PollingResult, Exception> pair = new ImmutablePair<>(PollingResult.SUCCESS, null);
        when(ambariOperationService.waitForOperations(eq(stack), eq(ambariClient), any(), eq(UPSCALE_AMBARI_PROGRESS_STATE))).thenReturn(pair);

        HostGroup hostGroup = cluster.getHostGroups().iterator().next();

        HostMetadata firstHostMetadata = mock(HostMetadata.class);
        when(firstHostMetadata.getHostName()).thenReturn("host1");
        HostMetadata secondHostMetadata = mock(HostMetadata.class);
        when(secondHostMetadata.getHostName()).thenReturn("host2");
        List<HostMetadata> hostMetadataList = Arrays.asList(firstHostMetadata, secondHostMetadata);

        List<Map<String, String>> hostListForAssociation = new ArrayList<>();
        Map<String, String> host1Map = new HashMap<>();
        host1Map.put(FQDN, "host1");
        host1Map.put("rack", "myrack");
        hostListForAssociation.add(host1Map);
        Map<String, String> host2Map = new HashMap<>();
        host2Map.put(FQDN, "host2");
        host2Map.put("rack", "myrack");
        hostListForAssociation.add(host2Map);

        when(hostGroupAssociationBuilder.buildHostGroupAssociation(hostGroup)).thenReturn(hostListForAssociation);

        ambariClusterModificationService.upscaleCluster(stack, hostGroup, hostMetadataList);

        Map<String, String> rackMap = new HashMap<>();
        rackMap.put("host1", "myrack");
        rackMap.put("host2", "myrack");

        verify(ambariClient, times(1)).addHostsAndRackInfoWithBlueprint(eq(cluster.getClusterDefinition().getName()), eq(hostGroup.getName()), eq(rackMap));
        verify(ambariClient, never()).updateRack(anyString(), anyString());
    }

    @Test
    public void testRackUpdateIfRackIsNull() throws CloudbreakException, HttpResponseException {
        Stack stack = TestUtil.stack();
        Cluster cluster = TestUtil.cluster();
        stack.setCluster(cluster);

        AmbariClient ambariClient = mock(AmbariClient.class);
        when(ambariClient.getClusterHosts()).thenReturn(cluster.getHostGroups().stream()
                .flatMap(hostGroup -> hostGroup.getHostNames().stream())
                .collect(Collectors.toList()));
        when(clientFactory.getAmbariClient(any(Stack.class), any())).thenReturn(ambariClient);

        ImmutablePair<PollingResult, Exception> pair = new ImmutablePair<>(PollingResult.SUCCESS, null);
        when(ambariOperationService.waitForOperations(eq(stack), eq(ambariClient), any(), eq(UPSCALE_AMBARI_PROGRESS_STATE))).thenReturn(pair);

        HostGroup hostGroup = cluster.getHostGroups().iterator().next();

        HostMetadata firstHostMetadata = mock(HostMetadata.class);
        when(firstHostMetadata.getHostName()).thenReturn("host1");
        HostMetadata secondHostMetadata = mock(HostMetadata.class);
        when(secondHostMetadata.getHostName()).thenReturn("host2");
        List<HostMetadata> hostMetadataList = Arrays.asList(firstHostMetadata, secondHostMetadata);

        List<Map<String, String>> hostListForAssociation = new ArrayList<>();
        Map<String, String> host1Map = new HashMap<>();
        host1Map.put(FQDN, "host1");
        hostListForAssociation.add(host1Map);
        Map<String, String> host2Map = new HashMap<>();
        host2Map.put(FQDN, "host2");
        hostListForAssociation.add(host2Map);

        when(hostGroupAssociationBuilder.buildHostGroupAssociation(hostGroup)).thenReturn(hostListForAssociation);

        ambariClusterModificationService.upscaleCluster(stack, hostGroup, hostMetadataList);

        Map<String, String> rackMap = new HashMap<>();
        rackMap.put("host1", "/default-rack");
        rackMap.put("host2", "/default-rack");

        verify(ambariClient, times(1)).addHostsAndRackInfoWithBlueprint(eq(cluster.getClusterDefinition().getName()), eq(hostGroup.getName()), eq(rackMap));
        verify(ambariClient, never()).updateRack(anyString(), anyString());
    }
}