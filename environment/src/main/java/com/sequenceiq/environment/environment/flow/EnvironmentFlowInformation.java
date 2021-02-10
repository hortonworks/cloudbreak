package com.sequenceiq.environment.environment.flow;

import static com.sequenceiq.environment.environment.flow.deletion.event.EnvClustersDeleteStateSelectors.START_DATAHUB_CLUSTERS_DELETE_EVENT;
import static com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteStateSelectors.START_FREEIPA_DELETE_EVENT;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.environment.environment.flow.creation.config.EnvCreationFlowConfig;
import com.sequenceiq.environment.environment.flow.deletion.config.EnvClustersDeleteFlowConfig;
import com.sequenceiq.environment.environment.flow.deletion.config.EnvDeleteFlowConfig;
import com.sequenceiq.flow.core.ApplicationFlowInformation;
import com.sequenceiq.flow.core.config.FlowConfiguration;

@Component
public class EnvironmentFlowInformation implements ApplicationFlowInformation {

    @Override
    public List<Class<? extends FlowConfiguration<?>>> getRestartableFlows() {
        return List.of(EnvCreationFlowConfig.class, EnvDeleteFlowConfig.class, EnvClustersDeleteFlowConfig.class);
    }

    @Override
    public List<String> getAllowedParallelFlows() {
        return List.of(START_FREEIPA_DELETE_EVENT.event(), START_DATAHUB_CLUSTERS_DELETE_EVENT.event());
    }

    @Override
    public List<Class<? extends FlowConfiguration<?>>> getTerminationFlow() {
        return List.of(EnvDeleteFlowConfig.class, EnvClustersDeleteFlowConfig.class);
    }
}
