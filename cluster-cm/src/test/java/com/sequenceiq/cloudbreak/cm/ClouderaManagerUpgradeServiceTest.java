package com.sequenceiq.cloudbreak.cm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.api.swagger.ClustersResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiCdhUpgradeArgs;
import com.cloudera.api.swagger.model.ApiCommand;
import com.sequenceiq.cloudbreak.cloud.scheduler.CancellationException;
import com.sequenceiq.cloudbreak.cm.commands.SyncApiCommandRetriever;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerPollingServiceProvider;
import com.sequenceiq.cloudbreak.cm.polling.PollingResultErrorHandler;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.polling.ExtendedPollingResult;
import com.sequenceiq.cloudbreak.service.CloudbreakException;

@ExtendWith(MockitoExtension.class)
class ClouderaManagerUpgradeServiceTest {

    private static final String COMMAND_NAME = "UpgradeCluster";

    private static final String STACK_PRODUCT_VERSION = "7.2.6";

    private static final String CLUSTER_NAME = "test-cluster";

    private static final BigDecimal COMMAND_ID = BigDecimal.TEN;

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

    @Mock
    private SyncApiCommandRetriever syncApiCommandRetriever;

    @Mock
    private ClouderaManagerCommandsService clouderaManagerCommandsService;

    @Test
    void testCallUpgradeCdhCommandShouldUpgradeCdpRuntimeWhenTheUpgradeCommendIsNotPresent() throws CloudbreakException, ApiException {
        Stack stack = createStack();
        ApiCommand apiCommand = createApiCommand();
        boolean rollingUpgradeEnabled = false;

        ArgumentCaptor<ApiCdhUpgradeArgs> cdhUpgradeArgsArgumentCaptor = ArgumentCaptor.forClass(ApiCdhUpgradeArgs.class);
        when(syncApiCommandRetriever.getCommandId(COMMAND_NAME, clustersResourceApi, stack)).thenReturn(Optional.empty());
        when(clustersResourceApi.upgradeCdhCommand(eq(CLUSTER_NAME), cdhUpgradeArgsArgumentCaptor.capture())).thenReturn(apiCommand);
        when(clouderaManagerPollingServiceProvider.startPollingCdpRuntimeUpgrade(stack, apiClient, COMMAND_ID, rollingUpgradeEnabled))
                .thenReturn(new ExtendedPollingResult.ExtendedPollingResultBuilder().success().build());

        underTest.callUpgradeCdhCommand(STACK_PRODUCT_VERSION, clustersResourceApi, stack, apiClient, rollingUpgradeEnabled);

        ApiCdhUpgradeArgs cdhUpgradeArgs = cdhUpgradeArgsArgumentCaptor.getValue();
        assertEquals(STACK_PRODUCT_VERSION, cdhUpgradeArgs.getCdhParcelVersion());
        assertNull(cdhUpgradeArgs.getRollingRestartArgs());
        verify(syncApiCommandRetriever).getCommandId(COMMAND_NAME, clustersResourceApi, stack);
        verify(clouderaManagerCommandsService, times(0)).retryApiCommand(apiClient, COMMAND_ID);
        verify(clustersResourceApi).upgradeCdhCommand(eq(CLUSTER_NAME), any());
        verify(clouderaManagerPollingServiceProvider).startPollingCdpRuntimeUpgrade(stack, apiClient, COMMAND_ID, rollingUpgradeEnabled);
        verify(clouderaManagerCommandsService, times(0)).getApiCommand(apiClient, COMMAND_ID);
    }

