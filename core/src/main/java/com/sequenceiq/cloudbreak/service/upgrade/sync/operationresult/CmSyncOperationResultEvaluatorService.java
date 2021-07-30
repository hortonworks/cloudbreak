package com.sequenceiq.cloudbreak.service.upgrade.sync.operationresult;

import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.service.upgrade.sync.common.ParcelInfo;

@Service
public class CmSyncOperationResultEvaluatorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CmSyncOperationResultEvaluatorService.class);

    public CmSyncOperationSummary.Builder evaluateParcelSync(CmParcelSyncOperationResult cmParcelSyncOperationResult) {
        if (cmParcelSyncOperationResult.getInstalledParcels().isEmpty()) {
            String message = "CM parcel sync failed, it was not possible to retrieve installed parcel versions from the CM server.";
            LOGGER.debug(message);
            return CmSyncOperationSummary.builder().withError(message);
        } else {
            return evaluateIfAllParcelsWereFound(cmParcelSyncOperationResult);
        }
    }

    public CmSyncOperationSummary.Builder evaluateCmRepoSync(CmRepoSyncOperationResult cmRepoSyncOperationResult) {
        if (cmRepoSyncOperationResult.getInstalledCmVersion().isEmpty()) {
            String message = "CM repository sync failed, it was not possible to retrieve CM version from the server.";
            LOGGER.debug(message);
            return CmSyncOperationSummary.builder().withError(message);
        } else {
            return evaluateIfCmRepoWasFound(cmRepoSyncOperationResult);
        }
    }

    private CmSyncOperationSummary.Builder evaluateIfAllParcelsWereFound(CmParcelSyncOperationResult cmParcelSyncOperationResult) {
        Set<String> installedParcelNames = cmParcelSyncOperationResult.getInstalledParcels().stream().map(ParcelInfo::getName).collect(Collectors.toSet());
        Set<String> foundCmProductNames = cmParcelSyncOperationResult.getFoundCmProducts().stream().map(cmp -> cmp.getName()).collect(Collectors.toSet());
        Set<String> notFoundProductNames = Sets.difference(installedParcelNames, foundCmProductNames);
        if (notFoundProductNames.isEmpty()) {
            String message = String.format("Successfully synced following parcel versions from CM server: %s", installedParcelNames);
            LOGGER.debug(message);
            return CmSyncOperationSummary.builder().withSuccess(message);
        } else {
            String message = getParcelErrorMessage(installedParcelNames, foundCmProductNames, notFoundProductNames);
            LOGGER.debug(message);
            return CmSyncOperationSummary.builder().withError(message);
        }
    }

    private CmSyncOperationSummary.Builder evaluateIfCmRepoWasFound(CmRepoSyncOperationResult cmRepoSyncOperationResult) {
        String cmInstalledVersion = cmRepoSyncOperationResult.getInstalledCmVersion().get();
        if (cmRepoSyncOperationResult.getFoundClouderaManagerRepo().isEmpty()) {
            String message = String.format("CM repository sync failed, no matching component found for CM server version %s.", cmInstalledVersion);
            LOGGER.debug(message);
            return CmSyncOperationSummary.builder().withError(message);
        } else {
            String message = String.format("CM repository sync succeeded, CM server version is %s.", cmInstalledVersion);
            LOGGER.debug(message);
            return CmSyncOperationSummary.builder().withSuccess(message);
        }
    }

    private String getParcelErrorMessage(Set<String> installedParcelNames, Set<String> foundCmProductNames, Set<String> notFoundProductNames) {
        return notFoundProductNames.size() == installedParcelNames.size()
                ? String.format("The version of parcels could not be synced from CM server: %s.", notFoundProductNames)
                : String.format("The version of some parcels could not be synced from CM server: %s. Parcel versions successfully synced: %s ",
                notFoundProductNames, foundCmProductNames);
    }

}