package com.sequenceiq.cloudbreak.service.upgrade.sync;

import java.util.Set;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.upgrade.sync.component.CmInstalledComponentFinderService;
import com.sequenceiq.cloudbreak.service.upgrade.sync.component.CmServerQueryService;
import com.sequenceiq.cloudbreak.service.upgrade.sync.db.ComponentPersistingService;
import com.sequenceiq.cloudbreak.service.upgrade.sync.operationresult.CmSyncOperationResult;
import com.sequenceiq.cloudbreak.service.upgrade.sync.operationresult.CmSyncOperationStatus;
import com.sequenceiq.cloudbreak.service.upgrade.sync.operationresult.CmSyncOperationSummary;
import com.sequenceiq.cloudbreak.service.upgrade.sync.operationresult.CmSyncOperationSummaryService;

@Service
public class CmSyncerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CmSyncerService.class);

    @Inject
    private CmInstalledComponentFinderService cmInstalledComponentFinderService;

    @Inject
    private CmServerQueryService cmServerQueryService;

    @Inject
    private CmSyncOperationSummaryService cmSyncOperationSummaryService;

    @Inject
    private ComponentPersistingService componentPersistingService;

    @Inject
    private MixedPackageVersionService mixedPackageVersionService;

    /**
     * Will retrieve (if CM server is reachable):
     * - all the active parcels, CDH and prewarmed parcels as well
     * - CM version
     * from the CM server and will try to find matching products in stated images.
     * The found products will then be used to update the Component and ClusterComponent table in DB
     * @param stack           The stack that needs to be synced
     * @param candidateImages Set of candidate images whose products are used to match against the parcel version received from CM server.
     * @return the summary of the sync operation, with status, parcel and package information
     */
    public CmSyncOperationSummary syncFromCmToDb(Stack stack, Set<Image> candidateImages) {
        if (cmServerQueryService.isCmServerNotRunning(stack)) {
            return buildSummaryWithErrorMessage("CM server is down, it is not possible to sync parcels and CM version from the server.");
        }
        if (candidateImages.isEmpty()) {
            return buildSummaryWithErrorMessage("No candidate images supplied for CM sync, it is not possible to sync parcels and CM version from the server. "
                    + "Please open Cloudera support ticket to investigate the issue");
        }
        return syncInternal(stack, candidateImages);
    }

    private CmSyncOperationSummary buildSummaryWithErrorMessage(String message) {
        LOGGER.warn(message);
        return new CmSyncOperationSummary(CmSyncOperationStatus.builder().withError(message).build());
    }

    private CmSyncOperationSummary syncInternal(Stack stack, Set<Image> candidateImages) {
        CmSyncOperationResult cmSyncOperationResult = new CmSyncOperationResult(
                cmInstalledComponentFinderService.findCmRepoComponent(stack, candidateImages),
                cmInstalledComponentFinderService.findParcelComponents(stack, candidateImages)
        );
        LOGGER.debug("Synced CM versions and found components: {}", cmSyncOperationResult);
        componentPersistingService.persistComponentsToDb(
                stack,
                cmSyncOperationResult
        );
        mixedPackageVersionService.validatePackageVersions(
                stack.getWorkspace().getId(),
                stack.getId(),
                cmSyncOperationResult,
                candidateImages
        );
        CmSyncOperationStatus cmSyncOperationStatus = cmSyncOperationSummaryService.evaluate(cmSyncOperationResult);
        LOGGER.info("CM sync was executed, summary: {}", cmSyncOperationStatus);
        return new CmSyncOperationSummary(cmSyncOperationStatus, cmSyncOperationResult);
    }

}
