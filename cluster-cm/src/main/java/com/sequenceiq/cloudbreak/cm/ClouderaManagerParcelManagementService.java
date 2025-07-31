package com.sequenceiq.cloudbreak.cm;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.auth.PaywallCredentialPopulator.ARCHIVE_URL_PATTERN;
import static java.util.Objects.requireNonNull;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cloudera.api.swagger.ClouderaManagerResourceApi;
import com.cloudera.api.swagger.ParcelResourceApi;
import com.cloudera.api.swagger.ParcelsResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiCommand;
import com.cloudera.api.swagger.model.ApiConfig;
import com.cloudera.api.swagger.model.ApiConfigList;
import com.cloudera.api.swagger.model.ApiParcel;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cluster.model.ParcelInfo;
import com.sequenceiq.cloudbreak.cluster.model.ParcelStatus;
import com.sequenceiq.cloudbreak.cm.exception.ClouderaManagerOperationFailedException;
import com.sequenceiq.cloudbreak.cm.model.ParcelResource;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerPollingServiceProvider;
import com.sequenceiq.cloudbreak.cm.polling.PollingResultErrorHandler;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.polling.ExtendedPollingResult;
import com.sequenceiq.cloudbreak.polling.PollingResult;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;

@Service
class ClouderaManagerParcelManagementService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerParcelManagementService.class);

    @Inject
    private ClouderaManagerPollingServiceProvider clouderaManagerPollingServiceProvider;

    @Inject
    private PollingResultErrorHandler pollingResultErrorHandler;

    @Inject
    private CloudbreakEventService eventService;

    public Set<ParcelInfo> getParcelsInStatus(ParcelsResourceApi parcelsResourceApi, String stackName, ParcelStatus parcelStatus) {
        requireNonNull(parcelStatus, "Parcel status cannot be null");
        try {
            Set<ParcelInfo> parcelResponse = getAllParcels(parcelsResourceApi, stackName).stream()
                    .filter(parcelInfo -> parcelStatus.equals(parcelInfo.getStatus()))
                    .collect(Collectors.toSet());
            LOGGER.debug("The following parcels are found in {} status: {}", parcelStatus, parcelResponse);
            return parcelResponse;
        } catch (ApiException e) {
            String errorMessage = String.format("Unable to fetch the list of %s parcels due to: %s",
                    parcelStatus.name().toLowerCase(Locale.ROOT), e.getMessage());
            LOGGER.error(errorMessage, e);
            throw new ClouderaManagerOperationFailedException(errorMessage, e);
        }
    }

    public Set<ParcelInfo> getAllParcels(ParcelsResourceApi parcelsResourceApi, String stackName) throws ApiException {
        LOGGER.debug("Retrieving all available parcels from CM");
        return parcelsResourceApi.readParcels(stackName, "summary").getItems().stream()
                .map(apiParcel -> new ParcelInfo(apiParcel.getProduct(), apiParcel.getVersion(), parseParcelStatus(apiParcel)))
                .collect(Collectors.toSet());
    }

    private ParcelStatus parseParcelStatus(ApiParcel apiParcel) {
        try {
            return ParcelStatus.valueOf(apiParcel.getStage());
        } catch (IllegalArgumentException e) {
            LOGGER.warn("An unknown parcel status occurred: {} for parcel {}. Returning UNKNOWN state.", apiParcel.getStage(), apiParcel, e);
            return ParcelStatus.UNKNOWN;
        }
    }

    public void setParcelRepos(Set<ClouderaManagerProduct> products, ClouderaManagerResourceApi clouderaManagerResourceApi) throws ApiException {
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

    public void refreshParcelRepos(ClouderaManagerResourceApi clouderaManagerResourceApi, StackDtoDelegate stack, ApiClient apiClient) {
        try {
            LOGGER.info("Refreshing parcel repos.");
            ApiCommand apiCommand = clouderaManagerResourceApi.refreshParcelRepos();
            clouderaManagerPollingServiceProvider.startPollingCmParcelRepositoryRefresh(stack, apiClient, apiCommand.getId());
        } catch (ApiException e) {
            LOGGER.info("Unable to refresh parcel repo", e);
            throw new ClouderaManagerOperationFailedException(e.getMessage(), e);
        }
    }

    public void downloadParcels(Set<ClouderaManagerProduct> products, ParcelResourceApi parcelResourceApi, ParcelsResourceApi parcelsResourceApi,
            StackDtoDelegate stack, ApiClient apiClient) throws ApiException, CloudbreakException {
        Set<ParcelInfo> availableParcels = getAllParcels(parcelsResourceApi, stack.getName());
        for (ClouderaManagerProduct product : products) {
            if (parcelAvailableInStatus(product, availableParcels, ParcelStatus::isDownloaded)) {
                LOGGER.debug("{} parcel is already downloaded", product.getName());
            } else {
                LOGGER.info("Downloading {} parcel.", product.getName());
                fireCloudbreakEvent(stack, ResourceEvent.CLUSTER_UPGRADE_DOWNLOAD_PARCEL, product);
                ApiCommand apiCommand = parcelResourceApi.startDownloadCommand(stack.getName(), product.getName(), product.getVersion());
                ExtendedPollingResult pollingResult = clouderaManagerPollingServiceProvider.startPollingCdpRuntimeParcelDownload(
                        stack, apiClient, apiCommand.getId(), new ParcelResource(stack.getName(), product.getName(), product.getVersion()));
                handlePollingResult(pollingResult.getPollingResult(), "Cluster was terminated while waiting for CDP Runtime Parcel to be downloaded",
                        isArchiveUrl(product) ? "The upgrade process encountered issues due to the degradation of archive.cloudera.com or the build cache, "
                                + "resulting in the inability to download the parcels within 120 minutes. "
                                + "If this issue persists, please contact our Support team for assistance."
                                : "The upgrade process encountered issues due to the degradation of build-cache.vpc.cloudera.com or the build cache, "
                                + "resulting in the inability to download the parcels within 120 minutes. "
                                + "If this issue persists, please contact our Release Engineering team for assistance.");
            }
        }
    }

    private boolean isArchiveUrl(ClouderaManagerProduct product) {
        return Optional.ofNullable(product.getParcel()).map(parcel -> ARCHIVE_URL_PATTERN.matcher(parcel).find()).orElse(false);
    }

    public void distributeParcels(Set<ClouderaManagerProduct> products, ParcelResourceApi parcelResourceApi, ParcelsResourceApi parcelsResourceApi,
            StackDtoDelegate stack, ApiClient apiClient) throws ApiException, CloudbreakException {
        Set<ParcelInfo> availableParcels = getAllParcels(parcelsResourceApi, stack.getName());
        for (ClouderaManagerProduct product : products) {
            if (parcelAvailableInStatus(product, availableParcels, ParcelStatus::isDistributed)) {
                LOGGER.debug("{} parcel is already distributed", product.getName());
            } else {
                LOGGER.info("Distributing downloaded {} parcel", product.getName());
                fireCloudbreakEvent(stack, ResourceEvent.CLUSTER_UPGRADE_DISTRIBUTE_PARCEL, product);
                ApiCommand apiCommand = parcelResourceApi.startDistributionCommand(stack.getName(), product.getName(), product.getVersion());
                ExtendedPollingResult pollingResult = clouderaManagerPollingServiceProvider.startPollingCdpRuntimeParcelDistribute(
                        stack, apiClient, apiCommand.getId(), new ParcelResource(stack.getName(), product.getName(), product.getVersion()));
                handlePollingResult(pollingResult.getPollingResult(), "Cluster was terminated while waiting for CDP Runtime Parcel to be distributed",
                        "Timeout during the updated CDP Runtime Parcel distribution.");
            }
        }
    }

    public void activateParcels(Set<ClouderaManagerProduct> products, ParcelResourceApi parcelResourceApi, ParcelsResourceApi parcelsResourceApi,
            StackDtoDelegate stack, ApiClient apiClient) throws ApiException, CloudbreakException {
        Set<ParcelInfo> availableParcels = getAllParcels(parcelsResourceApi, stack.getName());
        for (ClouderaManagerProduct product : products) {
            if (parcelAvailableInStatus(product, availableParcels, ParcelStatus::isActivated)) {
                LOGGER.debug("{} parcel is already activated", product.getName());
            } else {
                LOGGER.info("Activating {} parcel", product.getName());
                fireCloudbreakEvent(stack, ResourceEvent.CLUSTER_UPGRADE_ACTIVATE_PARCEL, product);
                ApiCommand apiCommand = parcelResourceApi.activateCommand(stack.getName(), product.getName(), product.getVersion());
                ExtendedPollingResult result = clouderaManagerPollingServiceProvider
                        .startPollingCmSingleParcelActivation(stack, apiClient, apiCommand.getId(), product);
                handlePollingResult(result.getPollingResult(), "Cluster was terminated while waiting for CDP Runtime Parcel to be activated",
                        "Timeout during the updated CDP Runtime Parcel activation.");
            }
        }
    }

    public void checkParcelApiAvailability(StackDtoDelegate stack, ApiClient apiClient) throws CloudbreakException {
        LOGGER.debug("Checking if Parcels API is available");
        ExtendedPollingResult pollingResult = clouderaManagerPollingServiceProvider.startPollingParcelsApiAvailable(stack, apiClient);
        handlePollingResult(pollingResult.getPollingResult(), "Cluster was terminated while waiting for Parcels API to be available",
                "Timeout during waiting for CM Parcels API to be available.");
    }

    private void handlePollingResult(PollingResult pollingResult, String cancellationMessage, String timeoutMessage) throws CloudbreakException {
        pollingResultErrorHandler.handlePollingResult(pollingResult, cancellationMessage, timeoutMessage);
    }

    private void fireCloudbreakEvent(StackDtoDelegate stack, ResourceEvent resourceEvent, ClouderaManagerProduct product) {
        eventService.fireCloudbreakEvent(stack.getId(), UPDATE_IN_PROGRESS.name(), resourceEvent, List.of(product.getName()));
    }

    private boolean parcelAvailableInStatus(ClouderaManagerProduct product, Set<ParcelInfo> availableParcels, Function<ParcelStatus, Boolean> isInStatus) {
        return availableParcels.stream().anyMatch(apiParcel ->
                apiParcel.getName().equals(product.getName())
                        && apiParcel.getVersion().equals(product.getVersion())
                        && isInStatus.apply(apiParcel.getStatus()));
    }
}
