package com.sequenceiq.consumption.flow.consumption.storage;

import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.statemachine.StateContext;

import com.sequenceiq.cloudbreak.common.event.ResourceCrnPayload;
import com.sequenceiq.cloudbreak.logger.MdcContext;
import com.sequenceiq.consumption.domain.Consumption;
import com.sequenceiq.consumption.flow.consumption.ConsumptionContext;
import com.sequenceiq.consumption.flow.consumption.storage.event.StorageConsumptionCollectionFailureEvent;
import com.sequenceiq.consumption.flow.consumption.storage.event.StorageConsumptionCollectionStateSelectors;
import com.sequenceiq.consumption.service.ConsumptionService;
import com.sequenceiq.flow.core.AbstractAction;
import com.sequenceiq.flow.core.FlowParameters;

abstract class AbstractStorageConsumptionCollectionAction<P extends ResourceCrnPayload>
        extends AbstractAction<StorageConsumptionCollectionState, StorageConsumptionCollectionStateSelectors, ConsumptionContext, P> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractStorageConsumptionCollectionAction.class);

    @Inject
    private ConsumptionService consumptionService;

    protected AbstractStorageConsumptionCollectionAction(Class<P> payloadClass) {
        super(payloadClass);
    }

    @Override
    protected ConsumptionContext createFlowContext(FlowParameters flowParameters,
            StateContext<StorageConsumptionCollectionState, StorageConsumptionCollectionStateSelectors> stateContext, P payload) {
        Consumption consumption = consumptionService.findConsumptionById(payload.getResourceId());
        return new ConsumptionContext(flowParameters, consumption);
    }

    @Override
    protected Object getFailurePayload(P payload, Optional<ConsumptionContext> flowContext, Exception ex) {
        return new StorageConsumptionCollectionFailureEvent(payload.getResourceId(), ex, payload.getResourceCrn());
    }

    @Override
    protected void prepareExecution(P payload, Map<Object, Object> variables) {
        if (payload != null) {
            MdcContext.builder().resourceCrn(payload.getResourceCrn()).buildMdc();
        } else {
            LOGGER.warn("Payload was null in prepareExecution so resourceCrn cannot be added to the MdcContext!");
        }
    }
}
