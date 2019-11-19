package com.sequenceiq.cloudbreak.cm;

import java.util.Optional;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.MgmtServiceResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiCommand;
import com.cloudera.api.swagger.model.ApiService;
import com.cloudera.api.swagger.model.ApiServiceState;
import com.sequenceiq.cloudbreak.cm.client.retry.ClouderaManagerApiFactory;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerPollingServiceProvider;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.polling.PollingResult;

@Component
class ClouderaManagerMgmtLaunchService {

    @Inject
    private ClouderaManagerApiFactory clouderaManagerApiFactory;

    @Inject
    private ClouderaManagerPollingServiceProvider clouderaManagerPollingServiceProvider;

    void startManagementServices(Stack stack, ApiClient apiClient) throws ApiException {
        MgmtServiceResourceApi mgmtServiceResourceApi = getMgmtServiceResourceApi(apiClient);
        ApiService mgmtService = getMgmtService(mgmtServiceResourceApi);
        Optional<ApiCommand> startCommand = Optional.empty();
        ApiServiceState serviceState = mgmtService.getServiceState();
        if (serviceState != ApiServiceState.STARTED && serviceState != ApiServiceState.STARTING) {
            startCommand = Optional.of(mgmtServiceResourceApi.startCommand());
        }
        startCommand.ifPresent(command -> startPolling(stack, apiClient, command));
    }

    private MgmtServiceResourceApi getMgmtServiceResourceApi(ApiClient apiClient) {
        return clouderaManagerApiFactory.getMgmtServiceResourceApi(apiClient);
    }

    private ApiService getMgmtService(MgmtServiceResourceApi mgmtServiceResourceApi) throws ApiException {
        return mgmtServiceResourceApi.readService(DataView.SUMMARY.name());
    }

    private PollingResult startPolling(Stack stack, ApiClient apiClient, ApiCommand sc) {
        return clouderaManagerPollingServiceProvider.startPollingCmManagementServiceStartup(stack, apiClient, sc.getId());
    }
}