    @Test
    void testCallUpgradeCdhCommandShouldUpgradeCdpRuntimeWhenTheUpgradeCommendIsNotPresentAndTheRollingUpgradeIsEnabled()
            throws CloudbreakException, ApiException {
        Stack stack = createStack();
        ApiCommand apiCommand = createApiCommand();
        boolean rollingUpgradeEnabled = true;

        ArgumentCaptor<ApiCdhUpgradeArgs> cdhUpgradeArgsArgumentCaptor = ArgumentCaptor.forClass(ApiCdhUpgradeArgs.class);
        when(syncApiCommandRetriever.getCommandId(COMMAND_NAME, clustersResourceApi, stack)).thenReturn(Optional.empty());
        when(clustersResourceApi.upgradeCdhCommand(eq(CLUSTER_NAME), cdhUpgradeArgsArgumentCaptor.capture())).thenReturn(apiCommand);
        when(clouderaManagerPollingServiceProvider.startPollingCdpRuntimeUpgrade(stack, apiClient, COMMAND_ID, rollingUpgradeEnabled))
                .thenReturn(new ExtendedPollingResult.ExtendedPollingResultBuilder().success().build());

        underTest.callUpgradeCdhCommand(STACK_PRODUCT_VERSION, clustersResourceApi, stack, apiClient, rollingUpgradeEnabled);

        ApiCdhUpgradeArgs cdhUpgradeArgs = cdhUpgradeArgsArgumentCaptor.getValue();
        assertEquals(STACK_PRODUCT_VERSION, cdhUpgradeArgs.getCdhParcelVersion());
        assertNotNull(cdhUpgradeArgs.getRollingRestartArgs());
        verify(syncApiCommandRetriever).getCommandId(COMMAND_NAME, clustersResourceApi, stack);
        verify(clouderaManagerCommandsService, times(0)).retryApiCommand(apiClient, COMMAND_ID);
        verify(clustersResourceApi).upgradeCdhCommand(eq(CLUSTER_NAME), any());
        verify(clouderaManagerPollingServiceProvider).startPollingCdpRuntimeUpgrade(stack, apiClient, COMMAND_ID, rollingUpgradeEnabled);
        verify(clouderaManagerCommandsService, times(0)).getApiCommand(apiClient, COMMAND_ID);
    }

    @Test
    void testCallUpgradeCdhCommandShouldNotUpgradeCdpRuntimeWhenTheUpgradeCommandIsAlreadyPresentAndActive() throws CloudbreakException, ApiException {
        Stack stack = createStack();
        ApiCommand apiCommand = createApiCommand();
        apiCommand.setActive(Boolean.TRUE);
        apiCommand.setCanRetry(Boolean.FALSE);
        apiCommand.setSuccess(Boolean.FALSE);
        boolean rollingUpgradeEnabled = false;

        when(syncApiCommandRetriever.getCommandId(COMMAND_NAME, clustersResourceApi, stack)).thenReturn(Optional.of(COMMAND_ID));
        when(clouderaManagerPollingServiceProvider.startPollingCdpRuntimeUpgrade(stack, apiClient, COMMAND_ID, rollingUpgradeEnabled))
                .thenReturn(new ExtendedPollingResult.ExtendedPollingResultBuilder().success().build());
        when(clouderaManagerCommandsService.getApiCommand(apiClient, COMMAND_ID)).thenReturn(apiCommand);

        underTest.callUpgradeCdhCommand(STACK_PRODUCT_VERSION, clustersResourceApi, stack, apiClient, rollingUpgradeEnabled);

        verify(syncApiCommandRetriever).getCommandId(COMMAND_NAME, clustersResourceApi, stack);
        verify(clouderaManagerCommandsService, times(0)).retryApiCommand(apiClient, COMMAND_ID);
        verify(clouderaManagerCommandsService).getApiCommand(apiClient, COMMAND_ID);
        verify(clustersResourceApi, times(0)).upgradeCdhCommand(eq(CLUSTER_NAME), any());
        verify(clouderaManagerPollingServiceProvider).startPollingCdpRuntimeUpgrade(stack, apiClient, COMMAND_ID, rollingUpgradeEnabled);
        verifyNoMoreInteractions(clustersResourceApi);
    }

