package com.sequenceiq.cloudbreak.service.upgrade.sync.db;

import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.domain.stack.Component;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterComponentUpdater;
import com.sequenceiq.cloudbreak.service.upgrade.StackComponentUpdater;
import com.sequenceiq.cloudbreak.service.upgrade.sync.operationresult.CmSyncOperationResult;

@Service
public class ComponentPersistingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ComponentPersistingService.class);

    @Inject
    private CmSyncResultMergerService cmSyncResultMergerService;

    @Inject
    private StackComponentUpdater stackComponentUpdater;

    @Inject
    private ClusterComponentUpdater clusterComponentUpdater;

    public void persistComponentsToDb(Stack stack, CmSyncOperationResult cmSyncOperationResult) {
        Set<Component> syncedFromServer = cmSyncResultMergerService.merge(stack, cmSyncOperationResult);
        LOGGER.debug("Active components read from CM server and persisting now to the DB: {}", syncedFromServer);
        stackComponentUpdater.updateComponentsByStackId(stack, syncedFromServer, false);
        clusterComponentUpdater.updateClusterComponentsByStackId(stack, syncedFromServer, false);
    }

}
