package com.sequenceiq.cloudbreak.core.flow2.stack.downscale;

import static com.sequenceiq.cloudbreak.core.flow2.ContextKeys.PRIVATE_IDS;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.cloudbreak.core.flow2.event.StackDownscaleTriggerEvent;
import com.sequenceiq.cloudbreak.service.flowlog.FlowLogUtil;
import com.sequenceiq.flow.core.FlowTriggerCondition;
import com.sequenceiq.flow.core.FlowTriggerConditionResult;
import com.sequenceiq.flow.domain.FlowLog;
import com.sequenceiq.flow.service.flowlog.FlowLogDBService;

@Component
public class StackDownscaleFlowTriggerCondition implements FlowTriggerCondition {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackDownscaleFlowTriggerCondition.class);

    @Inject
    private FlowLogDBService flowLogDBService;

    @Override
    public FlowTriggerConditionResult isFlowTriggerable(Payload payload) {
        FlowTriggerConditionResult result = FlowTriggerConditionResult.ok();
        try {
            StackDownscaleTriggerEvent triggerEvent = (StackDownscaleTriggerEvent) payload;
            if (triggerEvent.isPurgeZombies()) {
                Optional<FlowLog> lastFlowLog = flowLogDBService.getLastFlowLog(payload.getResourceId());
                if (lastFlowLog.isPresent()) {
                    FlowLog flowLog = lastFlowLog.get();
                    Map<Object, Object> variables = FlowLogUtil.deserializeVariables(flowLog);
                    Object privateIds = variables.get(PRIVATE_IDS);
                    if (privateIds == null || ((Set<Long>) privateIds).isEmpty()) {
                        result = FlowTriggerConditionResult.skip("Skip stack downscale flow, no downscaleable zombie nodes found.");
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Stack downscale trigger condition failed", e);
            result = FlowTriggerConditionResult.fail("Stack downscale trigger condition failed.");
        }
        return result;
    }
}