    @Test
    void testCallUpgradeCdhCommandShouldUpgradeCdpRuntimeWhenTheUpgradeCommandIsAlreadyPresentAndInactiveAndNotRetryable()
            throws CloudbreakException, ApiException {
        Stack stack = createStack();
        boolean rollingUpgradeEnabled = false;
        ApiCommand apiCommand = createApiCommand();
        apiCommand.setActive(Boolean.FALSE);
        apiCommand.setCanRetry(Boolean.FALSE);
        apiCommand.setSuccess(Boolean.TRUE);

        ArgumentCaptor<ApiCdhUpgradeArgs> cdhUpgradeArgsArgumentCaptor = ArgumentCaptor.forClass(ApiCdhUpgradeArgs.class);
        when(syncApiCommandRetriever.getCommandId(COMMAND_NAME, clustersResourceApi, stack)).thenReturn(Optional.of(COMMAND_ID));
        when(clouderaManagerPollingServiceProvider.startPollingCdpRuntimeUpgrade(stack, apiClient, COMMAND_ID, rollingUpgradeEnabled))
                .thenReturn(new ExtendedPollingResult.ExtendedPollingResultBuilder().success().build());
        when(clouderaManagerCommandsService.getApiCommand(apiClient, COMMAND_ID)).thenReturn(apiCommand);
        when(clustersResourceApi.upgradeCdhCommand(eq(CLUSTER_NAME), cdhUpgradeArgsArgumentCaptor.capture())).thenReturn(apiCommand);

        underTest.callUpgradeCdhCommand(STACK_PRODUCT_VERSION, clustersResourceApi, stack, apiClient, rollingUpgradeEnabled);

        ApiCdhUpgradeArgs cdhUpgradeArgs = cdhUpgradeArgsArgumentCaptor.getValue();
        assertEquals(STACK_PRODUCT_VERSION, cdhUpgradeArgs.getCdhParcelVersion());
        assertNull(cdhUpgradeArgs.getRollingRestartArgs());
        verify(syncApiCommandRetriever).getCommandId(COMMAND_NAME, clustersResourceApi, stack);
        verify(clouderaManagerCommandsService, times(0)).retryApiCommand(apiClient, COMMAND_ID);
        verify(clouderaManagerCommandsService).getApiCommand(apiClient, COMMAND_ID);
        verify(clustersResourceApi, times(1)).upgradeCdhCommand(eq(CLUSTER_NAME), any());
        verify(clouderaManagerPollingServiceProvider).startPollingCdpRuntimeUpgrade(stack, apiClient, COMMAND_ID, rollingUpgradeEnabled);
        verifyNoMoreInteractions(clustersResourceApi);
    }

    @Test
    void testCallUpgradeCdhCommandShouldRetryCdpRuntimeWhenTheUpgradeCommandIsAlreadyPresentAndInactiveAndRetryable()
            throws CloudbreakException, ApiException {
        Stack stack = createStack();
        boolean rollingUpgradeEnabled = false;
        ApiCommand apiCommand = createApiCommand();
        apiCommand.setActive(Boolean.FALSE);
        apiCommand.setCanRetry(Boolean.TRUE);
        apiCommand.setSuccess(Boolean.FALSE);

        when(syncApiCommandRetriever.getCommandId(COMMAND_NAME, clustersResourceApi, stack)).thenReturn(Optional.of(COMMAND_ID));
        when(clouderaManagerPollingServiceProvider.startPollingCdpRuntimeUpgrade(stack, apiClient, COMMAND_ID, rollingUpgradeEnabled))
                .thenReturn(new ExtendedPollingResult.ExtendedPollingResultBuilder().success().build());
        when(clouderaManagerCommandsService.getApiCommand(apiClient, COMMAND_ID)).thenReturn(apiCommand);
        when(clouderaManagerCommandsService.retryApiCommand(apiClient, COMMAND_ID)).thenReturn(apiCommand);

        underTest.callUpgradeCdhCommand(STACK_PRODUCT_VERSION, clustersResourceApi, stack, apiClient, rollingUpgradeEnabled);

        verify(syncApiCommandRetriever).getCommandId(COMMAND_NAME, clustersResourceApi, stack);
        verify(clouderaManagerCommandsService).getApiCommand(apiClient, COMMAND_ID);
        verify(clouderaManagerCommandsService).retryApiCommand(apiClient, COMMAND_ID);
        verify(clustersResourceApi, times(0)).upgradeCdhCommand(eq(CLUSTER_NAME), any());
        verify(clouderaManagerPollingServiceProvider).startPollingCdpRuntimeUpgrade(stack, apiClient, COMMAND_ID, rollingUpgradeEnabled);
        verifyNoMoreInteractions(clustersResourceApi);
    }

