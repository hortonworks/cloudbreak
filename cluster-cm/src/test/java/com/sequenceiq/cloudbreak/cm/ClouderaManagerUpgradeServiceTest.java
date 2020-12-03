package com.sequenceiq.cloudbreak.cm;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloudera.api.swagger.ClustersResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiCommand;
import com.cloudera.api.swagger.model.ApiCommandList;
import com.sequenceiq.cloudbreak.cloud.scheduler.CancellationException;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerPollingServiceProvider;
import com.sequenceiq.cloudbreak.cm.polling.PollingResultErrorHandler;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.polling.PollingResult;
import com.sequenceiq.cloudbreak.service.CloudbreakException;

@RunWith(MockitoJUnitRunner.class)
public class ClouderaManagerUpgradeServiceTest {

    private static final String SUMMARY = "SUMMARY";

    private static final String STACK_PRODUCT_VERSION = "7.2.6";

    private static final String CLUSTER_NAME = "test-cluster";

    @InjectMocks
    private ClouderaManagerUpgradeService underTest;

    @Mock
    private ClouderaManagerPollingServiceProvider clouderaManagerPollingServiceProvider;

    @Mock
    private ClustersResourceApi clustersResourceApi;

    @Mock
    private ApiClient apiClient;

    @Mock
    private PollingResultErrorHandler pollingResultErrorHandler;

    @Test
    public void testCallUpgradeCdhCommandShouldUpgradeCdpRuntimeWhenTheUpgradeCommendIsNotPresent() throws CloudbreakException, ApiException {
        Stack stack = createStack();
        ApiCommandList apiCommandList = createApiCommandList(Collections.emptyList());
        ApiCommand apiCommand = createApiCommand();

        when(clustersResourceApi.listActiveCommands(CLUSTER_NAME, SUMMARY)).thenReturn(apiCommandList);
        when(clustersResourceApi.upgradeCdhCommand(eq(CLUSTER_NAME), any())).thenReturn(apiCommand);
        when(clouderaManagerPollingServiceProvider.startPollingCdpRuntimeUpgrade(stack, apiClient, BigDecimal.ONE)).thenReturn(PollingResult.SUCCESS);

        underTest.callUpgradeCdhCommand(STACK_PRODUCT_VERSION, clustersResourceApi, stack, apiClient);

        verify(clustersResourceApi).listActiveCommands(CLUSTER_NAME, SUMMARY);
        verify(clustersResourceApi).upgradeCdhCommand(eq(CLUSTER_NAME), any());
        verify(clouderaManagerPollingServiceProvider).startPollingCdpRuntimeUpgrade(stack, apiClient, BigDecimal.ONE);
    }

    @Test
    public void testCallUpgradeCdhCommandShouldUpgradeCdpRuntimeWhenTheUpgradeCommendIsAlreadyPresent() throws CloudbreakException, ApiException {
        Stack stack = createStack();
        ApiCommand apiCommand = createApiCommand();
        ApiCommandList apiCommandList = createApiCommandList(Collections.singletonList(apiCommand));

        when(clustersResourceApi.listActiveCommands(CLUSTER_NAME, SUMMARY)).thenReturn(apiCommandList);
        when(clouderaManagerPollingServiceProvider.startPollingCdpRuntimeUpgrade(stack, apiClient, BigDecimal.ONE)).thenReturn(PollingResult.SUCCESS);

        underTest.callUpgradeCdhCommand(STACK_PRODUCT_VERSION, clustersResourceApi, stack, apiClient);

        verify(clustersResourceApi).listActiveCommands(CLUSTER_NAME, SUMMARY);
        verify(clouderaManagerPollingServiceProvider).startPollingCdpRuntimeUpgrade(stack, apiClient, BigDecimal.ONE);
        verifyNoMoreInteractions(clustersResourceApi);
    }

