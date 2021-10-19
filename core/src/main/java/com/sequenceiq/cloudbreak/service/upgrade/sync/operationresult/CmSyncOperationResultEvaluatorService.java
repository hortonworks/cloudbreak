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

    private static final String READING_CM_VERSION = "Reading CM repository version";

    private static final String READING_PARCEL_VERSIONS = "Reading versions of active parcels";

    public CmSyncOperationSummary.Builder evaluateParcelSync(CmParcelSyncOperationResult cmParcelSyncOperationResult) {
        if (cmParcelSyncOperationResult.getActiveParcels().isEmpty()) {
            String message = String.format("%s failed, it was not possible to retrieve versions of active parcels from the CM server.", READING_PARCEL_VERSIONS);
            LOGGER.debug(message);
            return CmSyncOperationSummary.builder().withError(message);
        } else {
            return evaluateIfAllParcelsWereFound(cmParcelSyncOperationResult);
        }
    }

    public CmSyncOperationSummary.Builder evaluateCmRepoSync(CmRepoSyncOperationResult cmRepoSyncOperationResult) {
        if (cmRepoSyncOperationResult.getInstalledCmVersion().isEmpty()) {
            String message = String.format("%s failed, it was not possible to retrieve CM version from the server.", READING_CM_VERSION);
            LOGGER.debug(message);
            return CmSyncOperationSummary.builder().withError(message);
        } else {
            return evaluateIfCmRepoWasFound(cmRepoSyncOperationResult);
        }
    }

    private CmSyncOperationSummary.Builder evaluateIfAllParcelsWereFound(CmParcelSyncOperationResult cmParcelSyncOperationResult) {
        Set<String> activeParcelNames = cmParcelSyncOperationResult.getActiveParcels().stream().map(ParcelInfo::getName).collect(Collectors.toSet());
        Set<String> foundCmProductNames = cmParcelSyncOperationResult.getFoundCmProducts().stream().map(cmp -> cmp.getName()).collect(Collectors.toSet());
        Set<String> notFoundProductNames = Sets.difference(activeParcelNames, foundCmProductNames);
        if (notFoundProductNames.isEmpty()) {
            String message = String.format("%s succeeded, the following active parcels were found on the CM server: %s.", READING_PARCEL_VERSIONS,
                    activeParcelsToPrintableList(cmParcelSyncOperationResult));
            LOGGER.debug(message);
            return CmSyncOperationSummary.builder().withSuccess(message);
        } else {
            String message = getParcelErrorMessage(activeParcelNames, foundCmProductNames, notFoundProductNames);
            LOGGER.debug(message);
            return CmSyncOperationSummary.builder().withError(message);
        }
    }

    private String activeParcelsToPrintableList(CmParcelSyncOperationResult cmParcelSyncOperationResult) {
        return cmParcelSyncOperationResult.getActiveParcels().stream()
                .map(ap -> String.format("%s: %s", ap.getName(), ap.getVersion()))
                .collect(Collectors.joining(", "));
    }

    private CmSyncOperationSummary.Builder evaluateIfCmRepoWasFound(CmRepoSyncOperationResult cmRepoSyncOperationResult) {
        String cmInstalledVersion = cmRepoSyncOperationResult.getInstalledCmVersion().get();
        if (cmRepoSyncOperationResult.getFoundClouderaManagerRepo().isEmpty()) {
            String message = String.format("%s failed, no matching component found on images for CM server version %s.", READING_CM_VERSION, cmInstalledVersion);
            LOGGER.debug(message);
            return CmSyncOperationSummary.builder().withError(message);
        } else {
            String message = String.format("%s succeeded, the current version of CM is %s.", READING_CM_VERSION, cmInstalledVersion);
            LOGGER.debug(message);
            return CmSyncOperationSummary.builder().withSuccess(message);
        }
    }

    private String getParcelErrorMessage(Set<String> activeParcelNames, Set<String> foundCmProductNames, Set<String> notFoundProductNames) {
        return notFoundProductNames.size() == activeParcelNames.size()
                ? String.format("%s failed, the version of active parcels that could not be retrieved from CM server: %s.",
                READING_PARCEL_VERSIONS, notFoundProductNames)
                : String.format("%s failed, the version of some active parcels could not be retrieved from CM server: %s. " +
                        "Parcel versions successfully read: %s.", READING_PARCEL_VERSIONS, notFoundProductNames, foundCmProductNames);
    }

}