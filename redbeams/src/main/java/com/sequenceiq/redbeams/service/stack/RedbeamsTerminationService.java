package com.sequenceiq.redbeams.service.stack;

import java.security.SecureRandom;
import java.util.Random;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.redbeams.api.model.common.DetailedDBStackStatus;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.flow.RedbeamsFlowManager;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsEvent;
import com.sequenceiq.redbeams.flow.redbeams.termination.RedbeamsTerminationEvent;

@Service
public class RedbeamsTerminationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedbeamsTerminationService.class);

    @Inject
    private DBStackService dbStackService;

    @Inject
    private DBStackStatusUpdater dbStackStatusUpdater;

    @Inject
    private RedbeamsFlowManager flowManager;

    private final Random random = new SecureRandom();

    public DBStack terminateDatabaseServer(String crn, boolean force) {
        DBStack dbStack = dbStackService.getByCrn(crn);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Terminate called for: {} with force: {}", dbStack, force);
        }
        if (dbStack.getStatus().isDeleteInProgressOrCompleted()) {
            LOGGER.debug("DatabaseServer with crn {} is already being deleted", dbStack.getResourceCrn());
            return dbStack;
        }

        dbStack = dbStackStatusUpdater.updateStatus(dbStack.getId(), DetailedDBStackStatus.DELETE_REQUESTED);
        flowManager.cancelRunningFlows(dbStack.getId());
        flowManager.notify(RedbeamsTerminationEvent.REDBEAMS_TERMINATION_EVENT.selector(),
                new RedbeamsEvent(RedbeamsTerminationEvent.REDBEAMS_TERMINATION_EVENT.selector(), dbStack.getId(), force));
        return dbStack;
    }
}
