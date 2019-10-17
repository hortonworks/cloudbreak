package com.sequenceiq.freeipa;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.flow.core.ApplicationFlowInformation;
import com.sequenceiq.flow.core.config.FlowConfiguration;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.FreeIpaCleanupEvent;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.FreeIpaCleanupFlowConfig;
import com.sequenceiq.freeipa.flow.freeipa.provision.FreeIpaProvisionFlowConfig;
import com.sequenceiq.freeipa.flow.stack.provision.StackProvisionFlowConfig;
import com.sequenceiq.freeipa.flow.stack.start.StackStartFlowConfig;
import com.sequenceiq.freeipa.flow.stack.stop.StackStopFlowConfig;
import com.sequenceiq.freeipa.flow.stack.termination.StackTerminationFlowConfig;

@Component
public class FreeIpaFlowInformation implements ApplicationFlowInformation {

    private static final List<Class<? extends FlowConfiguration<?>>> RESTARTABLE_FLOWS = List.of(
            StackProvisionFlowConfig.class,
            StackTerminationFlowConfig.class,
            FreeIpaProvisionFlowConfig.class,
            StackStartFlowConfig.class,
            StackStopFlowConfig.class,
            FreeIpaCleanupFlowConfig.class);

    private static final List<String> PARALLEL_FLOWS = List.of(FreeIpaCleanupEvent.CLEANUP_EVENT.event());

    @Override
    public List<Class<? extends FlowConfiguration<?>>> getRestartableFlows() {
        return RESTARTABLE_FLOWS;
    }

    @Override
    public List<String> getAllowedParallelFlows() {
        return PARALLEL_FLOWS;
    }
}
