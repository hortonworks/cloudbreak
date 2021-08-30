package com.sequenceiq.cloudbreak.cm;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloudera.api.swagger.ClustersResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.cloudbreak.cluster.service.ClusterClientInitException;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerApiClientProvider;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerClientInitException;
import com.sequenceiq.cloudbreak.cm.client.retry.ClouderaManagerApiFactory;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.service.CloudbreakException;

@RunWith(MockitoJUnitRunner.class)
public class ClouderaManagerClusterDecommissionServiceTest {

    private static final int GATEWAY_PORT = 8080;

    private static final String USER = "admin";

    private static final String PASSWORD = "admin123";

    @Mock
    private ClouderaManagerApiClientProvider clouderaManagerApiClientProvider;

    @Mock
    private ClouderaManagerApiFactory clouderaManagerApiFactory;

    @Mock
    private ClouderaManagerDecomissioner clouderaManagerDecomissioner;

    @Mock
    private ApplicationContext applicationContext;

    @Mock
    private HttpClientConfig clientConfig;

    @Mock
    private ApiClient v31Client;

    @Mock
    private ApiClient v45Client;

    private Stack stack = createStack();

    @InjectMocks
    private ClouderaManagerClusterDecommissionService underTest;

    @Before
    public void before() {
        underTest = new ClouderaManagerClusterDecommissionService(stack, clientConfig);
        ReflectionTestUtils.setField(underTest, "clouderaManagerApiFactory", clouderaManagerApiFactory);
        ReflectionTestUtils.setField(underTest, "clouderaManagerApiClientProvider", clouderaManagerApiClientProvider);
        ReflectionTestUtils.setField(underTest, "clouderaManagerDecomissioner", clouderaManagerDecomissioner);
        ReflectionTestUtils.setField(underTest, "applicationContext", applicationContext);
        ReflectionTestUtils.setField(underTest, "v31Client", v31Client);
        ReflectionTestUtils.setField(underTest, "v45Client", v45Client);
    }

    @Test
    public void testInitApiClientShouldCreateTheApiClient() throws ClusterClientInitException, ClouderaManagerClientInitException {
        ReflectionTestUtils.setField(underTest, "v31Client", null);
        ReflectionTestUtils.setField(underTest, "v45Client", null);
        ApiClient v31Client = Mockito.mock(ApiClient.class);
        ApiClient v45Client = Mockito.mock(ApiClient.class);
        when(clouderaManagerApiClientProvider.getV31Client(GATEWAY_PORT, USER, PASSWORD, clientConfig)).thenReturn(v31Client);
        when(clouderaManagerApiClientProvider.getV45Client(GATEWAY_PORT, USER, PASSWORD, clientConfig)).thenReturn(v45Client);

        underTest.initApiClient();

        assertEquals(v31Client, ReflectionTestUtils.getField(underTest, "v31Client"));
        assertEquals(v45Client, ReflectionTestUtils.getField(underTest, "v45Client"));
        verify(clouderaManagerApiClientProvider).getV31Client(GATEWAY_PORT, USER, PASSWORD, clientConfig);
        verify(clouderaManagerApiClientProvider).getV45Client(GATEWAY_PORT, USER, PASSWORD, clientConfig);
    }

    @Test
    public void testVerifyNodesAreRemovable() {
        underTest.verifyNodesAreRemovable(stack, Collections.emptyList());

        verify(clouderaManagerDecomissioner).verifyNodesAreRemovable(stack, Collections.emptyList(), v31Client);
    }

    @Test
    public void testCollectDownscaleCandidates() {
        HostGroup hostGroup = new HostGroup();
        Integer scalingAdjustment = 1;
        Set<InstanceMetaData> instanceMetadatas = new HashSet<>();
        InstanceMetaData instanceMetaData1 = new InstanceMetaData();
        instanceMetaData1.setDiscoveryFQDN("host1");
        instanceMetaData1.setPrivateId(1L);
        instanceMetadatas.add(instanceMetaData1);
        InstanceMetaData instanceMetaData2 = new InstanceMetaData();
        instanceMetaData2.setDiscoveryFQDN("host2");
        instanceMetaData2.setPrivateId(2L);
        instanceMetadatas.add(instanceMetaData2);

        when(clouderaManagerDecomissioner.collectDownscaleCandidates(v31Client, stack, hostGroup, scalingAdjustment, instanceMetadatas))
                .thenReturn(instanceMetadatas);

        Set<InstanceMetaData> actual = underTest.collectDownscaleCandidates(hostGroup, scalingAdjustment, instanceMetadatas);

        assertEquals(instanceMetadatas, actual);
        verify(clouderaManagerDecomissioner).collectDownscaleCandidates(v31Client, stack, hostGroup, scalingAdjustment, instanceMetadatas);
    }