    @Test(expected = CancellationException.class)
    public void testCallUpgradeCdhCommandShouldThrowCancellationExceptionWhenTheCommandIsExited() throws CloudbreakException, ApiException {
        Stack stack = createStack();
        ApiCommandList apiCommandList = createApiCommandList(Collections.emptyList());
        ApiCommand apiCommand = createApiCommand();
        PollingResult pollingResult = PollingResult.EXIT;

        when(clustersResourceApi.listActiveCommands(CLUSTER_NAME, SUMMARY)).thenReturn(apiCommandList);
        when(clustersResourceApi.upgradeCdhCommand(eq(CLUSTER_NAME), any())).thenReturn(apiCommand);
        when(clouderaManagerPollingServiceProvider.startPollingCdpRuntimeUpgrade(stack, apiClient, BigDecimal.ONE)).thenReturn(pollingResult);
        doThrow(new CancellationException("Exit")).when(pollingResultErrorHandler).handlePollingResult(eq(pollingResult), any(), any());

        underTest.callUpgradeCdhCommand(STACK_PRODUCT_VERSION, clustersResourceApi, stack, apiClient);
    }

    @Test(expected = CloudbreakException.class)
    public void testCallUpgradeCdhCommandShouldThrowCloudbreakExceptionWhenTheCommandFailedWithTimeout() throws CloudbreakException, ApiException {
        Stack stack = createStack();
        ApiCommandList apiCommandList = createApiCommandList(Collections.emptyList());
        ApiCommand apiCommand = createApiCommand();
        PollingResult pollingResult = PollingResult.TIMEOUT;

        when(clustersResourceApi.listActiveCommands(CLUSTER_NAME, SUMMARY)).thenReturn(apiCommandList);
        when(clustersResourceApi.upgradeCdhCommand(eq(CLUSTER_NAME), any())).thenReturn(apiCommand);
        when(clouderaManagerPollingServiceProvider.startPollingCdpRuntimeUpgrade(stack, apiClient, BigDecimal.ONE)).thenReturn(pollingResult);
        doThrow(new CloudbreakException("Timeout")).when(pollingResultErrorHandler).handlePollingResult(eq(pollingResult), any(), any());

        underTest.callUpgradeCdhCommand(STACK_PRODUCT_VERSION, clustersResourceApi, stack, apiClient);
    }

    @Test
    public void testCallUpgradeCdhCommandShouldExitWithoutErrorWhenTheClusterAlreadyUpgraded() throws CloudbreakException, ApiException {
        Stack stack = createStack();
        ApiCommandList apiCommandList = createApiCommandList(Collections.emptyList());
        ApiException apiException = new ApiException(0, "error", Collections.emptyMap(), "Cannot upgrade because the version is already CDH");

        when(clustersResourceApi.listActiveCommands(CLUSTER_NAME, SUMMARY)).thenReturn(apiCommandList);
        when(clustersResourceApi.upgradeCdhCommand(eq(CLUSTER_NAME), any())).thenThrow(apiException);

        underTest.callUpgradeCdhCommand(STACK_PRODUCT_VERSION, clustersResourceApi, stack, apiClient);

        verify(clustersResourceApi).listActiveCommands(CLUSTER_NAME, SUMMARY);
        verify(clustersResourceApi).upgradeCdhCommand(eq(CLUSTER_NAME), any());
        verifyNoInteractions(clouderaManagerPollingServiceProvider);
    }

    private ApiCommand createApiCommand() {
        ApiCommand apiCommand = new ApiCommand();
        apiCommand.setId(BigDecimal.ONE);
        apiCommand.setName("UpgradeCluster");
        return apiCommand;
    }

    private ApiCommandList createApiCommandList(List<ApiCommand> apiCommands) {
        ApiCommandList apiCommandList = new ApiCommandList();
        apiCommandList.setItems(apiCommands);
        return apiCommandList;
    }

    private Stack createStack() {
        Stack stack = new Stack();
        stack.setName(CLUSTER_NAME);
        return stack;
    }
}