package com.sequenceiq.cloudbreak.cm;

import static com.sequenceiq.cloudbreak.polling.PollingResult.isExited;
import static com.sequenceiq.cloudbreak.polling.PollingResult.isTimeout;
import static java.util.stream.Collectors.toMap;

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

    @Inject
    private ClouderaManagerParcelManagementService parcelManagementService;

    public Map<String, String> getParcelsInStatus(ParcelsResourceApi parcelsResourceApi, String stackName, ParcelStatus parcelStatus) {
        try {
            Map<String, String> parcelResponse = parcelManagementService.getClouderaManagerParcelsByStatus(parcelsResourceApi, stackName, parcelStatus).stream()
                    .collect(toMap(ApiParcel::getProduct, ApiParcel::getVersion));
            LOGGER.debug("The following parcels are found in {} status: {}", parcelStatus, parcelResponse);
            return parcelResponse;
        } catch (ApiException e) {
            LOGGER.info("Unable to fetch the list of activated parcels", e);
            throw new ClouderaManagerOperationFailedException("Unable to fetch the list of activated parcels", e);
        }
    }

    public ParcelOperationStatus deactivateUnusedParcels(ParcelsResourceApi parcelsResourceApi, ParcelResourceApi parcelResourceApi, String stackName,
            Set<String> usedParcelComponentNames, Set<String> parcelNamesFromImage) {
        Map<String, String> activeParcels = getParcelsInStatus(parcelsResourceApi, stackName, ParcelStatus.ACTIVATED);
        Map<String, String> parcelsToDeactivate = getUnusedParcels(activeParcels, usedParcelComponentNames, parcelNamesFromImage);
        LOGGER.debug("The following parcels will be deactivated: {}", parcelsToDeactivate);
        return deactivateParcels(parcelResourceApi, stackName, parcelsToDeactivate);
    }

    public ParcelOperationStatus undistributeUnusedParcels(ApiClient apiClient, ParcelsResourceApi parcelsResourceApi, ParcelResourceApi parcelResourceApi,
            Stack stack, Set<String> usedParcelComponentNames, Set<String> parcelNamesFromImage) {
        Map<String, String> distributedParcels = getParcelsInStatus(parcelsResourceApi, stack.getName(), ParcelStatus.DISTRIBUTED);
        Map<String, String> parcelsToUndistribute = getUnusedParcels(distributedParcels, usedParcelComponentNames, parcelNamesFromImage);
        LOGGER.debug("The following parcels will be undistributed: {}", parcelsToUndistribute);
        return undistributeParcels(apiClient, parcelResourceApi, stack, parcelsToUndistribute);
    }

    public ParcelOperationStatus removeUnusedParcels(ApiClient apiClient, ParcelsResourceApi parcelsResourceApi, ParcelResourceApi parcelResourceApi,
            Stack stack, Set<String> usedParcelComponentNames, Set<String> parcelNamesFromImage) {
        Map<String, String> downloadedParcels = getParcelsInStatus(parcelsResourceApi, stack.getName(), ParcelStatus.DOWNLOADED);
        Map<String, String> parcelsToRemove = getUnusedParcels(downloadedParcels, usedParcelComponentNames, parcelNamesFromImage);
        LOGGER.debug("The following parcels will be removed: {}", parcelsToRemove);
        return removeParcels(apiClient, parcelResourceApi, stack, parcelsToRemove);
    }

    public void removeUnusedParcelVersions(ApiClient apiClient, ParcelsResourceApi parcelsResourceApi, ParcelResourceApi parcelResourceApi, Stack stack,
            ClouderaManagerProduct product) throws ApiException {
        undistributeUnusedDistributedParcels(apiClient, parcelsResourceApi, parcelResourceApi, stack, product);
        removeDownloadedUnusedParcels(apiClient, parcelsResourceApi, parcelResourceApi, stack, product);
    }

    private void removeDownloadedUnusedParcels(ApiClient apiClient, ParcelsResourceApi parcelsResourceApi, ParcelResourceApi parcelResourceApi, Stack stack,
            ClouderaManagerProduct product) throws ApiException {
        Map<String, String> unusedDownloadedParcelVersions =
                getParcelVersionsByStatusNameAndVersion(parcelsResourceApi, stack, product, ParcelStatus.DOWNLOADED);
        removeParcels(apiClient, parcelResourceApi, stack, unusedDownloadedParcelVersions);
    }

    private void undistributeUnusedDistributedParcels(ApiClient apiClient, ParcelsResourceApi parcelsResourceApi, ParcelResourceApi parcelResourceApi,
            Stack stack, ClouderaManagerProduct product) throws ApiException {
        Map<String, String> unusedDistributedParcelVersions =
                getParcelVersionsByStatusNameAndVersion(parcelsResourceApi, stack, product, ParcelStatus.DISTRIBUTED);
        undistributeParcels(apiClient, parcelResourceApi, stack, unusedDistributedParcelVersions);
    }

    private Map<String, String> getParcelVersionsByStatusNameAndVersion(ParcelsResourceApi parcelsResourceApi, Stack stack, ClouderaManagerProduct product,
            ParcelStatus parcelStatus) throws ApiException {
        return parcelManagementService.getClouderaManagerParcelsByStatus(parcelsResourceApi, stack.getName(), parcelStatus)
                .stream()
                .filter(apiParcel -> apiParcel.getProduct().equals(product.getName()) && !apiParcel.getVersion().equals(product.getVersion()))
                .collect(toMap(ApiParcel::getProduct, ApiParcel::getVersion));
    }

    private Map<String, String> getUnusedParcels(Map<String, String> activeParcels, Set<String> usedParcelComponentNames, Set<String> parcelsFromImage) {
        return activeParcels.entrySet().stream()
                .filter(entry -> !usedParcelComponentNames.contains(entry.getKey()) && parcelsFromImage.contains(entry.getKey()))
                .collect(toMap(Entry::getKey, Entry::getValue));
    }

    private ParcelOperationStatus deactivateParcels(ParcelResourceApi parcelResourceApi, String stackName, Map<String, String> activeParcels) {
        ParcelOperationStatus deactivateStatus = new ParcelOperationStatus();
        for (Entry<String, String> activeParcel : activeParcels.entrySet()) {
            Parcel parcel = new Parcel(activeParcel.getKey(), activeParcel.getValue());
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
        ParcelOperationStatus undistributeStatus = parcels.entrySet().stream()
                .map(parcel -> runSingleParcelCommand("UNDISTRIBUTE", stack, parcel, parcelResourceApi::startRemovalOfDistributionCommand))
                .reduce(new ParcelOperationStatus(), ParcelOperationStatus::merge);

        Map<String, String> pollableParcels = filterForPollableParcels(parcels, undistributeStatus);
        PollingResult pollingResult = clouderaManagerPollingServiceProvider.startPollingCmParcelStatus(stack, apiClient, pollableParcels,
                ParcelStatus.DOWNLOADED);
        if (isExited(pollingResult)) {
            throw new CancellationException("Cluster was terminated while waiting for parcels undistribution");
        } else if (isTimeout(pollingResult)) {
            throw new ClouderaManagerOperationFailedException("Timeout while Cloudera Manager undistribute parcels.");
        }
        return undistributeStatus;
    }

    private Map<String, String> filterForPollableParcels(Map<String, String> parcels, ParcelOperationStatus parcelOperationStatus) {
        return parcels.entrySet().stream()
                .filter(filterParcels(parcelOperationStatus.getSuccessful().keySet()))
                .collect(toMap(Entry::getKey, Entry::getValue));
    }

    private ParcelOperationStatus runSingleParcelCommand(String commandName, Stack stack, Entry<String, String> distributedParcel,
            ParcelCommand parcelCommand) {
        Parcel parcel = new Parcel(distributedParcel.getKey(), distributedParcel.getValue());
        try {
            LOGGER.debug("[{}] Start for {} parcel", commandName, parcel);
            parcelCommand.apply(stack.getName(), parcel.getName(), parcel.getVersion());
            LOGGER.info("[{}] Successful for parcel: {}", commandName, parcel);
            return new ParcelOperationStatus().withSuccesful(parcel.getName(), parcel.getVersion());
        } catch (ApiException e) {
            LOGGER.info(String.format("[%s] Failed for parcel: %s", commandName, parcel), e);
            return new ParcelOperationStatus().withFailed(parcel.getName(), parcel.getVersion());
        }
    }

    private ParcelOperationStatus removeParcels(ApiClient apiClient, ParcelResourceApi parcelResourceApi, Stack stack, Map<String, String> parcels) {
        ParcelOperationStatus removalStatus = parcels.entrySet().stream()
                .map(parcel -> runSingleParcelCommand("REMOVE", stack, parcel, parcelResourceApi::removeDownloadCommand))
                .reduce(new ParcelOperationStatus(), ParcelOperationStatus::merge);
        Map<String, String> pollableParcels = filterForPollableParcels(parcels, removalStatus);
        PollingResult pollingResult = clouderaManagerPollingServiceProvider.startPollingCmParcelDelete(stack, apiClient, pollableParcels);
        if (isExited(pollingResult)) {
            throw new CancellationException("Cluster was terminated while waiting for parcels deletion");
        } else if (isTimeout(pollingResult)) {
            throw new ClouderaManagerOperationFailedException("Timeout while Cloudera Manager deletes parcels.");
        }
        return removalStatus;
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
