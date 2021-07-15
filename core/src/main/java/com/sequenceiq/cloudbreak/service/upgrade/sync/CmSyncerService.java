package com.sequenceiq.cloudbreak.service.upgrade.sync;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.domain.stack.Component;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.image.StatedImage;
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

    /**
     * Will retrieve
     * - all the active parcels, CDH and prewarms as well
     * - CM version
     * from the CM server and will try to find matching products in stated images.
     * Then the found products will then be used to update the Component and ClusterComponent table in DB
     *
     * @param stack           The stack that needs to be synced
     * @param candidateImages Set of candidate images whose products are used to match against the parcel version received from CM server.
     */
    public void syncFromCmToDb(Stack stack, Set<StatedImage> candidateImages) {
        Set<Component> syncedFromServer = new HashSet<>();
        cmInstalledComponentFinderService.findCmRepoComponent(stack, candidateImages, syncedFromServer);
        cmInstalledComponentFinderService.findParcelComponents(stack, candidateImages, syncedFromServer);
        LOGGER.debug("Active components read from CM server and persisting now to the DB: {}", syncedFromServer);
        stackComponentUpdater.updateComponentsByStackId(stack, syncedFromServer, false);
        clusterComponentUpdater.updateClusterComponentsByStackId(stack, syncedFromServer, false);
    }

}
