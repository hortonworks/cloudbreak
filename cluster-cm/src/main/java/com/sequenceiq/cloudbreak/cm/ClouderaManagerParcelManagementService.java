package com.sequenceiq.cloudbreak.cm;

import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cloudera.api.swagger.ClouderaManagerResourceApi;
import com.cloudera.api.swagger.ParcelResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiCommand;
import com.cloudera.api.swagger.model.ApiConfig;
import com.cloudera.api.swagger.model.ApiConfigList;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cm.model.ParcelResource;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerPollingServiceProvider;
import com.sequenceiq.cloudbreak.cm.polling.PollingResultErrorHandler;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.polling.PollingResult;
import com.sequenceiq.cloudbreak.service.CloudbreakException;

@Service
class ClouderaManagerParcelManagementService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerParcelManagementService.class);

    @Inject
    private ClouderaManagerPollingServiceProvider clouderaManagerPollingServiceProvider;

    @Inject
    private PollingResultErrorHandler pollingResultErrorHandler;

    void setParcelRepos(Set<ClouderaManagerProduct> products, ClouderaManagerResourceApi clouderaManagerResourceApi) throws ApiException {
        Set<String> stackProductParcels = products.stream()
                .map(ClouderaManagerProduct::getParcel)
                .collect(Collectors.toSet());
        LOGGER.info("Setting parcel repo to {}", stackProductParcels);
        ApiConfigList apiConfigList = new ApiConfigList()
                .addItemsItem(new ApiConfig()
                        .name("remote_parcel_repo_urls")
                        .value(String.join(",", stackProductParcels)));
        clouderaManagerResourceApi.updateConfig("Updated configurations.", apiConfigList);
    }

    void refreshParcelRepos(ClouderaManagerResourceApi clouderaManagerResourceApi, Stack stack, ApiClient apiClient) {
        try {
            LOGGER.info("Refreshing parcel repos.");
            ApiCommand apiCommand = clouderaManagerResourceApi.refreshParcelRepos();
            clouderaManagerPollingServiceProvider.startPollingCmParcelRepositoryRefresh(stack, apiClient, apiCommand.getId());
        } catch (ApiException e) {
            LOGGER.info("Unable to refresh parcel repo", e);
            throw new ClouderaManagerOperationFailedException(e.getMessage(), e);
        }
    }

    void downloadParcels(Set<ClouderaManagerProduct> products, ParcelResourceApi parcelResourceApi, Stack stack, ApiClient apiClient)
            throws ApiException, CloudbreakException {
        for (ClouderaManagerProduct product : products) {
            LOGGER.info("Downloading {} parcel.", product.getName());
            ApiCommand apiCommand = parcelResourceApi.startDownloadCommand(stack.getName(), product.getName(), product.getVersion());
            PollingResult pollingResult = clouderaManagerPollingServiceProvider.startPollingCdpRuntimeParcelDownload(
                    stack, apiClient, apiCommand.getId(), new ParcelResource(stack.getName(), product.getName(), product.getVersion()));
            handlePollingResult(pollingResult, "Cluster was terminated while waiting for CDP Runtime Parcel to be downloaded",
                    "Timeout during the updated CDP Runtime Parcel download.");
        }
    }

    void distributeParcels(Set<ClouderaManagerProduct> products, ParcelResourceApi parcelResourceApi, Stack stack, ApiClient apiClient)
            throws ApiException, CloudbreakException {
        for (ClouderaManagerProduct product : products) {
            LOGGER.info("Distributing downloaded {} parcel", product.getName());
            ApiCommand apiCommand = parcelResourceApi.startDistributionCommand(stack.getName(), product.getName(), product.getVersion());
            PollingResult pollingResult = clouderaManagerPollingServiceProvider.startPollingCdpRuntimeParcelDistribute(
                    stack, apiClient, apiCommand.getId(), new ParcelResource(stack.getName(), product.getName(), product.getVersion()));
            handlePollingResult(pollingResult, "Cluster was terminated while waiting for CDP Runtime Parcel to be distributed",
                    "Timeout during the updated CDP Runtime Parcel distribution.");
        }
    }

    void activateParcels(Set<ClouderaManagerProduct> products, ParcelResourceApi parcelResourceApi, Stack stack, ApiClient apiClient)
            throws ApiException, CloudbreakException {
        for (ClouderaManagerProduct product : products) {
            String productName = product.getName();
            LOGGER.info("Activating {} parcel", productName);
            ApiCommand apiCommand = parcelResourceApi.activateCommand(stack.getName(), productName, product.getVersion());
            PollingResult result = clouderaManagerPollingServiceProvider.startPollingCmSingleParcelActivation(stack, apiClient, apiCommand.getId(), product);
            handlePollingResult(result, "Cluster was terminated while waiting for CDP Runtime Parcel to be activated",
                    "Timeout during the updated CDP Runtime Parcel activation.");
        }
    }

    void checkParcelApiAvailability(Stack stack, ApiClient apiClient) throws CloudbreakException {
        LOGGER.debug("Checking if Parcels API is available");
        PollingResult pollingResult = clouderaManagerPollingServiceProvider.startPollingParcelsApiAvailable(stack, apiClient);
        handlePollingResult(pollingResult, "Cluster was terminated while waiting for Parcels API to be available",
                "Timeout during waiting for CM Parcels API to be available.");
    }

    private void handlePollingResult(PollingResult pollingResult, String cancellationMessage, String timeoutMessage) throws CloudbreakException {
        pollingResultErrorHandler.handlePollingResult(pollingResult, cancellationMessage, timeoutMessage);
    }
}
