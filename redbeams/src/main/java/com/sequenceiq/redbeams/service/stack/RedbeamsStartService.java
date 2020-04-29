package com.sequenceiq.redbeams.service.stack;

import com.sequenceiq.redbeams.api.model.common.DetailedDBStackStatus;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.flow.RedbeamsFlowManager;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsEvent;
import com.sequenceiq.redbeams.flow.redbeams.start.RedbeamsStartEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;

@Service
public class RedbeamsStartService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedbeamsStartService.class);

    @Inject
    private DBStackService dbStackService;

    @Inject
    private DBStackStatusUpdater dbStackStatusUpdater;

    @Inject
    private RedbeamsFlowManager flowManager;

    public void startDatabaseServer(String crn) {
        DBStack dbStack = dbStackService.getByCrn(crn);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Start called for: {}", dbStack);
        }
        if (dbStack.getStatus().isStartInProgressOrCompleted()) {
            LOGGER.debug("DatabaseServer with crn {} is already being started", dbStack.getResourceCrn());
            return;
        }

        dbStack = dbStackStatusUpdater.updateStatus(dbStack.getId(), DetailedDBStackStatus.START_REQUESTED);

        flowManager.notify(RedbeamsStartEvent.REDBEAMS_START_EVENT.selector(),
                new RedbeamsEvent(RedbeamsStartEvent.REDBEAMS_START_EVENT.selector(), dbStack.getId()));
    }
}
