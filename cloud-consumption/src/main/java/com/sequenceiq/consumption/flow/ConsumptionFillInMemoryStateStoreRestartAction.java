package com.sequenceiq.consumption.flow;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.consumption.domain.Consumption;
import com.sequenceiq.consumption.service.ConsumptionService;
import com.sequenceiq.flow.core.RestartContext;
import com.sequenceiq.flow.core.restart.DefaultRestartAction;

@Component("ConsumptionFillInMemoryStateStoreRestartAction")
public class ConsumptionFillInMemoryStateStoreRestartAction extends DefaultRestartAction {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConsumptionFillInMemoryStateStoreRestartAction.class);

    @Inject
    private ConsumptionService consumptionService;

    @Override
    public void doBeforeRestart(RestartContext restartContext, Object payload) {
        LOGGER.debug("Restoring MDC context and InMemoryStateStore entry for flow: '{}', flow chain: '{}', event: '{}'",
                restartContext.getFlowId(), restartContext.getFlowChainId(), restartContext.getEvent());
        try {
            Consumption consumption = consumptionService.findConsumptionById(restartContext.getResourceId());
            MDCBuilder.buildMdcContext(consumption);
            MDCBuilder.addFlowId(restartContext.getFlowId());
        } catch (NotFoundException e) {
            LOGGER.error("Consumption not found with id [{}], error: {}", restartContext.getResourceId(), e.getMessage());
        }
    }
}
