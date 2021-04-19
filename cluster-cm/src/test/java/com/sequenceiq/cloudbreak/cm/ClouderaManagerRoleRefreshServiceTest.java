package com.sequenceiq.cloudbreak.cm;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.api.swagger.ClouderaManagerResourceApi;
import com.cloudera.api.swagger.ClustersResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiCommand;
import com.cloudera.api.swagger.model.ApiCommandList;
import com.sequenceiq.cloudbreak.cm.client.retry.ClouderaManagerApiFactory;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerPollingServiceProvider;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.polling.PollingResult;
import com.sequenceiq.cloudbreak.service.CloudbreakException;

@ExtendWith(MockitoExtension.class)
public class ClouderaManagerRoleRefreshServiceTest {

    private static final String CLUSTER_NAME = "myCluster";

    private static final Integer COMMAND_ID = 10;

    @InjectMocks
    private ClouderaManagerRoleRefreshService underTest;

    @Mock
    private ClouderaManagerApiFactory clouderaManagerApiFactory;

    @Mock
    private ClouderaManagerPollingServiceProvider clouderaManagerPollingServiceProvider;

    @Mock
    private ClustersResourceApi clustersResourceApi;

    @Mock
    private ClouderaManagerResourceApi clouderaManagerResourceApi;

    @Mock
    private ApiClient apiClient;

    @Test
    public void testRestartClusterRolesShouldUpdateTheRoles() throws ApiException, CloudbreakException {
        Stack stack = createStack();
        setupMocks();
        when(clouderaManagerPollingServiceProvider.startPollingCmConfigurationRefresh(stack, apiClient, COMMAND_ID)).thenReturn(PollingResult.SUCCESS);

        underTest.refreshClusterRoles(apiClient, stack);

        verify(clustersResourceApi).refresh(CLUSTER_NAME);
        verify(clouderaManagerPollingServiceProvider).startPollingCmConfigurationRefresh(stack, apiClient, COMMAND_ID);
    }

    @Test
    public void testRestartClusterRolesCmFailure() throws ApiException, CloudbreakException {
        Stack stack = createStack();
        setupMocks();
        when(clouderaManagerPollingServiceProvider.startPollingCmConfigurationRefresh(stack, apiClient, COMMAND_ID)).
                thenThrow(new ClouderaManagerOperationFailedException("Refresh cluster failed."));

        underTest.refreshClusterRoles(apiClient, stack);

        verify(clustersResourceApi).refresh(CLUSTER_NAME);
        verify(clouderaManagerPollingServiceProvider).startPollingCmConfigurationRefresh(stack, apiClient, COMMAND_ID);
    }

    @Test
    public void testRestartClusterRolesOtherFailure() throws ApiException {
        Stack stack = createStack();
        setupMocks();
        when(clouderaManagerPollingServiceProvider.startPollingCmConfigurationRefresh(stack, apiClient, COMMAND_ID)).
                thenThrow(new CloudbreakServiceException("Refresh cluster failed."));

        assertThrows(CloudbreakServiceException.class, () -> underTest.refreshClusterRoles(apiClient, stack));

        verify(clustersResourceApi).refresh(CLUSTER_NAME);
        verify(clouderaManagerPollingServiceProvider).startPollingCmConfigurationRefresh(stack, apiClient, COMMAND_ID);
    }

    private void setupMocks() throws ApiException {
        when(clouderaManagerApiFactory.getClouderaManagerResourceApi(apiClient)).thenReturn(clouderaManagerResourceApi);
        when(clouderaManagerResourceApi.listActiveCommands(DataView.SUMMARY.name())).thenReturn(createApiCommandList());
        when(clouderaManagerApiFactory.getClustersResourceApi(apiClient)).thenReturn(clustersResourceApi);
        when(clustersResourceApi.refresh(CLUSTER_NAME)).thenReturn(createApiCommand());
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