    @Test
    void testCallUpgradeCdhCommandShouldThrowCancellationExceptionWhenTheCommandIsExited() throws CloudbreakException, ApiException {
        Stack stack = createStack();
        ApiCommand apiCommand = createApiCommand();
        ExtendedPollingResult pollingResult = new ExtendedPollingResult.ExtendedPollingResultBuilder().exit().build();
        boolean rollingUpgradeEnabled = false;

        when(clustersResourceApi.upgradeCdhCommand(eq(CLUSTER_NAME), any())).thenReturn(apiCommand);
        when(clouderaManagerPollingServiceProvider.startPollingCdpRuntimeUpgrade(stack, apiClient, COMMAND_ID, rollingUpgradeEnabled)).thenReturn(pollingResult);
        doThrow(new CancellationException("Exit")).when(pollingResultErrorHandler).handlePollingResult(eq(pollingResult), any(), any());

        assertThrows(CancellationException.class,
                () -> underTest.callUpgradeCdhCommand(STACK_PRODUCT_VERSION, clustersResourceApi, stack, apiClient, rollingUpgradeEnabled), "Exit");
    }

    @Test
    void testCallUpgradeCdhCommandShouldThrowCloudbreakExceptionWhenTheCommandFailedWithTimeout() throws CloudbreakException, ApiException {
        Stack stack = createStack();
        ApiCommand apiCommand = createApiCommand();
        ExtendedPollingResult pollingResult = new ExtendedPollingResult.ExtendedPollingResultBuilder().timeout().build();
        boolean rollingUpgradeEnabled = false;

        when(clustersResourceApi.upgradeCdhCommand(eq(CLUSTER_NAME), any())).thenReturn(apiCommand);
        when(clouderaManagerPollingServiceProvider.startPollingCdpRuntimeUpgrade(stack, apiClient, COMMAND_ID, rollingUpgradeEnabled)).thenReturn(pollingResult);
        doThrow(new CloudbreakException("Timeout")).when(pollingResultErrorHandler).handlePollingResult(eq(pollingResult), any(), any());

        assertThrows(CloudbreakException.class,
                () -> underTest.callUpgradeCdhCommand(STACK_PRODUCT_VERSION, clustersResourceApi, stack, apiClient, rollingUpgradeEnabled), "Timeout");

        verify(clouderaManagerPollingServiceProvider).startPollingCdpRuntimeUpgrade(stack, apiClient, COMMAND_ID, rollingUpgradeEnabled);
    }

    @Test
    void testCallUpgradeCdhCommandShouldExitWithoutErrorWhenTheClusterAlreadyUpgraded() throws CloudbreakException, ApiException {
        Stack stack = createStack();
        ApiException apiException = new ApiException(0, "error", Collections.emptyMap(), "Cannot upgrade because the version is already CDH");

        when(clustersResourceApi.upgradeCdhCommand(eq(CLUSTER_NAME), any())).thenThrow(apiException);

        underTest.callUpgradeCdhCommand(STACK_PRODUCT_VERSION, clustersResourceApi, stack, apiClient, false);

        verify(clustersResourceApi).upgradeCdhCommand(eq(CLUSTER_NAME), any());
        verifyNoInteractions(clouderaManagerPollingServiceProvider);
    }

    @Test
    void testCallPostRuntimeUpgradeCommandForNewSubmit() throws ApiException, CloudbreakException {
        Stack stack = createStack();
        ApiCommand apiCommand = createApiCommand();
        when(syncApiCommandRetriever.getCommandId("PostClouderaRuntimeUpgradeCommand", clustersResourceApi, stack)).thenReturn(Optional.empty());
        when(clustersResourceApi.postClouderaRuntimeUpgrade(eq(CLUSTER_NAME))).thenReturn(apiCommand);
        ExtendedPollingResult pollingResult = new ExtendedPollingResult.ExtendedPollingResultBuilder().success().build();
        when(clouderaManagerPollingServiceProvider.startPollingCdpRuntimeUpgrade(stack, apiClient, COMMAND_ID, false)).thenReturn(pollingResult);

        underTest.callPostRuntimeUpgradeCommand(clustersResourceApi, stack, apiClient);

        verify(syncApiCommandRetriever).getCommandId("PostClouderaRuntimeUpgradeCommand", clustersResourceApi, stack);
        verify(clouderaManagerPollingServiceProvider).startPollingCdpRuntimeUpgrade(stack, apiClient, COMMAND_ID, false);
        verify(clustersResourceApi, times(1)).postClouderaRuntimeUpgrade(CLUSTER_NAME);
    }

