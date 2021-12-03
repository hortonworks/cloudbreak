package com.sequenceiq.redbeams.service.operation;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.sequenceiq.flow.api.model.operation.OperationFlowsView;
import com.sequenceiq.flow.api.model.operation.OperationResource;
import com.sequenceiq.flow.api.model.operation.OperationView;
import com.sequenceiq.flow.converter.OperationDetailsPopulator;
import com.sequenceiq.flow.service.FlowService;

@Component
public class OperationService {

    private final FlowService flowService;

    private final OperationDetailsPopulator operationDetailsPopulator;

    public OperationService(FlowService flowService, OperationDetailsPopulator operationDetailsPopulator) {
        this.flowService = flowService;
        this.operationDetailsPopulator = operationDetailsPopulator;
    }

    public OperationView getOperationProgressByResourceCrn(String resourceCrn, boolean detailed) {
        OperationView response = new OperationView();
        Optional<OperationFlowsView> operationFlowsViewOpt = flowService.getLastFlowOperationByResourceCrn(resourceCrn);
        if (operationFlowsViewOpt.isPresent()) {
            return operationDetailsPopulator.createOperationView(operationFlowsViewOpt.get(), OperationResource.REMOTEDB);
        }
        return response;
    }
}
