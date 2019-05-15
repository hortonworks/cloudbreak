package com.sequenceiq.datalake.flow;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.flow.core.ApplicationFlowInformation;
import com.sequenceiq.flow.core.config.FlowConfiguration;

@Component
public class SdxFlowInformation implements ApplicationFlowInformation {
    @Override
    public List<Class<? extends FlowConfiguration<?>>> getRestartableFlows() {
        return new ArrayList<>();
    }

    @Override
    public List<String> getAllowedParallelFlows() {
        return new ArrayList<>();
    }
}
