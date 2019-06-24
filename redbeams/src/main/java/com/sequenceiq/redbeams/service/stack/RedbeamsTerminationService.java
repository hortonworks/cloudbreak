package com.sequenceiq.redbeams.service.stack;

import com.sequenceiq.redbeams.domain.DatabaseServerConfig;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.flow.RedbeamsFlowManager;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsEvent;
import com.sequenceiq.redbeams.flow.redbeams.termination.RedbeamsTerminationEvent;
// import com.sequenceiq.redbeams.service.crn.CrnService;

import java.security.SecureRandom;
import java.util.Random;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class RedbeamsTerminationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedbeamsTerminationService.class);

    @Inject
    private DBStackService dbStackService;

    @Inject
    private RedbeamsFlowManager flowManager;

    // @Inject
    // private CrnService crnService;

    private final Random random = new SecureRandom();

    public DatabaseServerConfig terminateDatabaseServer(String dbStackName, String environmentId) {
        // FIXME log the stack?

        DBStack dbStack = dbStackService.getByNameAndEnvironmentId(dbStackName, environmentId);

        // Replace resourceId with something non-random
        flowManager.notify(RedbeamsTerminationEvent.REDBEAMS_TERMINATION_EVENT.selector(),
                new RedbeamsEvent(RedbeamsTerminationEvent.REDBEAMS_TERMINATION_EVENT.selector(), dbStack.getId()));
        return new DatabaseServerConfig();
    }
}
