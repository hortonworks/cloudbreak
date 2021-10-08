package com.sequenceiq.cloudbreak.cm;

import static com.sequenceiq.cloudbreak.polling.PollingResult.isExited;
import static com.sequenceiq.cloudbreak.polling.PollingResult.isTimeout;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Predicate;

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
import com.sequenceiq.cloudbreak.cluster.model.ParcelOperationStatus;
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
                    .collect(toMap(ApiParcel::getProduct, ApiParcel::getVersion));
        } catch (ApiException e) {
            LOGGER.info("Unable to fetch the list of activated parcels", e);
            throw new ClouderaManagerOperationFailedException("Unable to fetch the list of activated parcels", e);
        }
    }

    public ParcelOperationStatus deactivateUnusedParcels(ParcelsResourceApi parcelsResourceApi, ParcelResourceApi parcelResourceApi,
            String stackName, Map<String, ClouderaManagerProduct> cmProducts) {
        Map<String, String> installedComponents = getParcelsInStatus(parcelsResourceApi, stackName, ParcelStatus.ACTIVATED);
        Map<String, String> filteredParcels = installedComponents.entrySet().stream()
                .filter(entry -> !cmProducts.containsKey(entry.getKey()))
                .collect(toMap(Entry::getKey, Entry::getValue));
        return deactivateParcels(parcelResourceApi, stackName, filteredParcels);
    }

    public ParcelOperationStatus undistributeUnusedParcels(ApiClient apiClient, ParcelsResourceApi parcelsResourceApi,
            ParcelResourceApi parcelResourceApi, Stack stack, Map<String, ClouderaManagerProduct> cmProducts) {
        Map<String, String> distributedComponents = getParcelsInStatus(parcelsResourceApi, stack.getName(), ParcelStatus.DISTRIBUTED);
        Map<String, String> filteredParcels = distributedComponents.entrySet().stream()
                .filter(entry -> !cmProducts.containsKey(entry.getKey()))
                .collect(toMap(Entry::getKey, Entry::getValue));
        return undistributeParcels(apiClient, parcelResourceApi, stack, filteredParcels);
    }

    public ParcelOperationStatus removeUnusedParcels(ApiClient apiClient, ParcelsResourceApi parcelsResourceApi,
            ParcelResourceApi parcelResourceApi, Stack stack, Map<String, ClouderaManagerProduct> cmProducts) {
        Map<String, String> downloadedParcels = getParcelsInStatus(parcelsResourceApi, stack.getName(), ParcelStatus.DOWNLOADED);
        Map<String, String> filteredParcels = downloadedParcels.entrySet().stream()
                .filter(entry -> !cmProducts.containsKey(entry.getKey()))
                .collect(toMap(Entry::getKey, Entry::getValue));
        return removeParcels(apiClient, parcelResourceApi, stack, filteredParcels);
    }

    public void removeUnusedParcelVersions(ApiClient apiClient, ParcelsResourceApi parcelsResourceApi, ParcelResourceApi parcelResourceApi, Stack stack,
            ClouderaManagerProduct product) throws ApiException {
        String parcelName = product.getName();
        String parcelVersion = product.getVersion();
        Map<String, String> unusedDistributedParcelVersions =
                getClouderaManagerParcelsByStatus(parcelsResourceApi, stack.getName(), ParcelStatus.DISTRIBUTED).stream()
                        .filter(apiParcel -> apiParcel.getProduct().equals(parcelName) && !apiParcel.getVersion().equals(parcelVersion))
                        .collect(toMap(ApiParcel::getProduct, ApiParcel::getVersion));

        undistributeParcels(apiClient, parcelResourceApi, stack, unusedDistributedParcelVersions);

        Map<String, String> unusedDownloadedParcelVersions =
                getClouderaManagerParcelsByStatus(parcelsResourceApi, stack.getName(), ParcelStatus.DOWNLOADED).stream()
                        .filter(apiParcel -> apiParcel.getProduct().equals(parcelName) && !apiParcel.getVersion().equals(parcelVersion))
                        .collect(toMap(ApiParcel::getProduct, ApiParcel::getVersion));
        removeParcels(apiClient, parcelResourceApi, stack, unusedDownloadedParcelVersions);
    }

    private ParcelOperationStatus deactivateParcels(ParcelResourceApi parcelResourceApi, String stackName, Map<String, String> installedParcels) {
        ParcelOperationStatus deactivateStatus = new ParcelOperationStatus();
        for (Entry<String, String> installedParcel : installedParcels.entrySet()) {
            Parcel parcel = new Parcel(installedParcel.getKey(), installedParcel.getValue());
            try {
                LOGGER.debug("Deactivating {} parcel", parcel);
                parcelResourceApi.deactivateCommand(stackName, parcel.getName(), parcel.getVersion());
                deactivateStatus.addSuccesful(parcel.getName(), parcel.getVersion());
                LOGGER.info("Successfully deactivated parcel: {}", parcel);
            } catch (ApiException e) {
                LOGGER.info(String.format("Unable to deactivate parcel: %s", parcel), e);
                deactivateStatus.addFailed(parcel.getName(), parcel.getVersion());
            }
        }
        return deactivateStatus;
    }

    private ParcelOperationStatus undistributeParcels(ApiClient apiClient, ParcelResourceApi parcelResourceApi, Stack stack, Map<String, String> parcels) {
        ParcelOperationStatus undistributeStatus = new ParcelOperationStatus();
        for (Entry<String, String> distributedParcel : parcels.entrySet()) {
            Parcel parcel = new Parcel(distributedParcel.getKey(), distributedParcel.getValue());
            try {
                LOGGER.debug("Undistributing {} parcel", parcel);
                parcelResourceApi.startRemovalOfDistributionCommand(stack.getName(), parcel.getName(), parcel.getVersion());
                undistributeStatus.addSuccesful(parcel.getName(), parcel.getVersion());
                LOGGER.info("Successfully undistributed parcel: {}", parcel);
            } catch (ApiException e) {
                LOGGER.info(String.format("Unable to undistribute parcel: %s", parcel), e);
                undistributeStatus.addFailed(parcel.getName(), parcel.getVersion());
            }
        }
        Map<String, String> pollableParcels = parcels.entrySet().stream()
                .filter(filterParcels(undistributeStatus.getSuccessful().keySet()))
                .collect(toMap(Entry::getKey, Entry::getValue));
        PollingResult pollingResult = clouderaManagerPollingServiceProvider.startPollingCmParcelStatus(stack, apiClient, pollableParcels,
                ParcelStatus.DOWNLOADED);
        if (isExited(pollingResult)) {
            throw new CancellationException("Cluster was terminated while waiting for parcels undistribution");
        } else if (isTimeout(pollingResult)) {
            throw new ClouderaManagerOperationFailedException("Timeout while Cloudera Manager undistribute parcels.");
        }
        return undistributeStatus;
    }

    private ParcelOperationStatus removeParcels(ApiClient apiClient, ParcelResourceApi parcelResourceApi, Stack stack, Map<String, String> parcels) {
        ParcelOperationStatus removalStatus = new ParcelOperationStatus();
        for (Entry<String, String> downloadedParcel : parcels.entrySet()) {
            Parcel parcel = new Parcel(downloadedParcel.getKey(), downloadedParcel.getValue());
            try {
                LOGGER.debug("Removing {} parcel", parcel);
                parcelResourceApi.removeDownloadCommand(stack.getName(), parcel.getName(), parcel.getVersion());
                removalStatus.addSuccesful(parcel.getName(), parcel.getVersion());
                LOGGER.info("Successfully removed parcel: {}", parcel);
            } catch (ApiException e) {
                LOGGER.info(String.format("Unable to delete parcel: %s", parcel), e);
                removalStatus.addFailed(parcel.getName(), parcel.getVersion());
            }
        }
        Map<String, String> pollableParcels = parcels.entrySet().stream()
                .filter(filterParcels(removalStatus.getSuccessful().keySet()))
                .collect(toMap(Entry::getKey, Entry::getValue));
        PollingResult pollingResult = clouderaManagerPollingServiceProvider.startPollingCmParcelDelete(stack, apiClient, pollableParcels);
        if (isExited(pollingResult)) {
            throw new CancellationException("Cluster was terminated while waiting for parcels deletion");
        } else if (isTimeout(pollingResult)) {
            throw new ClouderaManagerOperationFailedException("Timeout while Cloudera Manager deletes parcels.");
        }
        return removalStatus;
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

    private Predicate<Entry<String, String>> filterParcels(Set<String> parcels) {
        return parcel -> parcels.contains(parcel.getKey());
    }

    private static class Parcel {
        private final String name;

        private final String version;

        private Parcel(String name, String version) {
            this.name = name;
            this.version = version;
        }

        public String getName() {
            return name;
        }

        public String getVersion() {
            return version;
        }

        @Override
        public String toString() {
            return '[' + name + ':' + version + ']';
        }
    }
}
