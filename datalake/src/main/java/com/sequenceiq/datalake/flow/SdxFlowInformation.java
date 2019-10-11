package com.sequenceiq.datalake.flow;

import static com.sequenceiq.datalake.flow.delete.SdxDeleteEvent.SDX_DELETE_EVENT;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.datalake.flow.create.SdxCreateFlowConfig;
import com.sequenceiq.datalake.flow.delete.SdxDeleteFlowConfig;
import com.sequenceiq.datalake.flow.start.SdxStartFlowConfig;
import com.sequenceiq.datalake.flow.stop.SdxStopFlowConfig;
import com.sequenceiq.flow.core.ApplicationFlowInformation;
import com.sequenceiq.flow.core.config.FlowConfiguration;

@Component
public class SdxFlowInformation implements ApplicationFlowInformation {

    private static final List<Class<? extends FlowConfiguration<?>>> RESTARTABLE_FLOWS = Arrays.asList(
            SdxCreateFlowConfig.class,
            SdxDeleteFlowConfig.class,
            SdxStartFlowConfig.class,
            SdxStopFlowConfig.class);

    private static final List<String> ALLOWED_PARALLEL_FLOWS = Collections.singletonList(SDX_DELETE_EVENT.event());

    @Override
    public List<Class<? extends FlowConfiguration<?>>> getRestartableFlows() {
        return RESTARTABLE_FLOWS;
    }

    @Override
    public List<String> getAllowedParallelFlows() {
        return ALLOWED_PARALLEL_FLOWS;
    }

    @Override
    public List<Class<? extends FlowConfiguration<?>>> getTerminationFlow() {
        return Collections.singletonList(SdxDeleteFlowConfig.class);
    }
}
