package com.sequenceiq.freeipa.service.operation;

import java.util.List;
import java.util.Optional;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.flow.api.model.operation.OperationFlowsView;
import com.sequenceiq.flow.api.model.operation.OperationResource;
import com.sequenceiq.flow.api.model.operation.OperationView;
import com.sequenceiq.flow.converter.OperationDetailsPopulator;
import com.sequenceiq.flow.core.stats.FlowOperationStatisticsService;
import com.sequenceiq.freeipa.flow.freeipa.provision.FreeIpaProvisionFlowConfig;
import com.sequenceiq.freeipa.flow.stack.provision.StackProvisionFlowConfig;

@Service
public class FlowOperationService {

    @Inject
    private FlowOperationStatisticsService flowOperationStatisticsService;

    @Inject
    private OperationDetailsPopulator operationDetailsPopulator;

    public OperationView getOperationProgressByEnvironmentCrn(String environmentCrn, boolean detailed) {
        OperationView operationView = new OperationView();
        Optional<OperationFlowsView> operationFlowsViewOpt = flowOperationStatisticsService.getLastFlowOperationByResourceCrn(environmentCrn);
        if (operationFlowsViewOpt.isPresent()) {
            OperationFlowsView operationFlowsView = operationFlowsViewOpt.get();
            OperationResource operationResource = OperationResource.FREEIPA;
            com.sequenceiq.flow.api.model.operation.OperationType operationType = operationFlowsView.getOperationType();
            if (com.sequenceiq.flow.api.model.operation.OperationType.PROVISION.equals(operationType)) {
                operationView = operationDetailsPopulator.createOperationView(operationFlowsView, operationResource,
                        List.of(StackProvisionFlowConfig.class, FreeIpaProvisionFlowConfig.class));
            } else {
                operationView = operationDetailsPopulator.createOperationView(operationFlowsView, operationResource);
            }
        }
        return operationView;
    }
}
