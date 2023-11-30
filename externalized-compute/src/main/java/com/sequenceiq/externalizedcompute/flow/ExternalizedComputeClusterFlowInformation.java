package com.sequenceiq.externalizedcompute.flow;

import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.externalizedcompute.flow.delete.ExternalizedComputeClusterDeleteFlowConfig;
import com.sequenceiq.flow.core.ApplicationFlowInformation;
import com.sequenceiq.flow.core.config.FlowConfiguration;

@Component
public class ExternalizedComputeClusterFlowInformation implements ApplicationFlowInformation {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExternalizedComputeClusterFlowInformation.class);

    private static final List<String> ALLOWED_PARALLEL_FLOWS = Collections.emptyList();

    @Override
    public List<String> getAllowedParallelFlows() {
        return ALLOWED_PARALLEL_FLOWS;
    }

    @Override
    public List<Class<? extends FlowConfiguration<?>>> getTerminationFlow() {
        return Collections.singletonList(ExternalizedComputeClusterDeleteFlowConfig.class);
    }
}
