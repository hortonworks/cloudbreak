package com.sequenceiq.flow.core;

import java.util.List;

import com.sequenceiq.flow.core.config.FlowConfiguration;

public interface ApplicationFlowInformation  {
    List<Class<? extends FlowConfiguration<?>>> getRestartableFlows();

    List<String> getAllowedParallelFlows();

    default Class<? extends FlowConfiguration<?>> getTerminationFlow() {
        return null;
    }
}
