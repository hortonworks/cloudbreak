package com.sequenceiq.cloudbreak.cm;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.cm.DataView.SUMMARY;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_CM_CLUSTER_SERVICES_RESTARTING;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.api.swagger.ClustersResourceApi;
import com.cloudera.api.swagger.ServicesResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiCommand;
import com.cloudera.api.swagger.model.ApiCommandList;
import com.cloudera.api.swagger.model.ApiRestartClusterArgs;
import com.cloudera.api.swagger.model.ApiRollingRestartClusterArgs;
import com.cloudera.api.swagger.model.ApiService;
import com.cloudera.api.swagger.model.ApiServiceList;
import com.sequenceiq.cloudbreak.cm.client.retry.ClouderaManagerApiFactory;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerPollingServiceProvider;
import com.sequenceiq.cloudbreak.cm.polling.PollingResultErrorHandler;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.polling.ExtendedPollingResult;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;

@ExtendWith(MockitoExtension.class)
class ClouderaManagerRestartServiceTest {

    private static final BigDecimal COMMAND_ID = BigDecimal.ONE;

    @InjectMocks
    private ClouderaManagerRestartService underTest;

    @Mock
    private ClouderaManagerPollingServiceProvider clouderaManagerPollingServiceProvider;

    @Mock
    private PollingResultErrorHandler pollingResultErrorHandler;

    @Mock
    private CloudbreakEventService eventService;

    @Mock
    private ClouderaManagerApiFactory clouderaManagerApiFactory;

    @Mock
    private ApiClient apiClient;

    @Mock
    private ClustersResourceApi clustersResourceApi;

    @Mock
    private ServicesResourceApi servicesResourceApi;

    @Mock
    private ExtendedPollingResult pollingResult;

    private final Stack stack = createStack();

    @Test
    void testRestartWhenExistingRestartCommandIsPresent() throws CloudbreakException, ApiException {
        ApiCommandList activeCommands = new ApiCommandList().items(Collections.singletonList(new ApiCommand().name("Restart").id(COMMAND_ID)));
        when(clouderaManagerApiFactory.getClustersResourceApi(apiClient)).thenReturn(clustersResourceApi);
        when(clustersResourceApi.listActiveCommands(stack.getName(), SUMMARY.name(), null)).thenReturn(activeCommands);
        when(clouderaManagerPollingServiceProvider.startPollingCmServicesRestart(stack, apiClient, COMMAND_ID)).thenReturn(pollingResult);

        underTest.doRestartServicesIfNeeded(apiClient, stack, false, Optional.empty());

        verify(pollingResultErrorHandler).handlePollingResult(eq(pollingResult), anyString(), anyString());
        verifyNoInteractions(eventService);
    }

    @Test
    void testRestartWhenWaitForCommandExecutionOnly() throws CloudbreakException, ApiException {
        when(clouderaManagerApiFactory.getClustersResourceApi(apiClient)).thenReturn(clustersResourceApi);
        when(clustersResourceApi.listActiveCommands(stack.getName(), SUMMARY.name(), null)).thenReturn(new ApiCommandList().items(Collections.emptyList()));

        underTest.waitForRestartExecutionIfPresent(apiClient, stack, true);

        verify(clouderaManagerApiFactory).getClustersResourceApi(apiClient);
        verify(clustersResourceApi).listActiveCommands(stack.getName(), SUMMARY.name(), null);
        verifyNoInteractions(eventService, clouderaManagerPollingServiceProvider);
    }

    @Test
    void testRestartWhenTriggerRestartCommand() throws CloudbreakException, ApiException {
        when(clouderaManagerApiFactory.getClustersResourceApi(apiClient)).thenReturn(clustersResourceApi);
        when(clustersResourceApi.listActiveCommands(stack.getName(), SUMMARY.name(), null)).thenReturn(new ApiCommandList().items(Collections.emptyList()));
        when(clouderaManagerPollingServiceProvider.startPollingCmServicesRestart(stack, apiClient, COMMAND_ID)).thenReturn(pollingResult);
        when(clustersResourceApi.restartCommand(eq(stack.getName()), any(ApiRestartClusterArgs.class))).thenReturn(new ApiCommand().id(COMMAND_ID));

        underTest.doRestartServicesIfNeeded(apiClient, stack, false, Optional.empty());

        verify(pollingResultErrorHandler).handlePollingResult(eq(pollingResult), anyString(), anyString());
        verify(eventService).fireCloudbreakEvent(stack.getId(), UPDATE_IN_PROGRESS.name(), CLUSTER_CM_CLUSTER_SERVICES_RESTARTING);
    }

