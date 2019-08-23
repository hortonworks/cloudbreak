package com.sequenceiq.cloudbreak.cm;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
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
import com.cloudera.api.swagger.MgmtServiceResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.cloudbreak.cluster.service.ClusterClientInitException;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerClientFactory;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerClientInitException;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostMetadata;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.service.CloudbreakException;

@RunWith(MockitoJUnitRunner.class)
public class ClouderaManagerClusterDecomissionServiceTest {

    private static final int GATEWAY_PORT = 8080;

    private static final String USER = "admin";

    private static final String PASSWORD = "admin123";

    @Mock
    private ClouderaManagerClientFactory clouderaManagerClientFactory;

    @Mock
    private ClouderaManagerDecomissioner clouderaManagerDecomissioner;

    @Mock
    private ApplicationContext applicationContext;

    @Mock
    private HttpClientConfig clientConfig;

    @Mock
    private ApiClient apiClient;

    private Stack stack = createStack();

    @InjectMocks
    private ClouderaManagerClusterDecomissionService underTest;

    @Before
    public void before() {
        underTest = new ClouderaManagerClusterDecomissionService(stack, clientConfig);
        ReflectionTestUtils.setField(underTest, "clouderaManagerClientFactory", clouderaManagerClientFactory);
        ReflectionTestUtils.setField(underTest, "clouderaManagerDecomissioner", clouderaManagerDecomissioner);
        ReflectionTestUtils.setField(underTest, "applicationContext", applicationContext);
        ReflectionTestUtils.setField(underTest, "client", apiClient);
    }

    @Test
    public void testInitApiClientShouldCreateTheApiClient() throws ClusterClientInitException, ClouderaManagerClientInitException {
        ReflectionTestUtils.setField(underTest, "client", null);
        ApiClient client = Mockito.mock(ApiClient.class);
        when(clouderaManagerClientFactory.getClient(GATEWAY_PORT, USER, PASSWORD, clientConfig)).thenReturn(client);

        underTest.initApiClient();

        assertEquals(client, ReflectionTestUtils.getField(underTest, "client"));
        verify(clouderaManagerClientFactory).getClient(GATEWAY_PORT, USER, PASSWORD, clientConfig);
    }

    @Test
    public void testVerifyNodesAreRemovable() {
        Multimap<Long, HostMetadata> hostGroupWithInstances = ArrayListMultimap.create();
        Set<HostGroup> hostGroups = Collections.emptySet();

        underTest.verifyNodesAreRemovable(hostGroupWithInstances, hostGroups, 0, Collections.emptyList());

        verify(clouderaManagerDecomissioner).verifyNodesAreRemovable(stack, hostGroupWithInstances, hostGroups, apiClient);
    }

    @Test
    public void testCollectDownscaleCandidates() {
        HostGroup hostGroup = new HostGroup();
        Integer scalingAdjustment = 1;
        int defaultRootVolumeSize = 2;
        Set<InstanceMetaData> instanceMetaDatasInStack = Collections.emptySet();
        Set<String> candidates = Set.of("host1", "host2");
        when(clouderaManagerDecomissioner.collectDownscaleCandidates(apiClient, stack, hostGroup, scalingAdjustment, defaultRootVolumeSize,
                instanceMetaDatasInStack)).thenReturn(candidates);

        Set<String> actual = underTest.collectDownscaleCandidates(hostGroup, scalingAdjustment, defaultRootVolumeSize, instanceMetaDatasInStack);

        assertEquals(candidates, actual);
        verify(clouderaManagerDecomissioner).collectDownscaleCandidates(apiClient, stack, hostGroup, scalingAdjustment, defaultRootVolumeSize,
                instanceMetaDatasInStack);
    }

    @Test
    public void testCollectHostsToRemove() {
        HostGroup hostGroup = new HostGroup();
        Set<String> hostNames = Collections.emptySet();
        Map<String, HostMetadata> hosts = new HashMap<>();
        when(clouderaManagerDecomissioner.collectHostsToRemove(stack, hostGroup, hostNames, apiClient)).thenReturn(hosts);

        Map<String, HostMetadata> actual = underTest.collectHostsToRemove(hostGroup, hostNames);

        assertEquals(hosts, actual);
        verify(clouderaManagerDecomissioner).collectHostsToRemove(stack, hostGroup, hostNames, apiClient);
    }

    @Test
    public void testDecommissionClusterNodes() {
        Map<String, HostMetadata> hostsToRemove = new HashMap<>();
        Set<HostMetadata> hosts = Set.of(new HostMetadata());
        when(clouderaManagerDecomissioner.decommissionNodes(stack, hostsToRemove, apiClient)).thenReturn(hosts);

        Set<HostMetadata> actual = underTest.decommissionClusterNodes(hostsToRemove);

        assertEquals(hosts, actual);
        verify(clouderaManagerDecomissioner).decommissionNodes(stack, hostsToRemove, apiClient);
    }

    @Test
    public void testRemoveManagementServices() {
        underTest.removeManagementServices();

        verify(clouderaManagerDecomissioner).stopAndRemoveMgmtService(stack, apiClient);
    }

    @Test
    public void testDeleteHostFromCluster() {
        HostMetadata hostMetadata = new HostMetadata();

        underTest.deleteHostFromCluster(hostMetadata);

        verify(clouderaManagerDecomissioner).deleteHost(stack, hostMetadata, apiClient);
    }

    @Test
    public void testRestartStaleServices() throws CloudbreakException, ApiException {
        ClouderaManagerModificationService modificationService = Mockito.mock(ClouderaManagerModificationService.class);
        MgmtServiceResourceApi mgmtServiceResourceApi = Mockito.mock(MgmtServiceResourceApi.class);
        ClustersResourceApi clustersResourceApi = Mockito.mock(ClustersResourceApi.class);
        when(applicationContext.getBean(ClouderaManagerModificationService.class, stack, clientConfig)).thenReturn(modificationService);
        when(clouderaManagerClientFactory.getMgmtServiceResourceApi(apiClient)).thenReturn(mgmtServiceResourceApi);
        when(clouderaManagerClientFactory.getClustersResourceApi(apiClient)).thenReturn(clustersResourceApi);

        underTest.restartStaleServices();

        verify(applicationContext).getBean(ClouderaManagerModificationService.class, stack, clientConfig);
        verify(clouderaManagerClientFactory).getMgmtServiceResourceApi(apiClient);
        verify(clouderaManagerClientFactory).getClustersResourceApi(apiClient);
        verify(modificationService).restartStaleServices(mgmtServiceResourceApi, clustersResourceApi);
    }

    private Stack createStack() {
        Stack stack = new Stack();
        stack.setCluster(createCluster());
        stack.setGatewayPort(GATEWAY_PORT);
        return stack;
    }

    private Cluster createCluster() {
        Cluster cluster = new Cluster();
        cluster.setCloudbreakAmbariUser(USER);
        cluster.setCloudbreakAmbariPassword(PASSWORD);
        return cluster;
    }
}