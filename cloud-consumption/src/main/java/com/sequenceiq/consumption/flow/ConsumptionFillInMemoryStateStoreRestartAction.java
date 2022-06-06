package com.sequenceiq.consumption.flow;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.consumption.domain.Consumption;
import com.sequenceiq.consumption.service.ConsumptionService;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.restart.DefaultRestartAction;

@Component("ConsumptionFillInMemoryStateStoreRestartAction")
public class ConsumptionFillInMemoryStateStoreRestartAction extends DefaultRestartAction {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConsumptionFillInMemoryStateStoreRestartAction.class);

    @Inject
    private ConsumptionService consumptionService;

    @Override
    public void restart(FlowParameters flowParameters, String flowChainId, String event, Object payload) {
        LOGGER.debug("Restoring MDC context and InMemoryStateStore entry for flow: '{}', flow chain: '{}', event: '{}'", flowParameters.getFlowId(),
                flowChainId, event);
        Payload consumptionPayload = (Payload) payload;
        try {
            Consumption consumption = consumptionService.findConsumptionById(consumptionPayload.getResourceId());
            MDCBuilder.buildMdcContext(consumption);
            MDCBuilder.addFlowId(flowParameters.getFlowId());
            LOGGER.debug("MDC context and InMemoryStateStore entry have been restored for flow: '{}', flow chain: '{}', event: '{}'", flowParameters.getFlowId(),
                    flowChainId, event);
            super.restart(flowParameters, flowChainId, event, payload);
        } catch (NotFoundException e) {
            LOGGER.error("Consumption not found with id [{}], error: {}", consumptionPayload.getResourceId(), e.getMessage());
        }
    }
}
