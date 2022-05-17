package com.sequenceiq.redbeams;

import static com.sequenceiq.redbeams.flow.redbeams.termination.RedbeamsTerminationEvent.REDBEAMS_TERMINATION_EVENT;

import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.flow.core.ApplicationFlowInformation;
import com.sequenceiq.flow.core.config.FlowConfiguration;
import com.sequenceiq.redbeams.flow.redbeams.termination.RedbeamsTerminationFlowConfig;

@Component
public class RedbeamsFlowInformation implements ApplicationFlowInformation {

    @Override
    public List<String> getAllowedParallelFlows() {
        return List.of(REDBEAMS_TERMINATION_EVENT.event());
    }

    @Override
    public List<Class<? extends FlowConfiguration<?>>> getTerminationFlow() {
        return Collections.singletonList(RedbeamsTerminationFlowConfig.class);
    }
}
