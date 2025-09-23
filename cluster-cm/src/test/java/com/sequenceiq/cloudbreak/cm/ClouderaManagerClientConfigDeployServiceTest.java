package com.sequenceiq.cloudbreak.cm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.retry.RetryContext;
import org.springframework.retry.support.RetrySynchronizationManager;

import com.cloudera.api.swagger.ClustersResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiCommand;
import com.cloudera.api.swagger.model.ApiCommandList;
import com.sequenceiq.cloudbreak.cm.commands.SyncApiCommandPollerConfig;
import com.sequenceiq.cloudbreak.cm.model.ClouderaManagerClientConfigDeployRequest;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.CloudbreakException;

@ExtendWith(MockitoExtension.class)
class ClouderaManagerClientConfigDeployServiceTest {

    private static final String COMMAND_NAME = "commandName";

    private static final String STACK_CRN = "crn:cdp:datalake:us-west-1:acc1:datalake:cluster";

    @Mock
    private SyncApiCommandPollerConfig syncApiCommandPollerConfig;

    @Mock
    private ClouderaManagerSyncApiCommandIdProvider clouderaManagerSyncApiCommandIdProvider;

    @Mock
    private ClouderaManagerCommonCommandService clouderaManagerCommonCommandService;

    @Mock
    private ClustersResourceApi clustersResourceApi;

    @InjectMocks
    private ClouderaManagerClientConfigDeployService underTest;

    private Stack stack = mock(Stack.class);

    private List<ApiCommand> commands = new ArrayList<>();

    @Test
    public void testGetDeployClientConfigCommandId() throws CloudbreakException, ApiException {
        // GIVEN
        ApiClient apiClient = mock(ApiClient.class);
        given(stack.getResourceCrn()).willReturn(STACK_CRN);
        given(syncApiCommandPollerConfig.isSyncApiCommandPollingEnabled(STACK_CRN)).willReturn(false);
        given(clouderaManagerCommonCommandService.getApiCommand(any(), any(), any(), any()))
                .willReturn(new ApiCommand().name(COMMAND_NAME).id(BigDecimal.ONE));
        given(clustersResourceApi.listActiveCommands(any(), any(), any()))
                .willReturn(new ApiCommandList());
        RetryContext retryContext = mock(RetryContext.class);
        when(retryContext.getRetryCount()).thenReturn(1);
        // WHEN
        try (MockedStatic<RetrySynchronizationManager> retrySynchronizationManagerMockedStatic = mockStatic(RetrySynchronizationManager.class)) {
            when(RetrySynchronizationManager.getContext()).thenReturn(retryContext);
            BigDecimal result = underTest.deployClientConfig(
                    ClouderaManagerClientConfigDeployRequest.builder()
                            .stack(stack)
                            .client(apiClient)
                            .clustersResourceApi(clustersResourceApi)
                            .build()
            );

            assertEquals(BigDecimal.ONE, result);
        }
        // THEN

    }

    @Test
    public void testGetDeployClientConfigCommandIdWithSyncApiPolling() throws CloudbreakException, ApiException {
        RetryContext retryContext = mock(RetryContext.class);
        when(retryContext.getRetryCount()).thenReturn(1);
        // GIVEN
        ApiClient apiClient = mock(ApiClient.class);
        given(stack.getResourceCrn()).willReturn(STACK_CRN);
        given(syncApiCommandPollerConfig.isSyncApiCommandPollingEnabled(STACK_CRN)).willReturn(true);
        given(syncApiCommandPollerConfig.getDeployClusterClientConfigCommandName()).willReturn(COMMAND_NAME);
        given(clouderaManagerSyncApiCommandIdProvider.executeSyncApiCommandAndGetCommandId(anyString(), any(), any(), any(), any())).willReturn(BigDecimal.ONE);
        given(clustersResourceApi.listActiveCommands(any(), any(), any()))
                .willReturn(new ApiCommandList());
        // WHEN
        try (MockedStatic<RetrySynchronizationManager> retrySynchronizationManagerMockedStatic = mockStatic(RetrySynchronizationManager.class)) {
            when(RetrySynchronizationManager.getContext()).thenReturn(retryContext);
            BigDecimal result = underTest.deployClientConfig(
                    ClouderaManagerClientConfigDeployRequest.builder()
                            .stack(stack)
                            .client(apiClient)
                            .clustersResourceApi(clustersResourceApi)
                            .build()
            );

            assertEquals(BigDecimal.ONE, result);
        }
        // THEN

        verify(clouderaManagerSyncApiCommandIdProvider, times(1)).executeSyncApiCommandAndGetCommandId(anyString(), any(), any(), any(), any());
    }

}