package com.sequenceiq.datalake.flow;

import static com.sequenceiq.datalake.flow.delete.SdxDeleteEvent.SDX_DELETE_EVENT;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.flow.core.ApplicationFlowInformation;
import com.sequenceiq.flow.core.config.FlowConfiguration;

@Component
public class SdxFlowInformation implements ApplicationFlowInformation {

    private static final List<String> ALLOWED_PARALLEL_FLOWS = Collections.singletonList(SDX_DELETE_EVENT.event());

    @Override
    public List<Class<? extends FlowConfiguration<?>>> getRestartableFlows() {
        return new ArrayList<>();
    }

    @Override
    public List<String> getAllowedParallelFlows() {
        return ALLOWED_PARALLEL_FLOWS;
    }
}
