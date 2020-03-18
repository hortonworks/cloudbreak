package com.sequenceiq.redbeams.flow.ha;

import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.service.ha.HaApplication;
import com.sequenceiq.flow.core.FlowRegister;
import com.sequenceiq.flow.service.FlowCancelService;
import com.sequenceiq.redbeams.service.stack.DBStackService;
import com.sequenceiq.redbeams.service.store.RedbeamsInMemoryStateStoreService;

@Primary
@Component
public class RedbeamsHaApplication implements HaApplication {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedbeamsHaApplication.class);

    @Inject
    private DBStackService dbStackService;

    @Inject
    private RedbeamsInMemoryStateStoreService redbeamsInMemoryStateStoreService;

    @Inject
    private FlowRegister runningFlows;

    @Inject
    private FlowCancelService flowCancelService;

    @Override
    public Set<Long> getDeletingResources(Set<Long> resourceIds) {
        return dbStackService.findAllDeleting();
    }

    @Override
    public Set<Long> getAllDeletingResources() {
        Set<Long> allResourcesOfThisNode = redbeamsInMemoryStateStoreService.getAll();
        return allResourcesOfThisNode.isEmpty() ? Set.of() : dbStackService.findAllDeletingById(allResourcesOfThisNode);
    }

    @Override
    public void cleanupInMemoryStore(Long resourceId) {
        redbeamsInMemoryStateStoreService.delete(resourceId);
    }

    @Override
    public void cancelRunningFlow(Long resourceId) {
        dbStackService.findById(resourceId).ifPresentOrElse(
                db -> {
                    flowCancelService.cancelRunningFlows(resourceId);
                    redbeamsInMemoryStateStoreService.registerCancel(resourceId);
                },
                () -> LOGGER.error("Cannot cancel the flow, because the database stack with this id does not exist: {}", resourceId));
    }

    @Override
    public boolean isRunningOnThisNode(Set<String> runningFlowIds) {
        return runningFlowIds.stream().anyMatch(id -> runningFlows.get(id) != null);
    }
}
