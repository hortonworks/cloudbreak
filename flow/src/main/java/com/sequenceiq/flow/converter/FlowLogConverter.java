package com.sequenceiq.flow.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.flow.api.model.FlowLogResponse;
import com.sequenceiq.flow.api.model.StateStatus;
import com.sequenceiq.flow.domain.FlowLog;

@Component
public class FlowLogConverter extends AbstractConversionServiceAwareConverter<FlowLog, FlowLogResponse> {

    @Override
    public FlowLogResponse convert(FlowLog source) {
        FlowLogResponse flowLogResponse = new FlowLogResponse();
        flowLogResponse.setCreated(source.getCreated());
        flowLogResponse.setCurrentState(source.getCurrentState());
        flowLogResponse.setFinalized(source.getFinalized());
        flowLogResponse.setFlowChainId(source.getFlowChainId());
        flowLogResponse.setFlowId(source.getFlowId());
        flowLogResponse.setFlowTriggerUserCrn(source.getFlowTriggerUserCrn());
        flowLogResponse.setResourceId(source.getResourceId());
        flowLogResponse.setNextEvent(source.getNextEvent());
        flowLogResponse.setNodeId(source.getCloudbreakNodeId());
        if (source.getStateStatus() != null) {
            flowLogResponse.setStateStatus(StateStatus.valueOf(source.getStateStatus().name()));
        }
        return flowLogResponse;
    }
}
