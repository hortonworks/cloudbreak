package com.sequenceiq.cloudbreak.core.flow2;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus.UNKNOWN;

import java.util.Arrays;
import java.util.List;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.flow2.cluster.termination.ClusterTerminationEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.termination.ClusterTerminationFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.externaldatabase.terminate.config.ExternalDatabaseTerminationEvent;
import com.sequenceiq.cloudbreak.core.flow2.externaldatabase.terminate.config.ExternalDatabaseTerminationFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.stack.termination.StackTerminationEvent;
import com.sequenceiq.cloudbreak.core.flow2.stack.termination.StackTerminationFlowConfig;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackStatus;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.flow.core.ApplicationFlowInformation;
import com.sequenceiq.flow.core.config.FlowConfiguration;
import com.sequenceiq.flow.domain.FlowLog;

@Component
public class CloudbreakFlowInformation implements ApplicationFlowInformation {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudbreakFlowInformation.class);

    private static final List<String> ALLOWED_PARALLEL_FLOWS = List.of(
            ClusterTerminationEvent.TERMINATION_EVENT.event(),
            ExternalDatabaseTerminationEvent.START_EXTERNAL_DATABASE_TERMINATION_EVENT.event(),
            StackTerminationEvent.TERMINATION_EVENT.event(),
            ClusterTerminationEvent.PROPER_TERMINATION_EVENT.event());

    @Inject
    private StackService stackService;

    @Override
    public List<String> getAllowedParallelFlows() {
        return ALLOWED_PARALLEL_FLOWS;
    }

    @Override
    public List<Class<? extends FlowConfiguration<?>>> getTerminationFlow() {
        return Arrays.asList(StackTerminationFlowConfig.class, ClusterTerminationFlowConfig.class, ExternalDatabaseTerminationFlowConfig.class);
    }

    @Override
    public void handleFlowFail(FlowLog flowLog) {
        Stack stack = stackService.getById(flowLog.getResourceId());
        LOGGER.info("Handling failed CB flow {} for {}", flowLog, stack.getName());
        if (stack.getStackStatus() != null && stack.getStackStatus().getDetailedStackStatus() != null) {
            stack.setStackStatus(new StackStatus(stack, stack.getStackStatus().getStatus().mapToFailedIfInProgress(), "Flow failed", UNKNOWN));
            stackService.save(stack);
        }
    }
}
