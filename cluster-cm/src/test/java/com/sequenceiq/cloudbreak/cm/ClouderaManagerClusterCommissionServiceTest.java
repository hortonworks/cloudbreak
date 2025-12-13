package com.sequenceiq.cloudbreak.cm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

import com.cloudera.api.swagger.client.ApiClient;
import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.cloudbreak.cluster.service.ClusterClientInitException;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerApiClientProvider;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerClientInitException;
import com.sequenceiq.cloudbreak.cm.client.retry.ClouderaManagerApiFactory;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;

@ExtendWith(MockitoExtension.class)
class ClouderaManagerClusterCommissionServiceTest {

    private static final int GATEWAY_PORT = 8080;

    private static final String USER = "admin";

    private static final String PASSWORD = "admin123";

    @Mock
    private ClouderaManagerApiClientProvider clouderaManagerApiClientProvider;

    @Mock
    private ClouderaManagerApiFactory clouderaManagerApiFactory;

    @Mock
    private ClouderaManagerCommissioner clouderaManagerCommissioner;

    @Mock
    private ApplicationContext applicationContext;

    @Mock
    private HttpClientConfig clientConfig;

    @Mock
    private ApiClient apiClient;

    @InjectMocks
    private ClouderaManagerClusterCommissionService underTest;

    private Stack stack = createStack();

    @BeforeEach
    void before() {
        underTest = new ClouderaManagerClusterCommissionService(stack, clientConfig);
        ReflectionTestUtils.setField(underTest, "clouderaManagerApiClientProvider", clouderaManagerApiClientProvider);
        ReflectionTestUtils.setField(underTest, "clouderaManagerCommissioner", clouderaManagerCommissioner);
        ReflectionTestUtils.setField(underTest, "client", apiClient);
    }

    @Test
    void testInitApiClientShouldCreateTheApiClient() throws ClusterClientInitException, ClouderaManagerClientInitException {
        ReflectionTestUtils.setField(underTest, "client", null);
        ApiClient client = mock(ApiClient.class);
        when(clouderaManagerApiClientProvider.getV31Client(GATEWAY_PORT, USER, PASSWORD, clientConfig)).thenReturn(client);

        underTest.initApiClient();

        assertEquals(client, ReflectionTestUtils.getField(underTest, "client"));
        verify(clouderaManagerApiClientProvider).getV31Client(GATEWAY_PORT, USER, PASSWORD, clientConfig);
    }

    @Test
    void testCollectHostsToCommission() {
        HostGroup hostGroup = mock(HostGroup.class);
        Set<String> hostnames = mock(Set.class);
        underTest.collectHostsToCommission(hostGroup, hostnames);
        verify(clouderaManagerCommissioner).collectHostsToCommission(stack, hostGroup, hostnames, apiClient);
    }

    @Test
    void testRecommissionClusterNodes() {
        Map<String, InstanceMetaData> hosts = mock(Map.class);
        underTest.recommissionClusterNodes(hosts);
        verify(clouderaManagerCommissioner).recommissionNodes(stack, hosts, apiClient);
    }

    @Test
    void testRecommissionClusterHosts() {
        List<String> hosts = mock(List.class);
        underTest.recommissionHosts(hosts);
        verify(clouderaManagerCommissioner).recommissionHosts(stack, apiClient, hosts);
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




