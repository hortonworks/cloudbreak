package com.sequenceiq.flow.core;

import java.util.List;

import com.sequenceiq.flow.core.config.FlowConfiguration;
import com.sequenceiq.flow.domain.FlowLog;

public interface ApplicationFlowInformation  {

    List<Class<? extends FlowConfiguration<?>>> getRestartableFlows();

    List<String> getAllowedParallelFlows();

    List<Class<? extends FlowConfiguration<?>>> getTerminationFlow();

    default void handleFlowFail(FlowLog flowLog) {

    }
}
