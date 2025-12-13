package com.sequenceiq.cloudbreak.cm;

import static com.cloudera.api.swagger.model.ApiServiceState.STARTED;
import static com.cloudera.api.swagger.model.ApiServiceState.STOPPED;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.api.swagger.ServicesResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiCommand;
import com.cloudera.api.swagger.model.ApiService;
import com.cloudera.api.swagger.model.ApiServiceList;
import com.sequenceiq.cloudbreak.cm.client.retry.ClouderaManagerApiFactory;
import com.sequenceiq.cloudbreak.cm.exception.ClouderaManagerOperationFailedException;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerPollingServiceProvider;
import com.sequenceiq.cloudbreak.cm.polling.PollingResultErrorHandler;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.polling.ExtendedPollingResult;

@ExtendWith(MockitoExtension.class)
class ClouderaManagerServiceManagementServiceTest {

    private static final String TEST_CLUSTER_NAME = "test-cluster-name";

    private static final String SERVICE_TYPE = "YARN";

    private static final String SERVICE_NAME = "yarn-1";

    private static final BigDecimal COMMAND_ID = BigDecimal.valueOf(123L);

    @InjectMocks
    private ClouderaManagerServiceManagementService underTest;

    @Mock
    private ClouderaManagerApiFactory clouderaManagerApiFactory;

    @Mock
    private ClouderaManagerPollingServiceProvider clouderaManagerPollingServiceProvider;

    @Mock
    private PollingResultErrorHandler pollingResultErrorHandler;

    @Mock
    private ServicesResourceApi servicesResourceApi;

    @Mock
    private ApiClient apiClient;

    @Mock
    private StackDtoDelegate stack;

    @BeforeEach
    void before() {
        lenient().when(stack.getName()).thenReturn(TEST_CLUSTER_NAME);
    }

    @Test
    void testReadServices() throws ApiException {
        when(clouderaManagerApiFactory.getServicesResourceApi(any())).thenReturn(servicesResourceApi);
        when(servicesResourceApi.readServices(any(), any())).thenReturn(new ApiServiceList());

        underTest.readServices(apiClient, "cluster");

        verify(servicesResourceApi).readServices(any(), any());
    }

    @Test
    public void testReadServicesFailure() throws ApiException {
        when(clouderaManagerApiFactory.getServicesResourceApi(any())).thenReturn(servicesResourceApi);
        when(servicesResourceApi.readServices(any(), any())).thenThrow(new ApiException("something"));

        assertThrows(ClouderaManagerOperationFailedException.class, () -> underTest.readServices(apiClient, "cluster"));

        verify(servicesResourceApi).readServices(any(), any());
    }

    @Test
    public void testStopServiceSuccess() throws Exception {
        ApiServiceList apiServiceList = new ApiServiceList().addItemsItem(new ApiService().name(SERVICE_NAME).type(SERVICE_TYPE));
        when(servicesResourceApi.readServices(TEST_CLUSTER_NAME, DataView.SUMMARY.name())).thenReturn(apiServiceList);
        when(clouderaManagerApiFactory.getServicesResourceApi(any())).thenReturn(servicesResourceApi);
        when(servicesResourceApi.stopCommand(TEST_CLUSTER_NAME, SERVICE_NAME)).thenReturn(new ApiCommand().id(COMMAND_ID));
        when(clouderaManagerPollingServiceProvider.startPollingServiceStop(stack, apiClient, COMMAND_ID)).thenReturn(mock(ExtendedPollingResult.class));

        underTest.stopClouderaManagerService(apiClient, stack, SERVICE_TYPE, true);

        verify(servicesResourceApi, times(1)).stopCommand(eq(TEST_CLUSTER_NAME), eq(SERVICE_NAME));
        verify(clouderaManagerPollingServiceProvider).startPollingServiceStop(stack, apiClient, COMMAND_ID);
        verify(pollingResultErrorHandler).handlePollingResult(any(ExtendedPollingResult.class), any(), any());
    }

    @Test
    public void testStopServiceSuccessWhenNotNecessaryToWaitForCommandExecution() throws Exception {
        ApiServiceList apiServiceList = new ApiServiceList().addItemsItem(new ApiService().name(SERVICE_NAME).type(SERVICE_TYPE));
        when(servicesResourceApi.readServices(TEST_CLUSTER_NAME, DataView.SUMMARY.name())).thenReturn(apiServiceList);
        when(clouderaManagerApiFactory.getServicesResourceApi(any())).thenReturn(servicesResourceApi);
        when(servicesResourceApi.stopCommand(TEST_CLUSTER_NAME, SERVICE_NAME)).thenReturn(new ApiCommand().id(COMMAND_ID));

        underTest.stopClouderaManagerService(apiClient, stack, SERVICE_TYPE, false);

        verify(servicesResourceApi, times(1)).stopCommand(eq(TEST_CLUSTER_NAME), eq(SERVICE_NAME));
        verifyNoInteractions(clouderaManagerPollingServiceProvider);
        verifyNoInteractions(pollingResultErrorHandler);
    }

    @Test
    public void testStopServiceWhenTheServiceAlreadyStopped() throws Exception {
        ApiServiceList apiServiceList = new ApiServiceList().addItemsItem(new ApiService().name(SERVICE_NAME).type(SERVICE_TYPE).serviceState(STOPPED));
        when(servicesResourceApi.readServices(TEST_CLUSTER_NAME, DataView.SUMMARY.name())).thenReturn(apiServiceList);
        when(clouderaManagerApiFactory.getServicesResourceApi(any())).thenReturn(servicesResourceApi);

        underTest.stopClouderaManagerService(apiClient, stack, SERVICE_TYPE, true);

        verify(servicesResourceApi, times(0)).stopCommand(TEST_CLUSTER_NAME, SERVICE_NAME);
    }

