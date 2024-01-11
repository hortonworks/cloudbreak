package com.sequenceiq.cloudbreak.cm;

import java.util.Objects;
import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

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
import com.sequenceiq.cloudbreak.service.CloudbreakException;

@Service
public class ClouderaManagerServiceManagementService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerServiceManagementService.class);

    @Inject
    private ClouderaManagerApiFactory clouderaManagerApiFactory;

    @Inject
    private ClouderaManagerPollingServiceProvider clouderaManagerPollingServiceProvider;

    @Inject
    private PollingResultErrorHandler pollingResultErrorHandler;

    public ApiServiceList readServices(ApiClient client, String clusterName) {
        try {
            LOGGER.debug("Reading services of Cloudera Manager for cluster {}.", clusterName);
            ServicesResourceApi servicesResourceApi = clouderaManagerApiFactory.getServicesResourceApi(client);
            return servicesResourceApi.readServices(clusterName, DataView.SUMMARY.name());
        } catch (ApiException e) {
            LOGGER.error("Failed to get services from Cloudera Manager.", e);
            throw new ClouderaManagerOperationFailedException(e.getMessage(), e);
        }
    }

    public void startClouderaManagerService(ApiClient client, StackDtoDelegate stack, String serviceType) {
        LOGGER.info("Trying to start services for service type: {} for cluster {}", serviceType, stack.getName());
        ServicesResourceApi servicesResourceApi = clouderaManagerApiFactory.getServicesResourceApi(client);
        performClouderaManagerOperation(client, servicesResourceApi, stack, serviceType, "start", servicesResourceApi::startCommand,
                clouderaManagerPollingServiceProvider::startPollingServiceStart);
    }

    public void stopClouderaManagerService(ApiClient client, StackDtoDelegate stack, String serviceType) {
        LOGGER.info("Trying to stop services for service type: {} for cluster {}", serviceType, stack.getName());
        ServicesResourceApi servicesResourceApi = clouderaManagerApiFactory.getServicesResourceApi(client);
        performClouderaManagerOperation(client, servicesResourceApi, stack, serviceType, "stop", servicesResourceApi::stopCommand,
                clouderaManagerPollingServiceProvider::startPollingServiceStop);
    }

    public void deleteClouderaManagerService(ApiClient client, StackDtoDelegate stack, String serviceType) {
        LOGGER.debug("Trying to delete services for service type: {} for cluster {}", serviceType, stack.getName());
        ServicesResourceApi servicesResourceApi = clouderaManagerApiFactory.getServicesResourceApi(client);
        findServiceOnCluster(stack.getName(), serviceType, servicesResourceApi)
                .ifPresentOrElse(apiService -> {
                            try {
                                LOGGER.debug("Executing delete command on stack {} and service {}", stack.getName(), serviceType);
                                servicesResourceApi.deleteService(stack.getName(), apiService.getName());
                                ExtendedPollingResult result = clouderaManagerPollingServiceProvider.startPollingServiceDeletion(stack, client, serviceType);
                                pollingResultErrorHandler.handlePollingResult(result,
                                        String.format("Cluster was terminated while %s service deletion is running.", serviceType),
                                        String.format("Timeout happened while %s service deletion is running.", serviceType));
                            } catch (Exception e) {
                                LOGGER.error("Failed to delete services for service type: {} for cluster {}", serviceType, stack.getName(), e);
                                throw new ClouderaManagerOperationFailedException(e.getMessage(), e);
                            }
                        }, () -> LOGGER.debug("Unable to delete {} service because is not present on the cluster", serviceType));
    }

    private void performClouderaManagerOperation(ApiClient client, ServicesResourceApi servicesResourceApi, StackDtoDelegate stack, String serviceType,
            String operationName, ClouderaManagerServicesCommand clouderaManagerServicesCommand,
            ClouderaManagerOperationPollerCommand clouderaManagerOperationPollerCommand) {
        Optional<ApiService> apiService = findServiceOnCluster(stack.getName(), serviceType, servicesResourceApi);
        if (apiService.isPresent()) {
            try {
                LOGGER.debug("Executing {} command on stack {} and service {}", operationName, stack.getName(), serviceType);
                ApiCommand apiCommand = clouderaManagerServicesCommand.apply(stack.getName(), apiService.get().getName());
                waitForCommandExecution(client, stack, apiCommand, operationName, clouderaManagerOperationPollerCommand);
            } catch (Exception e) {
                LOGGER.error("Failed to {} services for service type: {} for cluster {}", operationName, serviceType, stack.getName(), e);
                throw new ClouderaManagerOperationFailedException(e.getMessage(), e);
            }
        } else {
            LOGGER.debug("Unable to {} {} service because is not present on the cluster", operationName, serviceType);
        }
    }

    private Optional<ApiService> findServiceOnCluster(String clusterName, String serviceType, ServicesResourceApi servicesResourceApi) {
        try {
            LOGGER.debug("Looking for service of name {} in cluster {}", serviceType, clusterName);
            ApiServiceList serviceList = servicesResourceApi.readServices(clusterName, DataView.SUMMARY.name());
            return serviceList.getItems().stream()
                    .filter(service -> serviceType.equals(service.getType()))
                    .findFirst();
        } catch (ApiException e) {
            String errorMessage = String.format("Failed to get %s service name from Cloudera Manager.", serviceType);
            LOGGER.debug(errorMessage, e);
            throw new ClouderaManagerOperationFailedException(errorMessage, e);
        }
    }

    private void waitForCommandExecution(ApiClient apiClient, StackDtoDelegate stack, ApiCommand apiCommand, String operationName,
            ClouderaManagerOperationPollerCommand clouderaManagerOperationPollerCommand) throws CloudbreakException {
        if (Objects.isNull(apiCommand)) {
            LOGGER.debug("There is no running {} command.", operationName);
        } else {
            LOGGER.debug("Start polling {} command. The command ID is: {}", apiCommand.getId(), operationName);
            ExtendedPollingResult pollingResult = clouderaManagerOperationPollerCommand.apply(stack, apiClient, apiCommand.getId());
            pollingResultErrorHandler.handlePollingResult(pollingResult,
                    String.format("Cluster was terminated while %s services command is running.", operationName),
                    String.format("Timeout happened while %s services command is running.", operationName));
        }
    }
}
