package com.sequenceiq.cloudbreak.cm;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.api.swagger.MgmtServiceResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiCommand;
import com.cloudera.api.swagger.model.ApiService;
import com.cloudera.api.swagger.model.ApiServiceState;
import com.sequenceiq.cloudbreak.cm.client.retry.ClouderaManagerApiFactory;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerPollingServiceProvider;
import com.sequenceiq.cloudbreak.domain.stack.Stack;

@ExtendWith(MockitoExtension.class)
class ClouderaManagerMgmtLaunchServiceTest {

    @InjectMocks
    private ClouderaManagerMgmtLaunchService underTest;

    @Mock
    private ClouderaManagerApiFactory clouderaManagerApiFactory;

    @Mock
    private ClouderaManagerPollingServiceProvider clouderaManagerPollingServiceProvider;

    @Mock
    private ApiClient apiClient;

    private Stack stack = new Stack();

    @Test
    void testStartManagementServicesShouldSendStartCommandWhenTheManagementServicesAreNotStarted() throws ApiException {
        MgmtServiceResourceApi mgmtServiceResourceApi = mock(MgmtServiceResourceApi.class);
        ApiService apiService = mock(ApiService.class);
        ApiCommand apiCommand = getApiCommand();

        when(clouderaManagerApiFactory.getMgmtServiceResourceApi(apiClient)).thenReturn(mgmtServiceResourceApi);
        when(mgmtServiceResourceApi.readService(DataView.SUMMARY.name())).thenReturn(apiService);
        when(apiService.getServiceState()).thenReturn(ApiServiceState.STOPPED);
        when(mgmtServiceResourceApi.startCommand()).thenReturn(apiCommand);

        underTest.startManagementServices(stack, apiClient);

        verify(mgmtServiceResourceApi).readService(DataView.SUMMARY.name());
        verify(mgmtServiceResourceApi).startCommand();
        verify(clouderaManagerPollingServiceProvider).startPollingCmManagementServiceStartup(stack, apiClient, apiCommand.getId());
    }

    @Test
    void testStartManagementServicesShouldNotSendStartCommandWhenTheManagementServicesAreStarted() throws ApiException {
        MgmtServiceResourceApi mgmtServiceResourceApi = mock(MgmtServiceResourceApi.class);
        ApiService apiService = mock(ApiService.class);

        when(clouderaManagerApiFactory.getMgmtServiceResourceApi(apiClient)).thenReturn(mgmtServiceResourceApi);
        when(mgmtServiceResourceApi.readService(DataView.SUMMARY.name())).thenReturn(apiService);
        when(apiService.getServiceState()).thenReturn(ApiServiceState.STARTED);

        underTest.startManagementServices(stack, apiClient);

        verify(mgmtServiceResourceApi).readService(DataView.SUMMARY.name());
        verifyNoMoreInteractions(mgmtServiceResourceApi);
        verifyNoInteractions(clouderaManagerPollingServiceProvider);
    }

    private ApiCommand getApiCommand() {
        ApiCommand apiCommand = new ApiCommand();
        apiCommand.setId(BigDecimal.TEN);
        return apiCommand;
    }

}
