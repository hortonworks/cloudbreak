package com.sequenceiq.cloudbreak.cm.polling.task;

import java.util.Objects;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudera.api.swagger.ServicesResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiService;
import com.cloudera.api.swagger.model.ApiServiceList;
import com.sequenceiq.cloudbreak.cluster.service.ClusterEventService;
import com.sequenceiq.cloudbreak.cm.DataView;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerApiPojoFactory;
import com.sequenceiq.cloudbreak.cm.exception.ClouderaManagerOperationFailedException;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerPollerObject;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;

public class ClouderaManagerServiceDeletionListenerTask extends AbstractClouderaManagerApiCheckerTask<ClouderaManagerPollerObject> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerServiceDeletionListenerTask.class);

    private final String serviceType;

    public ClouderaManagerServiceDeletionListenerTask(ClouderaManagerApiPojoFactory clouderaManagerApiPojoFactory,
            ClusterEventService clusterEventService, String serviceType) {
        super(clouderaManagerApiPojoFactory, clusterEventService);
        this.serviceType = serviceType;
    }

    @Override
    protected boolean doStatusCheck(ClouderaManagerPollerObject pollerObject) throws ApiException {
        ApiClient apiClient = pollerObject.getApiClient();
        StackDtoDelegate stack = pollerObject.getStack();
        Optional<ApiService> apiService = findServiceOnCluster(stack.getName(), serviceType, apiClient);
        if (apiService.isEmpty()) {
            LOGGER.debug("The {} service successfully deleted.", serviceType);
            return true;
        } else {
            LOGGER.debug("Service deletion is in progress. Current status is: {}", apiService.map(ApiService::getServiceState).map(Objects::toString));
            return false;
        }
    }

    @Override
    protected String getPollingName() {
        return "Service deletion";
    }

    private Optional<ApiService> findServiceOnCluster(String clusterName, String serviceName, ApiClient apiClient) {
        try {
            LOGGER.debug("Looking for service of name {} in cluster {}", serviceName, clusterName);
            ServicesResourceApi servicesResourceApi = clouderaManagerApiPojoFactory.getServicesResourceApi(apiClient);
            ApiServiceList serviceList = servicesResourceApi.readServices(clusterName, DataView.SUMMARY.name());
            return serviceList.getItems().stream()
                    .filter(service -> serviceName.equals(service.getName()))
                    .findFirst();
        } catch (ApiException e) {
            String errorMessage = String.format("Failed to get %s service name from Cloudera Manager.", serviceName);
            LOGGER.debug(errorMessage, e);
            throw new ClouderaManagerOperationFailedException(errorMessage, e);
        }
    }

}
