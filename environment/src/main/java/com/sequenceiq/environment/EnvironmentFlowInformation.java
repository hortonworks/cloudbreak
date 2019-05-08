package com.sequenceiq.environment;

import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.flow2.ApplicationFlowInformation;
import com.sequenceiq.cloudbreak.core.flow2.config.FlowConfiguration;

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
