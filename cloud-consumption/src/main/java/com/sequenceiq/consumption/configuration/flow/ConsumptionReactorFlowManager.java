package com.sequenceiq.consumption.configuration.flow;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.flow.core.FlowConstants;
import com.sequenceiq.flow.reactor.api.event.EventSender;
import com.sequenceiq.flow.service.FlowCancelService;

@Service
public class ConsumptionReactorFlowManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsumptionReactorFlowManager.class);

    private final EventSender eventSender;

    private final FlowCancelService flowCancelService;

    public ConsumptionReactorFlowManager(EventSender eventSender, FlowCancelService flowCancelService) {
        this.eventSender = eventSender;
        this.flowCancelService = flowCancelService;
    }

    private Map<String, Object> getFlowTriggerUsercrn(String userCrn) {
        return Map.of(FlowConstants.FLOW_TRIGGER_USERCRN, userCrn);
    }
}
