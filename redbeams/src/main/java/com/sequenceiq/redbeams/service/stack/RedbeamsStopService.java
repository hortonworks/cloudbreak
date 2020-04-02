package com.sequenceiq.redbeams.service.stack;

import com.sequenceiq.redbeams.api.model.common.DetailedDBStackStatus;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.flow.RedbeamsFlowManager;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsEvent;
import com.sequenceiq.redbeams.flow.redbeams.stop.RedbeamsStopEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;

@Service
public class RedbeamsStopService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedbeamsStopService.class);

    @Inject
    private DBStackService dbStackService;

    @Inject
    private DBStackStatusUpdater dbStackStatusUpdater;

    @Inject
    private RedbeamsFlowManager flowManager;

    public void stopDatabaseServer(String crn) {
        DBStack dbStack = dbStackService.getByCrn(crn);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Stop called for: {}", dbStack);
        }
        if (dbStack.getStatus().isStopInProgressOrCompleted()) {
            LOGGER.debug("DatabaseServer with crn {} is already being stopped", dbStack.getResourceCrn());
            return;
        }

        dbStack = dbStackStatusUpdater.updateStatus(dbStack.getId(), DetailedDBStackStatus.STOP_REQUESTED);

        flowManager.notify(RedbeamsStopEvent.REDBEAMS_STOP_EVENT.selector(),
                new RedbeamsEvent(RedbeamsStopEvent.REDBEAMS_STOP_EVENT.selector(), dbStack.getId()));
    }
}
