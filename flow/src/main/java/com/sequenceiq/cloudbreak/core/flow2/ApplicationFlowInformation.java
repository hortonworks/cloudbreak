package com.sequenceiq.cloudbreak.core.flow2;

import java.util.List;

import com.sequenceiq.cloudbreak.core.flow2.config.FlowConfiguration;

public interface ApplicationFlowInformation {
    List<Class<? extends FlowConfiguration<?>>> getRestartableFlows();

    List<String> getAllowedParallelFlows();
}
