package com.sequenceiq.cloudbreak.cm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;

@ExtendWith(MockitoExtension.class)
class ClouderaManagerClusterDecommissionServiceTest {

    private static final int GATEWAY_PORT = 8080;

    private static final String USER = "admin";

    private static final String PASSWORD = "admin123";

    @Mock
    private ClouderaManagerApiClientProvider clouderaManagerApiClientProvider;

    @Mock
    private ClouderaManagerApiFactory clouderaManagerApiFactory;

    @Mock
    private ClouderaManagerDecommissioner clouderaManagerDecommissioner;

    @Mock
    private ApplicationContext applicationContext;

    @Mock
    private HttpClientConfig clientConfig;

    @Mock
    private ApiClient v31Client;

    @Mock
    private ApiClient v51Client;

    @Mock
    private ApiClient v53Client;

    @Mock
    private ApiClient v45Client;

    private StackDtoDelegate stack = createStack();

    @InjectMocks
    private ClouderaManagerClusterDecommissionService underTest;

    @BeforeEach
    void before() {
        underTest = new ClouderaManagerClusterDecommissionService(stack, clientConfig);
        ReflectionTestUtils.setField(underTest, "clouderaManagerApiFactory", clouderaManagerApiFactory);
        ReflectionTestUtils.setField(underTest, "clouderaManagerApiClientProvider", clouderaManagerApiClientProvider);
        ReflectionTestUtils.setField(underTest, "clouderaManagerDecommissioner", clouderaManagerDecommissioner);
        ReflectionTestUtils.setField(underTest, "applicationContext", applicationContext);
        ReflectionTestUtils.setField(underTest, "v31Client", v31Client);
        ReflectionTestUtils.setField(underTest, "v45Client", v45Client);
        ReflectionTestUtils.setField(underTest, "v51Client", v51Client);
        ReflectionTestUtils.setField(underTest, "v53Client", v53Client);
    }

    @Test
    void testInitApiClientShouldCreateTheApiClient() throws ClusterClientInitException, ClouderaManagerClientInitException {
        ReflectionTestUtils.setField(underTest, "v31Client", null);
        ReflectionTestUtils.setField(underTest, "v45Client", null);
        ApiClient v31Client = mock(ApiClient.class);
        ApiClient v45Client = mock(ApiClient.class);
        when(clouderaManagerApiClientProvider.getV31Client(GATEWAY_PORT, USER, PASSWORD, clientConfig)).thenReturn(v31Client);
        when(clouderaManagerApiClientProvider.getV45Client(GATEWAY_PORT, USER, PASSWORD, clientConfig)).thenReturn(v45Client);

        underTest.initApiClient();

        assertEquals(v31Client, ReflectionTestUtils.getField(underTest, "v31Client"));
        assertEquals(v45Client, ReflectionTestUtils.getField(underTest, "v45Client"));
        verify(clouderaManagerApiClientProvider).getV31Client(GATEWAY_PORT, USER, PASSWORD, clientConfig);
        verify(clouderaManagerApiClientProvider).getV45Client(GATEWAY_PORT, USER, PASSWORD, clientConfig);
    }

    @Test
    void testVerifyNodesAreRemovable() {
        underTest.verifyNodesAreRemovable(stack, Collections.emptyList());

        verify(clouderaManagerDecommissioner).verifyNodesAreRemovable(stack, Collections.emptyList(), v31Client);
    }

    @Test
    void testCollectDownscaleCandidates() {
        String hostGroupName = "hgName";
        Integer scalingAdjustment = 1;
        Set<InstanceMetadataView> instanceMetadatas = new HashSet<>();
        InstanceMetaData instanceMetaData1 = new InstanceMetaData();
        instanceMetaData1.setDiscoveryFQDN("host1");
        instanceMetaData1.setPrivateId(1L);
        instanceMetadatas.add(instanceMetaData1);
        InstanceMetaData instanceMetaData2 = new InstanceMetaData();
        instanceMetaData2.setDiscoveryFQDN("host2");
        instanceMetaData2.setPrivateId(2L);
        instanceMetadatas.add(instanceMetaData2);

        when(clouderaManagerDecommissioner.collectDownscaleCandidates(v31Client, stack, hostGroupName, scalingAdjustment, instanceMetadatas))
                .thenReturn(instanceMetadatas);

        Set<InstanceMetadataView> actual = underTest.collectDownscaleCandidates(hostGroupName, scalingAdjustment, instanceMetadatas);

        assertEquals(instanceMetadatas, actual);
        verify(clouderaManagerDecommissioner).collectDownscaleCandidates(v31Client, stack, hostGroupName, scalingAdjustment, instanceMetadatas);
    }

