package com.sequenceiq.flow.core.edh;

import java.util.Optional;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.common.usage.UsageProto.CDPCloudbreakFlowEvent;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.usage.UsageReporter;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.ResourceIdProvider;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;
import com.sequenceiq.flow.core.listener.FlowTransitionContext;
import com.sequenceiq.flow.core.metrics.FlowStateUtil;
import com.sequenceiq.flow.domain.FlowLog;
import com.sequenceiq.flow.service.flowlog.FlowLogDBService;

@Component
public class FlowUsageSender {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlowUsageSender.class);

    @Inject
    private UsageReporter usageReporter;

    @Inject
    private ResourceIdProvider resourceIdProvider;

    @Inject
    private FlowLogDBService flowLogDBService;

    public void send(FlowTransitionContext flowTransitionContext, String nextFlowState, String flowEvent) {
        try {
            String flowType = flowTransitionContext.getFlowType();
            Enum<? extends FlowState> nextFlowStateEnum = FlowStateUtil.getFlowStateEnum(flowTransitionContext.getStateType(), nextFlowState, flowEvent);
            if (nextFlowStateEnum == null) {
                LOGGER.debug("nextFlowStateEnum is null for flow type: '{}', flow chain type: '{}', next flow state: '{}', flow event: '{}'," +
                        " flow metrics is not recorded!", flowType, flowTransitionContext.getFlowChainType(), nextFlowState, flowEvent);
                return;
            }
            AbstractFlowConfiguration.FlowEdgeConfig edgeConfig = flowTransitionContext.getEdgeConfig();
            boolean edgeState = edgeConfig.getInitState().equals(nextFlowStateEnum)
                    || edgeConfig.getFinalState().equals(nextFlowStateEnum)
                    || edgeConfig.getDefaultFailureState().equals(nextFlowStateEnum);

            String resourceCrn = resourceIdProvider.getResourceCrnByResourceId(flowTransitionContext.getResourceId());
            if (StringUtils.isNotEmpty(resourceCrn)) {
                String reason = getReason(edgeConfig, nextFlowStateEnum, flowTransitionContext.getFlowId());
                CDPCloudbreakFlowEvent event = CDPCloudbreakFlowEvent.newBuilder()
                        .setResourceCrn(resourceCrn)
                        .setRootFlowChainType(flowTransitionContext.getRootFlowChainType() != null ? flowTransitionContext.getRootFlowChainType() : "")
                        .setActualFlowChainType(flowTransitionContext.getActualFlowChainType() != null ? flowTransitionContext.getActualFlowChainType() : "")
                        .setFlowChainId(flowTransitionContext.getFlowChainId() != null ? flowTransitionContext.getFlowChainId() : "")
                        .setFlowType(flowType)
                        .setFlowId(flowTransitionContext.getFlowId())
                        .setStateType(flowTransitionContext.getStateType().getSimpleName())
                        .setFlowState(nextFlowStateEnum.name())
                        .setFlowEvent(flowEvent != null ? flowEvent : "")
                        .setEdgeState(edgeState)
                        .setReason(reason)
                        .setRequestId(MDCBuilder.getOrGenerateRequestId())
                        .build();
                usageReporter.cdpCloudbreakFlowEvent(event);
            } else {
                LOGGER.warn("Resource not found for flow usage event sending, resourceId: {}", flowTransitionContext.getResourceId());
            }
        } catch (Exception e) {
            LOGGER.warn("Flow usage event sending failed!", e);
        }
    }

    private String getReason(AbstractFlowConfiguration.FlowEdgeConfig edgeConfig, Enum<? extends FlowState> nextFlowStateEnum, String flowId) {
        if (edgeConfig.getDefaultFailureState().equals(nextFlowStateEnum)) {
            Optional<FlowLog> flowLog = flowLogDBService.findFirstByFlowIdOrderByCreatedDesc(flowId);
            if (flowLog.isPresent()) {
                String reason = flowLog.get().getReason();
                return reason != null ? reason : "";
            }
        }
        return "";
    }
}
