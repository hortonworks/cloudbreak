package com.sequenceiq.freeipa;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.flow.core.ApplicationFlowInformation;
import com.sequenceiq.flow.core.config.FlowConfiguration;

@Component
public class FreeIpaFlowInformation implements ApplicationFlowInformation {
    @Override
    public List<Class<? extends FlowConfiguration<?>>> getRestartableFlows() {
        return List.of();
    }

    @Override
    public List<String> getAllowedParallelFlows() {
        return List.of();
    }
}
