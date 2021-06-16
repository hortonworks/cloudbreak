package com.sequenceiq.cloudbreak.service.upgrade.sync;

import java.util.HashSet;
import java.util.Optional;
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

    /**
     * Will retrieve (if CM server is reachable):
     * - all the active parcels, CDH and prewarm parcels as well
     * - CM version
     * from the CM server and will try to find matching products in stated images.
     * The found products will then be used to update the Component and ClusterComponent table in DB
     *
     * @param stack           The stack that needs to be synced
     * @param candidateImages Set of candidate images whose products are used to match against the parcel version received from CM server.
     */
    public void syncFromCmToDb(Stack stack, Set<Image> candidateImages) {
        if (!cmServerQueryService.isCmServerRunning(stack)) {
            LOGGER.info("CM server is down, it is not possible to sync parcels and CM version to DB.");
            return;
        }
        if (candidateImages.isEmpty()) {
            LOGGER.info("No candidate images supplied, skipping syncing CM parcels and CM version to DB.");
            return;
        }
        syncInternal(stack, candidateImages);
    }

    private void syncInternal(Stack stack, Set<Image> candidateImages) {
        Optional<Component> cmRepoComponents = cmInstalledComponentFinderService.findCmRepoComponent(stack, candidateImages);
        Set<Component> parcelComponents = cmInstalledComponentFinderService.findParcelComponents(stack, candidateImages);
        Set<Component> syncedFromServer = mergeFoundComponents(cmRepoComponents, parcelComponents);
        LOGGER.debug("Active components read from CM server and persisting now to the DB: {}", syncedFromServer);
        stackComponentUpdater.updateComponentsByStackId(stack, syncedFromServer, false);
        clusterComponentUpdater.updateClusterComponentsByStackId(stack, syncedFromServer, false);
    }

    private Set<Component> mergeFoundComponents(Optional<Component> cmRepoComponents, Set<Component> parcelComponents) {
        Set<Component> syncedFromServer = new HashSet<>();
        cmRepoComponents.ifPresent(syncedFromServer::add);
        syncedFromServer.addAll(parcelComponents);
        return syncedFromServer;
    }

}
