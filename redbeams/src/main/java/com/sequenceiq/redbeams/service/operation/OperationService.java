package com.sequenceiq.redbeams.service.operation;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.sequenceiq.flow.api.model.operation.OperationFlowsView;
import com.sequenceiq.flow.api.model.operation.OperationResource;
import com.sequenceiq.flow.api.model.operation.OperationView;
import com.sequenceiq.flow.converter.OperationDetailsPopulator;
import com.sequenceiq.flow.core.stats.FlowOperationStatisticsService;

@Component
public class OperationService {

    private final FlowOperationStatisticsService flowOperationStatisticsService;

    private final OperationDetailsPopulator operationDetailsPopulator;

    public OperationService(FlowOperationStatisticsService flowOperationStatisticsService, OperationDetailsPopulator operationDetailsPopulator) {
        this.flowOperationStatisticsService = flowOperationStatisticsService;
        this.operationDetailsPopulator = operationDetailsPopulator;
    }

    public OperationView getOperationProgressByResourceCrn(String resourceCrn, boolean detailed) {
        OperationView response = new OperationView();
        Optional<OperationFlowsView> operationFlowsViewOpt = flowOperationStatisticsService.getLastFlowOperationByResourceCrn(resourceCrn);
        if (operationFlowsViewOpt.isPresent()) {
            return operationDetailsPopulator.createOperationView(operationFlowsViewOpt.get(), OperationResource.REMOTEDB);
        }
        return response;
    }
}