    @Test
    public void testStopServiceNoServiceFound() throws Exception {
        ApiServiceList apiServiceList = new ApiServiceList().addItemsItem(new ApiService().name(SERVICE_NAME).type("HUE"));
        when(servicesResourceApi.readServices(TEST_CLUSTER_NAME, DataView.SUMMARY.name())).thenReturn(apiServiceList);
        when(clouderaManagerApiFactory.getServicesResourceApi(any())).thenReturn(servicesResourceApi);

        underTest.stopClouderaManagerService(apiClient, stack, SERVICE_TYPE, true);

        verify(servicesResourceApi, times(0)).stopCommand(TEST_CLUSTER_NAME, SERVICE_NAME);
    }

    @Test
    public void testStartServiceSuccess() throws Exception {
        ApiServiceList apiServiceList = new ApiServiceList().addItemsItem(new ApiService().name(SERVICE_NAME).type(SERVICE_TYPE));
        when(servicesResourceApi.readServices(TEST_CLUSTER_NAME, DataView.SUMMARY.name())).thenReturn(apiServiceList);
        when(clouderaManagerApiFactory.getServicesResourceApi(any())).thenReturn(servicesResourceApi);
        when(servicesResourceApi.startCommand(TEST_CLUSTER_NAME, SERVICE_NAME)).thenReturn(new ApiCommand().id(COMMAND_ID));
        when(clouderaManagerPollingServiceProvider.startPollingServiceStart(stack, apiClient, COMMAND_ID)).thenReturn(mock(ExtendedPollingResult.class));

        underTest.startClouderaManagerService(apiClient, stack, SERVICE_TYPE, true);

        verify(servicesResourceApi, times(1)).startCommand(eq(TEST_CLUSTER_NAME), eq(SERVICE_NAME));
        verify(clouderaManagerPollingServiceProvider).startPollingServiceStart(stack, apiClient, COMMAND_ID);
        verify(pollingResultErrorHandler).handlePollingResult(any(ExtendedPollingResult.class), any(), any());
    }

    @Test
    public void testStopServiceWhenTheServiceAlreadyStarted() throws Exception {
        ApiServiceList apiServiceList = new ApiServiceList().addItemsItem(new ApiService().name(SERVICE_NAME).type(SERVICE_TYPE).serviceState(STARTED));
        when(servicesResourceApi.readServices(TEST_CLUSTER_NAME, DataView.SUMMARY.name())).thenReturn(apiServiceList);
        when(clouderaManagerApiFactory.getServicesResourceApi(any())).thenReturn(servicesResourceApi);

        underTest.startClouderaManagerService(apiClient, stack, SERVICE_TYPE, true);

        verify(servicesResourceApi, times(0)).startCommand(TEST_CLUSTER_NAME, SERVICE_NAME);
    }

    @Test
    public void testStartServiceNoServiceFound() throws Exception {
        ApiServiceList apiServiceList = new ApiServiceList().addItemsItem(new ApiService().name(SERVICE_NAME).type("HUE"));
        when(servicesResourceApi.readServices(TEST_CLUSTER_NAME, DataView.SUMMARY.name())).thenReturn(apiServiceList);
        when(clouderaManagerApiFactory.getServicesResourceApi(any())).thenReturn(servicesResourceApi);

        underTest.startClouderaManagerService(apiClient, stack, SERVICE_TYPE, true);

        verify(servicesResourceApi, times(0)).startCommand(TEST_CLUSTER_NAME, SERVICE_NAME);
    }

    @Test
    public void testDeleteServiceSuccess() throws Exception {
        ApiServiceList apiServiceList = new ApiServiceList().addItemsItem(new ApiService().name(SERVICE_NAME).type(SERVICE_TYPE));
        when(servicesResourceApi.readServices(TEST_CLUSTER_NAME, DataView.SUMMARY.name())).thenReturn(apiServiceList);
        when(clouderaManagerApiFactory.getServicesResourceApi(any())).thenReturn(servicesResourceApi);
        when(clouderaManagerPollingServiceProvider.startPollingServiceDeletion(stack, apiClient, SERVICE_TYPE))
                .thenReturn(mock(ExtendedPollingResult.class));

        underTest.deleteClouderaManagerService(apiClient, stack, SERVICE_TYPE);

        verify(servicesResourceApi, times(1)).deleteService(eq(TEST_CLUSTER_NAME), eq(SERVICE_NAME));
        verify(clouderaManagerPollingServiceProvider).startPollingServiceDeletion(stack, apiClient, SERVICE_TYPE);
        verify(pollingResultErrorHandler).handlePollingResult(any(ExtendedPollingResult.class), any(), any());
    }

    @Test
    public void testDeleteServiceNoServiceFound() throws Exception {
        ApiServiceList apiServiceList = new ApiServiceList().addItemsItem(new ApiService().name(SERVICE_NAME).type("HUE"));
        when(servicesResourceApi.readServices(TEST_CLUSTER_NAME, DataView.SUMMARY.name())).thenReturn(apiServiceList);
        when(clouderaManagerApiFactory.getServicesResourceApi(any())).thenReturn(servicesResourceApi);

        underTest.deleteClouderaManagerService(apiClient, stack, SERVICE_TYPE);

        verify(servicesResourceApi, times(0)).deleteService(TEST_CLUSTER_NAME, SERVICE_NAME);
    }

}