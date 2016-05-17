package com.sequenceiq.cloudbreak.core.flow2.stackandcluster.downscale;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.downscale.ClusterDownscaleEvent.DECOMMISSION_AND_DOWNSCALE_EVENT;

import java.util.EnumSet;
import java.util.Optional;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.Payload;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.common.type.ScalingType;
import com.sequenceiq.cloudbreak.core.flow.FlowPhases;
import com.sequenceiq.cloudbreak.core.flow.context.StackScalingContext;
import com.sequenceiq.cloudbreak.core.flow2.ChainFlow;
import com.sequenceiq.cloudbreak.core.flow2.Flow;
import com.sequenceiq.cloudbreak.core.flow2.cluster.downscale.ClusterDownscaleEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.downscale.ClusterDownscaleFlowConfig;
import com.sequenceiq.cloudbreak.domain.Constraint;
import com.sequenceiq.cloudbreak.domain.HostGroup;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.reactor.api.event.ScalingAdjustmentPayload;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.stack.StackService;

import reactor.bus.Event;

@Component
public class ClusterAndStackDownscaleFlowConfig extends ClusterDownscaleFlowConfig {
    private static final EnumSet<ClusterDownscaleEvent> OWNEVENTS = EnumSet.of(DECOMMISSION_AND_DOWNSCALE_EVENT);

    @Inject
    private StackService stackService;

    @Inject
    private HostGroupService hostGroupService;

    @Override
    public Flow createFlow(String flowId) {
        return new ChainFlow(super.createFlow(flowId)) {
            @Override
            public String nextSelector() {
                return FlowPhases.STACK_DOWNSCALE.name();
            }

            @Override
            public Object nextPayload(Event<? extends Payload> event) {
                Long stackId = event.getData().getStackId();
                Stack stack = stackService.getById(stackId);
                ScalingAdjustmentPayload context = (ScalingAdjustmentPayload) event.getData();
                HostGroup hostGroup = hostGroupService.getByClusterIdAndName(stack.getCluster().getId(), context.getHostGroupName());
                Constraint hostGroupConstraint = hostGroup.getConstraint();
                String instanceGroupName = Optional.ofNullable(hostGroupConstraint.getInstanceGroup()).map(ig -> ig.getGroupName()).orElse(null);
                return new StackScalingContext(stack.getId(), Platform.platform(stack.cloudPlatform()), context.getScalingAdjustment(),
                        instanceGroupName, ScalingType.DOWNSCALE_TOGETHER);
            }
        };
    }

    @Override
    public ClusterDownscaleEvent[] getEvents() {
        return OWNEVENTS.toArray(new ClusterDownscaleEvent[]{});
    }

    @Override
    public ClusterDownscaleEvent[] getInitEvents() {
        return new ClusterDownscaleEvent[] { DECOMMISSION_AND_DOWNSCALE_EVENT };
    }
}