    @Test
    void testRestartWhenTriggerRestartCommandWithServices() throws CloudbreakException, ApiException {
        when(clouderaManagerApiFactory.getClustersResourceApi(apiClient)).thenReturn(clustersResourceApi);
        when(clustersResourceApi.listActiveCommands(stack.getName(), SUMMARY.name(), null)).thenReturn(new ApiCommandList().items(Collections.emptyList()));
        when(clouderaManagerPollingServiceProvider.startPollingCmServicesRestart(stack, apiClient, COMMAND_ID)).thenReturn(pollingResult);
        when(clustersResourceApi.restartCommand(eq(stack.getName()), any(ApiRestartClusterArgs.class))).thenReturn(new ApiCommand().id(COMMAND_ID));

        underTest.doRestartServicesIfNeeded(apiClient, stack, false, Optional.of(List.of("test")));

        verify(pollingResultErrorHandler).handlePollingResult(eq(pollingResult), anyString(), anyString());
        verify(eventService).fireCloudbreakEvent(stack.getId(), UPDATE_IN_PROGRESS.name(), CLUSTER_CM_CLUSTER_SERVICES_RESTARTING);
        ArgumentCaptor<ApiRestartClusterArgs> restartArgsCaptor = ArgumentCaptor.forClass(ApiRestartClusterArgs.class);
        verify(clustersResourceApi).restartCommand(eq(stack.getName()), restartArgsCaptor.capture());
        assertEquals(List.of("test"), restartArgsCaptor.getValue().getRestartServiceNames());
    }

    @Test
    void testRestartWhenTriggeredRestartCommandIsNull() throws CloudbreakException, ApiException {
        when(clouderaManagerApiFactory.getClustersResourceApi(apiClient)).thenReturn(clustersResourceApi);
        when(clustersResourceApi.listActiveCommands(stack.getName(), SUMMARY.name(), null)).thenReturn(new ApiCommandList().items(Collections.emptyList()));
        when(clustersResourceApi.restartCommand(eq(stack.getName()), any(ApiRestartClusterArgs.class))).thenReturn(null);

        underTest.doRestartServicesIfNeeded(apiClient, stack, false, Optional.empty());

        verify(eventService).fireCloudbreakEvent(stack.getId(), UPDATE_IN_PROGRESS.name(), CLUSTER_CM_CLUSTER_SERVICES_RESTARTING);
        verifyNoInteractions(pollingResultErrorHandler);
    }

    @Test
    void testRestartWhenTriggerRollingRestartCommand() throws CloudbreakException, ApiException {
        List<String> serviceNames = List.of("hive, spark, flink");
        when(clouderaManagerApiFactory.getClustersResourceApi(apiClient)).thenReturn(clustersResourceApi);
        when(clustersResourceApi.listActiveCommands(stack.getName(), SUMMARY.name(), null)).thenReturn(new ApiCommandList().items(Collections.emptyList()));
        when(clouderaManagerApiFactory.getServicesResourceApi(apiClient)).thenReturn(servicesResourceApi);
        when(servicesResourceApi.readServices(stack.getName(), SUMMARY.name())).thenReturn(createApiServiceList(serviceNames));
        when(clustersResourceApi.rollingRestart(eq(stack.getName()), any(ApiRollingRestartClusterArgs.class))).thenReturn(new ApiCommand().id(COMMAND_ID));
        when(clouderaManagerPollingServiceProvider.startPollingCmServicesRestart(stack, apiClient, COMMAND_ID)).thenReturn(pollingResult);

        underTest.doRestartServicesIfNeeded(apiClient, stack, true, Optional.empty());

        ArgumentCaptor<ApiRollingRestartClusterArgs> argumentCaptor = ArgumentCaptor.forClass(ApiRollingRestartClusterArgs.class);
        verify(clustersResourceApi).rollingRestart(eq(stack.getName()), argumentCaptor.capture());
        assertTrue(CollectionUtils.isEqualCollection(serviceNames, argumentCaptor.getValue().getRestartServiceNames()));
        verify(pollingResultErrorHandler).handlePollingResult(eq(pollingResult), anyString(), anyString());
        verify(eventService).fireCloudbreakEvent(stack.getId(), UPDATE_IN_PROGRESS.name(), CLUSTER_CM_CLUSTER_SERVICES_RESTARTING);
    }

    private ApiServiceList createApiServiceList(List<String> serviceNames) {
        return new ApiServiceList().items(serviceNames.stream()
                .map(serviceName -> new ApiService().name(serviceName))
                .collect(Collectors.toList()));
    }

    private Stack createStack() {
        Stack stack = new Stack();
        stack.setName("stack-name");
        stack.setId(1L);
        return stack;
    }

}