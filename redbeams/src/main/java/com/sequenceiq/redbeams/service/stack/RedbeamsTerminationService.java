package com.sequenceiq.redbeams.service.stack;

import java.security.SecureRandom;
import java.util.List;
import java.util.Random;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.redbeams.api.model.common.DetailedDBStackStatus;
import com.sequenceiq.redbeams.api.model.common.Status;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.exception.NotFoundException;
import com.sequenceiq.redbeams.flow.RedbeamsFlowManager;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsEvent;
import com.sequenceiq.redbeams.flow.redbeams.termination.RedbeamsTerminationEvent;

@Service
public class RedbeamsTerminationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedbeamsTerminationService.class);

    private static final List<Status> TERMINATION_STATES = List.of(
            Status.DELETE_REQUESTED,
            Status.PRE_DELETE_IN_PROGRESS,
            Status.DELETE_IN_PROGRESS,
            Status.DELETE_COMPLETED
    );

    @Inject
    private DBStackService dbStackService;

    @Inject
    private DBStackStatusUpdater dbStackStatusUpdater;

    @Inject
    private RedbeamsFlowManager flowManager;

    private final Random random = new SecureRandom();

    public DBStack terminateDatabaseServer(String crn) {
        DBStack dbStack = dbStackService.getByCrn(crn);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Terminate called for: {}", dbStack);
        }
        if (TERMINATION_STATES.contains(dbStack.getStatus())) {
            LOGGER.debug("DatabaseServer with crn {} is already being deleted", dbStack.getResourceCrn());
            throw new NotFoundException(String.format("DatabaseServer with crn '%s' is already being deleted", dbStack.getResourceCrn()));
        }

        dbStack = dbStackStatusUpdater.updateStatus(dbStack.getId(), DetailedDBStackStatus.DELETE_REQUESTED);
        flowManager.cancelRunningFlows(dbStack.getId());
        flowManager.notify(RedbeamsTerminationEvent.REDBEAMS_TERMINATION_EVENT.selector(),
                new RedbeamsEvent(RedbeamsTerminationEvent.REDBEAMS_TERMINATION_EVENT.selector(), dbStack.getId()));
        return dbStack;
    }
}
