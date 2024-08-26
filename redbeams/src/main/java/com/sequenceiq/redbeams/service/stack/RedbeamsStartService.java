package com.sequenceiq.redbeams.service.stack;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.flow.RedbeamsFlowManager;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsEvent;
import com.sequenceiq.redbeams.flow.redbeams.start.RedbeamsStartEvent;

@Service
public class RedbeamsStartService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedbeamsStartService.class);

    @Inject
    private DBStackService dbStackService;

    @Inject
    private RedbeamsFlowManager flowManager;

    public void startDatabaseServer(String crn) {
        DBStack dbStack = dbStackService.getByCrn(crn);
        MDCBuilder.buildMdcContext(dbStack);
        LOGGER.debug("Start called for: {}", dbStack);

        flowManager.notify(RedbeamsStartEvent.REDBEAMS_START_EVENT.selector(),
                new RedbeamsEvent(RedbeamsStartEvent.REDBEAMS_START_EVENT.selector(), dbStack.getId()));
    }
}