    @Test
    void testCollectHostsToRemove() {
        String hostGroupName = "hgName";
        Set<String> hostNames = Collections.emptySet();
        Map<String, InstanceMetadataView> hosts = new HashMap<>();
        when(clouderaManagerDecommissioner.collectHostsToRemove(stack, hostGroupName, hostNames, v31Client)).thenReturn(hosts);

        Map<String, InstanceMetadataView> actual = underTest.collectHostsToRemove(hostGroupName, hostNames);

        assertEquals(hosts, actual);
        verify(clouderaManagerDecommissioner).collectHostsToRemove(stack, hostGroupName, hostNames, v31Client);
    }

    @Test
    void testDecommissionClusterNodes() {
        Map<String, InstanceMetadataView> hostsToRemove = new HashMap<>();
        Set<String> hosts = Set.of("host");
        when(clouderaManagerDecommissioner.decommissionNodes(stack, hostsToRemove, v31Client)).thenReturn(hosts);

        Set<String> actual = underTest.decommissionClusterNodes(hostsToRemove);

        assertEquals(hosts, actual);
        verify(clouderaManagerDecommissioner).decommissionNodes(stack, hostsToRemove, v31Client);
    }

    @Test
    void testRemoveManagementServices() {
        underTest.removeManagementServices();

        verify(clouderaManagerDecommissioner).stopAndRemoveMgmtService(stack, v31Client);
    }

    @Test
    void testDeleteHostFromCluster() {
        InstanceMetaData hostMetadata = new InstanceMetaData();

        underTest.deleteHostFromCluster(hostMetadata);

        verify(clouderaManagerDecommissioner).deleteHost(stack, hostMetadata, v31Client);
    }

    @Test
    void testRestartStaleServices() throws CloudbreakException, ApiException {
        ClouderaManagerModificationService modificationService = mock(ClouderaManagerModificationService.class);
        ClustersResourceApi clustersResourceApi = mock(ClustersResourceApi.class);
        when(applicationContext.getBean(ClouderaManagerModificationService.class, stack, clientConfig)).thenReturn(modificationService);
        when(clouderaManagerApiFactory.getClustersResourceApi(v31Client)).thenReturn(clustersResourceApi);

        underTest.restartStaleServices(false);

        verify(applicationContext).getBean(ClouderaManagerModificationService.class, stack, clientConfig);
        verify(clouderaManagerApiFactory).getClustersResourceApi(v31Client);
        verify(modificationService).restartStaleServices(clustersResourceApi, false);
    }

    @Test
    void testRemoveHostsFromClusterFailsWhenNoV45ClientAvailable() {
        ReflectionTestUtils.setField(underTest, "v45Client", null);
        ClusterClientInitException cloudbreakException = assertThrows(ClusterClientInitException.class,
                () -> underTest.removeHostsFromCluster(List.of(new InstanceMetaData())));

        assertEquals("V45 client is not initialized, bulk host removal is not supported", cloudbreakException.getMessage());
        verify(clouderaManagerDecommissioner, never()).removeHostsFromCluster(any(), anyList(), any());
    }

    @Test
    void testRemoveHostsFromClusterWhenV45ClientAvailable() throws ClusterClientInitException {
        List<InstanceMetadataView> hosts = List.of(new InstanceMetaData());
        underTest.removeHostsFromCluster(hosts);

        verify(clouderaManagerDecommissioner, times(1)).removeHostsFromCluster(eq(stack), eq(hosts), eq(v45Client));
    }

    @Test
    void stopRolesOnHostsTest() throws CloudbreakException {
        Set<String> hosts = Set.of("host1", "host2");
        underTest.stopRolesOnHosts(hosts, true);
        verify(clouderaManagerDecommissioner, times(1)).stopRolesOnHosts(stack, v53Client, v51Client, hosts, true);
    }

    private Stack createStack() {
        Stack stack = new Stack();
        stack.setCluster(createCluster());
        stack.setGatewayPort(GATEWAY_PORT);
        return stack;
    }

    private Cluster createCluster() {
        Cluster cluster = new Cluster();
        cluster.setCloudbreakClusterManagerUser(USER);
        cluster.setCloudbreakClusterManagerPassword(PASSWORD);
        return cluster;
    }
}