package com.sequenceiq.redbeams.service.stack;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.flow.RedbeamsFlowManager;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsEvent;
import com.sequenceiq.redbeams.flow.redbeams.stop.RedbeamsStopEvent;

@Service
public class RedbeamsStopService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedbeamsStopService.class);

    @Inject
    private DBStackService dbStackService;

    @Inject
    private RedbeamsFlowManager flowManager;

    public void stopDatabaseServer(String crn) {
        DBStack dbStack = dbStackService.getByCrn(crn);
        MDCBuilder.buildMdcContext(dbStack);
        LOGGER.debug("Stop called for: {}", dbStack);

        flowManager.notify(RedbeamsStopEvent.REDBEAMS_STOP_EVENT.selector(),
                new RedbeamsEvent(RedbeamsStopEvent.REDBEAMS_STOP_EVENT.selector(), dbStack.getId()));
    }
}