    @Test
    void testCallPostRuntimeUpgradeCommandWhenItsAlreadyRunning() throws ApiException, CloudbreakException {
        Stack stack = createStack();
        ApiCommand apiCommand = createApiCommand();
        apiCommand.setActive(Boolean.TRUE);
        apiCommand.setSuccess(Boolean.FALSE);
        apiCommand.setCanRetry(Boolean.FALSE);
        when(syncApiCommandRetriever.getCommandId("PostClouderaRuntimeUpgradeCommand", clustersResourceApi, stack)).thenReturn(Optional.of(COMMAND_ID));
        when(clouderaManagerCommandsService.getApiCommand(apiClient, COMMAND_ID)).thenReturn(apiCommand);
        ExtendedPollingResult pollingResult = new ExtendedPollingResult.ExtendedPollingResultBuilder().success().build();
        when(clouderaManagerPollingServiceProvider.startPollingCdpRuntimeUpgrade(stack, apiClient, COMMAND_ID, false)).thenReturn(pollingResult);

        underTest.callPostRuntimeUpgradeCommand(clustersResourceApi, stack, apiClient);

        verify(syncApiCommandRetriever).getCommandId("PostClouderaRuntimeUpgradeCommand", clustersResourceApi, stack);
        verify(clouderaManagerPollingServiceProvider).startPollingCdpRuntimeUpgrade(stack, apiClient, COMMAND_ID, false);
        verify(clouderaManagerCommandsService, times(0)).retryApiCommand(apiClient, COMMAND_ID);
        verifyNoMoreInteractions(clustersResourceApi);
    }

    @Test
    void testCallPostRuntimeUpgradeCommandWhenItsAlreadySucceededAndShouldBeSubmittedAgain() throws ApiException, CloudbreakException {
        Stack stack = createStack();
        ApiCommand apiCommand = createApiCommand();
        apiCommand.setActive(Boolean.FALSE);
        apiCommand.setSuccess(Boolean.TRUE);
        apiCommand.setCanRetry(Boolean.FALSE);
        when(syncApiCommandRetriever.getCommandId("PostClouderaRuntimeUpgradeCommand", clustersResourceApi, stack)).thenReturn(Optional.of(COMMAND_ID));
        when(clouderaManagerCommandsService.getApiCommand(apiClient, COMMAND_ID)).thenReturn(apiCommand);
        when(clustersResourceApi.postClouderaRuntimeUpgrade(eq(CLUSTER_NAME))).thenReturn(apiCommand);
        ExtendedPollingResult pollingResult = new ExtendedPollingResult.ExtendedPollingResultBuilder().success().build();
        when(clouderaManagerPollingServiceProvider.startPollingCdpRuntimeUpgrade(stack, apiClient, COMMAND_ID, false)).thenReturn(pollingResult);

        underTest.callPostRuntimeUpgradeCommand(clustersResourceApi, stack, apiClient);

        verify(syncApiCommandRetriever).getCommandId("PostClouderaRuntimeUpgradeCommand", clustersResourceApi, stack);
        verify(clouderaManagerPollingServiceProvider).startPollingCdpRuntimeUpgrade(stack, apiClient, COMMAND_ID, false);
        verify(clouderaManagerCommandsService, times(0)).retryApiCommand(apiClient, COMMAND_ID);
        verify(clustersResourceApi, times(1)).postClouderaRuntimeUpgrade(CLUSTER_NAME);
    }

    private ApiCommand createApiCommand() {
        ApiCommand apiCommand = new ApiCommand();
        apiCommand.setId(COMMAND_ID);
        apiCommand.setName(COMMAND_NAME);
        return apiCommand;
    }

    private Stack createStack() {
        Stack stack = new Stack();
        stack.setName(CLUSTER_NAME);
        return stack;
    }
}