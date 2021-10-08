package com.sequenceiq.cloudbreak.cm;

import static com.sequenceiq.cloudbreak.polling.PollingResult.isExited;
import static com.sequenceiq.cloudbreak.polling.PollingResult.isTimeout;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cloudera.api.swagger.ParcelResourceApi;
import com.cloudera.api.swagger.ParcelsResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiParcel;
import com.cloudera.api.swagger.model.ApiParcelList;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cloud.scheduler.CancellationException;
import com.sequenceiq.cloudbreak.cm.model.ParcelStatus;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerPollingServiceProvider;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.polling.PollingResult;

@Service
class ClouderaManagerParcelDecommissionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerParcelDecommissionService.class);

    @Inject
    private ClouderaManagerPollingServiceProvider clouderaManagerPollingServiceProvider;

    public Map<String, String> getParcelsInStatus(ParcelsResourceApi parcelsResourceApi, String stackName, ParcelStatus parcelStatus) {
        try {
            return getClouderaManagerParcelsByStatus(parcelsResourceApi, stackName, parcelStatus)
                    .stream()
                    .collect(Collectors.toMap(ApiParcel::getProduct, ApiParcel::getVersion));
        } catch (ApiException e) {
            LOGGER.info("Unable to fetch the list of activated parcels", e);
            throw new ClouderaManagerOperationFailedException("Unable to fetch the list of activated parcels", e);
        }
    }

    public void deactivateUnusedParcels(ParcelsResourceApi parcelsResourceApi, ParcelResourceApi parcelResourceApi, String stackName, Map<String,
            ClouderaManagerProduct> cmProducts) {
        Map<String, String> installedComponents = getParcelsInStatus(parcelsResourceApi, stackName, ParcelStatus.ACTIVATED);
        Map<String, String> filteredParcels = installedComponents.entrySet().stream()
                .filter(entry -> !cmProducts.containsKey(entry.getKey()))
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
        deactivateParcels(parcelResourceApi, stackName, filteredParcels);
    }

    public void undistributeUnusedParcels(ApiClient apiClient, ParcelsResourceApi parcelsResourceApi, ParcelResourceApi parcelResourceApi, Stack stack,
            Map<String, ClouderaManagerProduct> cmProducts) {
        Map<String, String> distributedComponents = getParcelsInStatus(parcelsResourceApi, stack.getName(), ParcelStatus.DISTRIBUTED);
        Map<String, String> filteredParcels = distributedComponents.entrySet().stream()
                .filter(entry -> !cmProducts.containsKey(entry.getKey()))
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
        undistributeParcels(apiClient, parcelResourceApi, stack, filteredParcels);
    }

    public void removeUnusedParcels(ApiClient apiClient, ParcelsResourceApi parcelsResourceApi, ParcelResourceApi parcelResourceApi, Stack stack,
            Map<String, ClouderaManagerProduct> cmProducts) {
        Map<String, String> downloadedParcels = getParcelsInStatus(parcelsResourceApi, stack.getName(), ParcelStatus.DOWNLOADED);
        Map<String, String> filteredParcels = downloadedParcels.entrySet().stream()
                .filter(entry -> !cmProducts.containsKey(entry.getKey()))
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
        removeParcels(apiClient, parcelResourceApi, stack, filteredParcels);
    }

    public void removeUnusedParcelVersions(ApiClient apiClient, ParcelsResourceApi parcelsResourceApi, ParcelResourceApi parcelResourceApi, Stack stack,
            String parcel, String usedVersion) throws ApiException {
        Map<String, String> unusedDistributedParcelVersions =
                getClouderaManagerParcelsByStatus(parcelsResourceApi, stack.getName(), ParcelStatus.DISTRIBUTED).stream()
                        .filter(apiParcel -> apiParcel.getProduct().equals(parcel) && !apiParcel.getVersion().equals(usedVersion))
                        .collect(Collectors.toMap(ApiParcel::getProduct, ApiParcel::getVersion));
        undistributeParcels(apiClient, parcelResourceApi, stack, unusedDistributedParcelVersions);

        Map<String, String> unusedDownloadedParcelVersions =
                getClouderaManagerParcelsByStatus(parcelsResourceApi, stack.getName(), ParcelStatus.DOWNLOADED).stream()
                        .filter(apiParcel -> apiParcel.getProduct().equals(parcel) && !apiParcel.getVersion().equals(usedVersion))
                        .collect(Collectors.toMap(ApiParcel::getProduct, ApiParcel::getVersion));
        removeParcels(apiClient, parcelResourceApi, stack, unusedDownloadedParcelVersions);
    }

    private void deactivateParcels(ParcelResourceApi parcelResourceApi, String stackName, Map<String, String> installedComponents) {
        Set<String> failedDeactivations = new HashSet<>();
        for (Map.Entry<String, String> installedComp : installedComponents.entrySet()) {
            String product = "[" + installedComp.getKey() + ":" + installedComp.getValue() + "]";
            try {
                LOGGER.debug("Deactivating {} product", product);
                parcelResourceApi.deactivateCommand(stackName, installedComp.getKey(), installedComp.getValue());
            } catch (ApiException e) {
                LOGGER.info(String.format("Unable to deactivate product: %s", product), e);
                failedDeactivations.add(product);
            }
        }
        if (!failedDeactivations.isEmpty()) {
            throw new ClouderaManagerOperationFailedException(String.format("Deactivation failed on the following products: %s", failedDeactivations));
        }
    }

    private void undistributeParcels(ApiClient apiClient, ParcelResourceApi parcelResourceApi, Stack stack, Map<String, String> parcels) {
        Set<String> failedDistribution = new HashSet<>();
        for (Map.Entry<String, String> distributedComponent : parcels.entrySet()) {
            String product = "[" + distributedComponent.getKey() + ":" + distributedComponent.getValue() + "]";
            try {
                LOGGER.debug("Undistributing {} product", product);
                parcelResourceApi.startRemovalOfDistributionCommand(stack.getName(), distributedComponent.getKey(), distributedComponent.getValue());
            } catch (ApiException e) {
                LOGGER.info(String.format("Unable to undistribute product: %s", product), e);
                failedDistribution.add(product);
            }
        }
        if (!failedDistribution.isEmpty()) {
            throw new ClouderaManagerOperationFailedException(String.format("Undistribution failed on the following products: %s", failedDistribution));
        }
        PollingResult pollingResult = clouderaManagerPollingServiceProvider.startPollingCmParcelStatus(stack, apiClient, parcels,
                ParcelStatus.DOWNLOADED);
        if (isExited(pollingResult)) {
            throw new CancellationException("Cluster was terminated while waiting for parcels undistribution");
        } else if (isTimeout(pollingResult)) {
            throw new ClouderaManagerOperationFailedException("Timeout while Cloudera Manager undistribute parcels.");
        }
    }

    private void removeParcels(ApiClient apiClient, ParcelResourceApi parcelResourceApi, Stack stack, Map<String, String> parcels) {
        Set<String> failedDeletion = new HashSet<>();
        for (Map.Entry<String, String> downloadedParcel : parcels.entrySet()) {
            String product = "[" + downloadedParcel.getKey() + ":" + downloadedParcel.getValue() + "]";
            try {
                LOGGER.debug("Removing {} product", product);
                parcelResourceApi.removeDownloadCommand(stack.getName(), downloadedParcel.getKey(), downloadedParcel.getValue());
            } catch (ApiException e) {
                LOGGER.info(String.format("Unable to delete product: %s", product), e);
                failedDeletion.add(product);
            }
        }
        if (!failedDeletion.isEmpty()) {
            throw new ClouderaManagerOperationFailedException(String.format("Deletion failed on the following products: %s", failedDeletion));
        }
        PollingResult pollingResult = clouderaManagerPollingServiceProvider.startPollingCmParcelDelete(stack, apiClient, parcels);
        if (isExited(pollingResult)) {
            throw new CancellationException("Cluster was terminated while waiting for parcels deletion");
        } else if (isTimeout(pollingResult)) {
            throw new ClouderaManagerOperationFailedException("Timeout while Cloudera Manager deletes parcels.");
        }
    }

    private List<ApiParcel> getClouderaManagerParcelsByStatus(ParcelsResourceApi parcelsResourceApi, String stackName, ParcelStatus parcelStatus)
            throws ApiException {
        ApiParcelList parcelList = getClouderaManagerParcels(parcelsResourceApi, stackName);
        return parcelList.getItems()
                .stream()
                .filter(parcel -> parcelStatus.name().equals(parcel.getStage()))
                .peek(parcel -> LOGGER.debug("Parcel {} is found with status {}", parcel.getDisplayName(), parcelStatus))
                .collect(toList());
    }

    private ApiParcelList getClouderaManagerParcels(ParcelsResourceApi parcelsResourceApi, String stackName) throws ApiException {
        return parcelsResourceApi.readParcels(stackName, "summary");
    }
}
