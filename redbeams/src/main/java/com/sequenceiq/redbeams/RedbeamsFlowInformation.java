package com.sequenceiq.redbeams;

import com.sequenceiq.flow.core.ApplicationFlowInformation;
import com.sequenceiq.flow.core.config.FlowConfiguration;

import java.util.List;

import org.springframework.stereotype.Component;

@Component
public class RedbeamsFlowInformation implements ApplicationFlowInformation {
    @Override
    public List<Class<? extends FlowConfiguration<?>>> getRestartableFlows() {
        return List.of();
    }

    @Override
    public List<String> getAllowedParallelFlows() {
        return List.of();
    }
}
