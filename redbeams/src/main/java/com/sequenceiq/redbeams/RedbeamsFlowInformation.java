package com.sequenceiq.redbeams;

import static com.sequenceiq.redbeams.flow.redbeams.termination.RedbeamsTerminationEvent.REDBEAMS_TERMINATION_EVENT;

import java.util.Collections;
import java.util.List;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.flow.core.ApplicationFlowInformation;
import com.sequenceiq.flow.core.config.FlowConfiguration;
import com.sequenceiq.flow.domain.FlowLog;
import com.sequenceiq.redbeams.api.model.common.DetailedDBStackStatus;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.domain.stack.DBStackStatus;
import com.sequenceiq.redbeams.flow.redbeams.termination.RedbeamsTerminationFlowConfig;
import com.sequenceiq.redbeams.service.stack.DBStackService;

@Component
public class RedbeamsFlowInformation implements ApplicationFlowInformation {

    @Inject
    private Clock clock;

    @Inject
    private DBStackService dbStackService;

    @Override
    public List<String> getAllowedParallelFlows() {
        return List.of(REDBEAMS_TERMINATION_EVENT.event());
    }

    @Override
    public List<Class<? extends FlowConfiguration<?>>> getTerminationFlow() {
        return Collections.singletonList(RedbeamsTerminationFlowConfig.class);
    }

    @Override
    public void handleFlowFail(FlowLog flowLog) {
        DBStack dbStack = dbStackService.getById(flowLog.getResourceId());
        LOGGER.info("Handling failed redbeams flow {} for {}", flowLog, dbStack);
        if (dbStack.getStatus() != null) {
            DBStackStatus dbStackFailedStatus = new DBStackStatus(dbStack, dbStack.getStatus().mapToFailedIfInProgress(), "Flow failed",
                    DetailedDBStackStatus.UNKNOWN, clock.getCurrentInstant().toEpochMilli());
            dbStack.setDBStackStatus(dbStackFailedStatus);
            dbStackService.save(dbStack);
        }
    }

}
