package com.sequenceiq.cloudbreak.service.upgrade.sync.operationresult;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.service.upgrade.sync.ParcelInfo;

@Service
public class CmSyncOperationResultEvaluatorService {

    public void evaluateParcelSync(Optional<CmParcelSyncOperationResult> cmParcelSyncOperationResultOptional,
            CmSyncOperationSummary.Builder cmSyncOperationSummaryBuilder) {
        if (cmParcelSyncOperationResultOptional.isEmpty() || cmParcelSyncOperationResultOptional.get().getInstalledParcels().isEmpty()) {
            cmSyncOperationSummaryBuilder.withError("CM parcel sync failed, it was not possible to retrieve installed parcel versions from the CM server.");
        } else {
            CmParcelSyncOperationResult cmParcelSyncOperationResult = cmParcelSyncOperationResultOptional.get();
            Set<String> installedParcelNames = cmParcelSyncOperationResult.getInstalledParcels().stream().map(ParcelInfo::getName).collect(Collectors.toSet());
            Set<String> foundCmProductNames = cmParcelSyncOperationResult.getFoundCmProducts().stream().map(cmp -> cmp.getName()).collect(Collectors.toSet());
            Set<String> notFoundProductNames = Sets.difference(installedParcelNames, foundCmProductNames);
            if (notFoundProductNames.isEmpty()) {
                String message = String.format("Successfully synced following parcel versions from CM server: %s", installedParcelNames);
                cmSyncOperationSummaryBuilder.withSuccess(message);
            } else {
                String message;
                if (notFoundProductNames.size() == installedParcelNames.size()) {
                    message = String.format("The version of parcels could not be synced from CM server: %s.", notFoundProductNames);
                } else {
                    message = String.format("The version of some parcels could not be synced from CM server: %s. Parcel versions successfully synced: %s ",
                            notFoundProductNames, foundCmProductNames);
                }
                cmSyncOperationSummaryBuilder.withError(message);
            }
        }
    }

    public void evaluateCmRepoSync(Optional<CmRepoSyncOperationResult> cmRepoSyncOperationResultOptional,
            CmSyncOperationSummary.Builder cmSyncOperationSummaryBuilder) {
        if (cmRepoSyncOperationResultOptional.isEmpty() || cmRepoSyncOperationResultOptional.get().getInstalledCmVersion().isEmpty()) {
            cmSyncOperationSummaryBuilder.withError("CM repository sync failed, it was not possible to retrieve CM version from the server.");
        } else {
            CmRepoSyncOperationResult cmRepoSyncOperationResult = cmRepoSyncOperationResultOptional.get();
            String cmInstalledVersion = cmRepoSyncOperationResult.getInstalledCmVersion().get();
            if (cmRepoSyncOperationResult.getFoundClouderaManagerRepo().isEmpty()) {
                String message = String.format("CM repository sync failed, no matching component found for CM server version %s.", cmInstalledVersion);
                cmSyncOperationSummaryBuilder.withError(message);
            } else {
                String message = String.format("CM repository sync succeeded, CM server version is %s.", cmInstalledVersion);
                cmSyncOperationSummaryBuilder.withSuccess(message);
            }
        }
    }

}
