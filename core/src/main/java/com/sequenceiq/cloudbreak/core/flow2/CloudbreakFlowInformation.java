package com.sequenceiq.cloudbreak.core.flow2;

import static com.sequenceiq.cloudbreak.core.flow2.stack.termination.StackTerminationEvent.TERMINATION_EVENT;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.flow2.cluster.downscale.ClusterDownscaleFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.cluster.repair.master.ha.ChangePrimaryGatewayFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.cluster.reset.ClusterResetFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.cluster.start.ClusterStartFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.cluster.stop.ClusterStopFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.cluster.sync.ClusterSyncFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.cluster.termination.ClusterTerminationFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.cluster.upgrade.ClusterUpgradeFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.cluster.userpasswd.ClusterCredentialChangeFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.config.FlowConfiguration;
import com.sequenceiq.cloudbreak.core.flow2.stack.downscale.StackDownscaleConfig;
import com.sequenceiq.cloudbreak.core.flow2.stack.instance.termination.InstanceTerminationFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.stack.repair.ManualStackRepairTriggerFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.stack.start.StackStartFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.stack.stop.StackStopFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.stack.sync.StackSyncFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.stack.termination.StackTerminationFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.stack.upscale.StackUpscaleConfig;

@Component
public class CloudbreakFlowInformation implements ApplicationFlowInformation {
    private static final List<String> ALLOWED_PARALLEL_FLOWS = Collections.singletonList(TERMINATION_EVENT.event());

    private static final List<Class<? extends FlowConfiguration<?>>> RESTARTABLE_FLOWS = Arrays.asList(
            StackCreationFlowConfig.class,
            StackSyncFlowConfig.class, StackTerminationFlowConfig.class, StackStopFlowConfig.class, StackStartFlowConfig.class,
            StackUpscaleConfig.class, StackDownscaleConfig.class,
            InstanceTerminationFlowConfig.class,
            ManualStackRepairTriggerFlowConfig.class,
            ClusterCreationFlowConfig.class,
            ClusterSyncFlowConfig.class, ClusterTerminationFlowConfig.class, ClusterCredentialChangeFlowConfig.class,
            ClusterStartFlowConfig.class, ClusterStopFlowConfig.class,
            ClusterUpscaleFlowConfig.class, ClusterDownscaleFlowConfig.class,
            ClusterUpgradeFlowConfig.class, ClusterResetFlowConfig.class, ChangePrimaryGatewayFlowConfig.class
    );

    public List<Class<? extends FlowConfiguration<?>>> getRestartableFlows() {
        return RESTARTABLE_FLOWS;
    }

    public List<String> getAllowedParallelFlows() {
        return ALLOWED_PARALLEL_FLOWS;
    }
}