    @Test
    public void testCollectHostsToRemove() {
        HostGroup hostGroup = new HostGroup();
        Set<String> hostNames = Collections.emptySet();
        Map<String, InstanceMetaData> hosts = new HashMap<>();
        when(clouderaManagerDecomissioner.collectHostsToRemove(stack, hostGroup, hostNames, v31Client)).thenReturn(hosts);

        Map<String, InstanceMetaData> actual = underTest.collectHostsToRemove(hostGroup, hostNames);

        assertEquals(hosts, actual);
        verify(clouderaManagerDecomissioner).collectHostsToRemove(stack, hostGroup, hostNames, v31Client);
    }

    @Test
    public void testDecommissionClusterNodes() {
        Map<String, InstanceMetaData> hostsToRemove = new HashMap<>();
        Set<String> hosts = Set.of("host");
        when(clouderaManagerDecomissioner.decommissionNodes(stack, hostsToRemove, v31Client)).thenReturn(hosts);

        Set<String> actual = underTest.decommissionClusterNodes(hostsToRemove);

        assertEquals(hosts, actual);
        verify(clouderaManagerDecomissioner).decommissionNodes(stack, hostsToRemove, v31Client);
    }

    @Test
    public void testRemoveManagementServices() {
        underTest.removeManagementServices();

        verify(clouderaManagerDecomissioner).stopAndRemoveMgmtService(stack, v31Client);
    }

    @Test
    public void testDeleteHostFromCluster() {
        InstanceMetaData hostMetadata = new InstanceMetaData();

        underTest.deleteHostFromCluster(hostMetadata);

        verify(clouderaManagerDecomissioner).deleteHost(stack, hostMetadata, v31Client);
    }

    @Test
    public void testRestartStaleServices() throws CloudbreakException, ApiException {
        ClouderaManagerModificationService modificationService = Mockito.mock(ClouderaManagerModificationService.class);
        ClustersResourceApi clustersResourceApi = Mockito.mock(ClustersResourceApi.class);
        when(applicationContext.getBean(ClouderaManagerModificationService.class, stack, clientConfig)).thenReturn(modificationService);
        when(clouderaManagerApiFactory.getClustersResourceApi(v31Client)).thenReturn(clustersResourceApi);

        underTest.restartStaleServices(false);

        verify(applicationContext).getBean(ClouderaManagerModificationService.class, stack, clientConfig);
        verify(clouderaManagerApiFactory).getClustersResourceApi(v31Client);
        verify(modificationService).restartStaleServices(clustersResourceApi, false);
    }

    @Test
    public void testRemoveHostsFromClusterFailsWhenNoV45ClientAvailable() {
        ReflectionTestUtils.setField(underTest, "v45Client", null);
        ClusterClientInitException cloudbreakException = assertThrows(ClusterClientInitException.class,
                () -> underTest.removeHostsFromCluster(List.of(new InstanceMetaData())));

        assertEquals("V45 client is not initialized, bulk host removal is not supported", cloudbreakException.getMessage());
        verify(clouderaManagerDecomissioner, never()).removeHostsFromCluster(any(), anyList(), any());
    }

    @Test
    public void testRemoveHostsFromClusterWhenV45ClientAvailable() throws ClusterClientInitException {
        List<InstanceMetaData> hosts = List.of(new InstanceMetaData());
        underTest.removeHostsFromCluster(hosts);

        verify(clouderaManagerDecomissioner, times(1)).removeHostsFromCluster(eq(stack), eq(hosts), eq(v45Client));
    }

    private Stack createStack() {
        Stack stack = new Stack();
        stack.setCluster(createCluster());
        stack.setGatewayPort(GATEWAY_PORT);
        return stack;
    }

    private Cluster createCluster() {
        Cluster cluster = new Cluster();
        cluster.setCloudbreakUser(USER);
        cluster.setCloudbreakPassword(PASSWORD);
        return cluster;
    }
}