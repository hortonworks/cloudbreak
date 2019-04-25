package com.sequenceiq.cloudbreak.ambari;

import static com.sequenceiq.cloudbreak.ambari.AmbariOperationType.UPSCALE_AMBARI_PROGRESS_STATE;
import static com.sequenceiq.cloudbreak.ambari.HostGroupAssociationBuilder.FQDN;
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
import java.util.Arrays;
import java.util.Collections;
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
import com.sequenceiq.cloudbreak.ambari.flow.AmbariOperationService;
import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.cloudbreak.cluster.service.ClusterConnectorPollingResultChecker;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostMetadata;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.polling.PollingResult;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.event.CloudbreakEventService;

@RunWith(MockitoJUnitRunner.class)
public class AmbariClusterModificationServiceTest {

    @Mock
    private ClusterConnectorPollingResultChecker clusterConnectorPollingResultChecker;

    @Mock
    private AmbariOperationService ambariOperationService;

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
    private AmbariClient ambariClient;

    private Stack stack = TestUtil.stack();

    private HttpClientConfig clientConfig = new HttpClientConfig("1.1.1.1");

    @InjectMocks
    private AmbariClusterModificationService ambariClusterModificationService = new AmbariClusterModificationService(stack, clientConfig);

    @Test
    public void testRackUpdate() throws CloudbreakException, IOException, URISyntaxException {
        Cluster cluster = TestUtil.cluster();
        stack.setCluster(cluster);

        when(ambariClient.getClusterHosts()).thenReturn(cluster.getHostGroups().stream()
                .flatMap(hostGroup -> hostGroup.getHostNames().stream())
                .collect(Collectors.toList()));

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

        when(hostGroupAssociationBuilder.buildHostGroupAssociation(hostGroup, Collections.emptyList())).thenReturn(hostListForAssociation);

        ambariClusterModificationService.upscaleCluster(hostGroup, hostMetadataList, Collections.emptyList());

        Map<String, String> rackMap = new HashMap<>();
        rackMap.put("host1", "myrack");
        rackMap.put("host2", "myrack");

        verify(ambariClient, times(1)).addHostsAndRackInfoWithBlueprint(eq(cluster.getBlueprint().getName()), eq(hostGroup.getName()), eq(rackMap));
        verify(ambariClient, never()).updateRack(anyString(), anyString());
    }

    @Test
    public void testRackUpdateIfNewAmbari() throws CloudbreakException, IOException, URISyntaxException {
        Cluster cluster = TestUtil.cluster();
        stack.setCluster(cluster);

        when(ambariClient.getClusterHosts()).thenReturn(cluster.getHostGroups().stream()
                .flatMap(hostGroup -> hostGroup.getHostNames().stream())
                .collect(Collectors.toList()));

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

        when(hostGroupAssociationBuilder.buildHostGroupAssociation(hostGroup, Collections.emptyList())).thenReturn(hostListForAssociation);

        ambariClusterModificationService.upscaleCluster(hostGroup, hostMetadataList, Collections.emptyList());

        Map<String, String> rackMap = new HashMap<>();
        rackMap.put("host1", "myrack");
        rackMap.put("host2", "myrack");

        verify(ambariClient, times(1)).addHostsAndRackInfoWithBlueprint(eq(cluster.getBlueprint().getName()), eq(hostGroup.getName()), eq(rackMap));
        verify(ambariClient, never()).updateRack(anyString(), anyString());
    }

    @Test
    public void testRackUpdateIfRackIsNull() throws CloudbreakException, IOException, URISyntaxException {
        Cluster cluster = TestUtil.cluster();
        stack.setCluster(cluster);

        when(ambariClient.getClusterHosts()).thenReturn(cluster.getHostGroups().stream()
                .flatMap(hostGroup -> hostGroup.getHostNames().stream())
                .collect(Collectors.toList()));

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

        when(hostGroupAssociationBuilder.buildHostGroupAssociation(hostGroup, Collections.emptyList())).thenReturn(hostListForAssociation);

        ambariClusterModificationService.upscaleCluster(hostGroup, hostMetadataList, Collections.emptyList());

        Map<String, String> rackMap = new HashMap<>();
        rackMap.put("host1", "/default-rack");
        rackMap.put("host2", "/default-rack");

        verify(ambariClient, times(1)).addHostsAndRackInfoWithBlueprint(eq(cluster.getBlueprint().getName()), eq(hostGroup.getName()), eq(rackMap));
        verify(ambariClient, never()).updateRack(anyString(), anyString());
    }
}