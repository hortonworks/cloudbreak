package com.sequenceiq.environment.environment.flow;

import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.flow.core.ApplicationFlowInformation;
import com.sequenceiq.flow.core.config.FlowConfiguration;

@Component
public class EnvironmentFlowInformation implements ApplicationFlowInformation {

    @Override
    public List<Class<? extends FlowConfiguration<?>>> getRestartableFlows() {
        return Collections.emptyList();
    }

    @Override
    public List<String> getAllowedParallelFlows() {
        return Collections.emptyList();
    }
}
