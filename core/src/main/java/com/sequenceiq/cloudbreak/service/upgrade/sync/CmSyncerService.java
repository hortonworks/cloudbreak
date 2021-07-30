package com.sequenceiq.cloudbreak.service.upgrade.sync;

import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.domain.stack.Component;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterComponentUpdater;
import com.sequenceiq.cloudbreak.service.upgrade.StackComponentUpdater;
import com.sequenceiq.cloudbreak.service.upgrade.sync.operationresult.CmParcelSyncOperationResult;
import com.sequenceiq.cloudbreak.service.upgrade.sync.operationresult.CmRepoSyncOperationResult;
import com.sequenceiq.cloudbreak.service.upgrade.sync.operationresult.CmSyncOperationResult;
import com.sequenceiq.cloudbreak.service.upgrade.sync.operationresult.CmSyncOperationSummary;
import com.sequenceiq.cloudbreak.service.upgrade.sync.operationresult.CmSyncOperationSummaryService;

@Service
public class CmSyncerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CmSyncerService.class);

    @Inject
    private StackComponentUpdater stackComponentUpdater;

    @Inject
    private ClusterComponentUpdater clusterComponentUpdater;

    @Inject
    private CmInstalledComponentFinderService cmInstalledComponentFinderService;

    @Inject
    private CmServerQueryService cmServerQueryService;

    @Inject
    private CmSyncOperationSummaryService cmSyncOperationSummaryService;

    @Inject
    private CmSyncResultMergerService cmSyncResultMergerService;

    /**
     * Will retrieve (if CM server is reachable):
     * - all the active parcels, CDH and prewarm parcels as well
     * - CM version
     * from the CM server and will try to find matching products in stated images.
     * The found products will then be used to update the Component and ClusterComponent table in DB
     *  @param stack           The stack that needs to be synced
     * @param candidateImages Set of candidate images whose products are used to match against the parcel version received from CM server.
     */
    public CmSyncOperationSummary syncFromCmToDb(Stack stack, Set<Image> candidateImages) {
        if (!cmServerQueryService.isCmServerRunning(stack)) {
            String message = "CM server is down, it is not possible to sync parcels and CM version from the server.";
            LOGGER.info(message);
            return CmSyncOperationSummary.builder().withError(message).build();
        }
        if (candidateImages.isEmpty()) {
            String message = "No candidate images supplied for CM sync, it is not possible to sync parcels and CM version from the server. " +
                    "Please call Cloudera support";
            LOGGER.info(message);
            return CmSyncOperationSummary.builder().withError(message).build();
        }
        return syncInternal(stack, candidateImages);
    }

    private CmSyncOperationSummary syncInternal(Stack stack, Set<Image> candidateImages) {
        CmRepoSyncOperationResult cmRepoSyncResult = cmInstalledComponentFinderService.findCmRepoComponent(stack, candidateImages);
        CmParcelSyncOperationResult cmParcelSyncResult = cmInstalledComponentFinderService.findParcelComponents(stack, candidateImages);
        Set<Component> syncedFromServer = cmSyncResultMergerService.merge(cmRepoSyncResult, cmParcelSyncResult, stack);
        LOGGER.debug("Active components read from CM server and persisting now to the DB: {}", syncedFromServer);
        stackComponentUpdater.updateComponentsByStackId(stack, syncedFromServer, false);
        clusterComponentUpdater.updateClusterComponentsByStackId(stack, syncedFromServer, false);
        CmSyncOperationSummary cmSyncOperationSummary = cmSyncOperationSummaryService.evaluate(new CmSyncOperationResult(cmRepoSyncResult, cmParcelSyncResult));
        LOGGER.info("CM sync was executed, summary: {}", cmSyncOperationSummary);
        return cmSyncOperationSummary;
    }

}
