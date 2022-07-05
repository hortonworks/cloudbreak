package com.sequenceiq.flow.core;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.flow.core.config.FlowConfiguration;
import com.sequenceiq.flow.domain.FlowLog;

public interface ApplicationFlowInformation  {

    Logger LOGGER = LoggerFactory.getLogger(ApplicationFlowInformation.class);

    List<String> getAllowedParallelFlows();

    List<Class<? extends FlowConfiguration<?>>> getTerminationFlow();

    default void handleFlowFail(FlowLog flowLog) {
        LOGGER.debug("'handleFlowFail' is not implemented, affected flow: {}", flowLog);
    }
}
