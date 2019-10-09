package com.sequenceiq.cloudbreak.cm;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Collections;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloudera.api.swagger.ClouderaManagerResourceApi;
import com.cloudera.api.swagger.ClustersResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiCommand;
import com.cloudera.api.swagger.model.ApiCommandList;
import com.sequenceiq.cloudbreak.cm.client.retry.ClouderaManagerApiFactory;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerPollingServiceProvider;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.polling.PollingResult;
import com.sequenceiq.cloudbreak.service.CloudbreakException;

@RunWith(MockitoJUnitRunner.class)
public class ClouderaManagerRoleRefreshServiceTest {

    private static final String CLUSTER_NAME = "myCluster";

    private static final BigDecimal COMMAND_ID = BigDecimal.TEN;

    @InjectMocks
    private ClouderaManagerRoleRefreshService underTest;

    @Mock
    private ClouderaManagerApiFactory clouderaManagerApiFactory;

    @Mock
    private ClouderaManagerPollingServiceProvider clouderaManagerPollingServiceProvider;

    @Test
    public void testRestartClusterRolesShouldUpdateTheRoles() throws ApiException, CloudbreakException {
        ApiClient apiClient = mock(ApiClient.class);
        Stack stack = createStack();
        ClustersResourceApi clustersResourceApi = mock(ClustersResourceApi.class);
        ClouderaManagerResourceApi clouderaManagerResourceApi = mock(ClouderaManagerResourceApi.class);
        ApiCommand apiCommand = createApiCommand();
        ApiCommandList apiCommandList = createApiCommandList();

        when(clouderaManagerApiFactory.getClouderaManagerResourceApi(apiClient)).thenReturn(clouderaManagerResourceApi);
        when(clouderaManagerResourceApi.listActiveCommands(DataView.SUMMARY.name())).thenReturn(apiCommandList);
        when(clouderaManagerApiFactory.getClustersResourceApi(apiClient)).thenReturn(clustersResourceApi);
        when(clustersResourceApi.refresh(CLUSTER_NAME)).thenReturn(apiCommand);
        when(clouderaManagerPollingServiceProvider.startPollingCmConfigurationRefresh(stack, apiClient, COMMAND_ID)).thenReturn(PollingResult.SUCCESS);

        underTest.refreshClusterRoles(apiClient, stack);

        verify(clouderaManagerPollingServiceProvider).startPollingCmConfigurationRefresh(stack, apiClient, COMMAND_ID);
        verify(clustersResourceApi).refresh(CLUSTER_NAME);
        verify(clouderaManagerPollingServiceProvider).startPollingCmConfigurationRefresh(stack, apiClient, COMMAND_ID);
    }

    private ApiCommand createApiCommand() {
        ApiCommand apiCommand = new ApiCommand();
        apiCommand.setId(COMMAND_ID);
        return apiCommand;
    }

    private ApiCommandList createApiCommandList() {
        ApiCommandList apiCommandList = new ApiCommandList();
        apiCommandList.setItems(Collections.emptyList());
        return apiCommandList;
    }

    private Stack createStack() {
        Stack stack = new Stack();
        Cluster cluster = new Cluster();
        cluster.setName(CLUSTER_NAME);
        stack.setCluster(cluster);
        return stack;
    }
}