package com.sequenceiq.remoteenvironment.flow;

import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.flow.core.ApplicationFlowInformation;
import com.sequenceiq.flow.core.config.FlowConfiguration;
import com.sequenceiq.flow.domain.FlowLog;

@Component
public class RemoteEnvironmentFlowInformation implements ApplicationFlowInformation {

    private static final Logger LOGGER = LoggerFactory.getLogger(RemoteEnvironmentFlowInformation.class);

    private static final List<String> ALLOWED_PARALLEL_FLOWS = List.of();

    @Override
    public List<String> getAllowedParallelFlows() {
        return ALLOWED_PARALLEL_FLOWS;
    }

    @Override
    public List<Class<? extends FlowConfiguration<?>>> getTerminationFlow() {
        return Arrays.asList();
    }

    @Override
    public void handleFlowFail(FlowLog flowLog) {
    }
}
