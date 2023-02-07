package com.sequenceiq.environment.operation.service;

import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.environment.environment.service.freeipa.FreeIpaService;
import com.sequenceiq.flow.api.model.operation.OperationCondition;
import com.sequenceiq.flow.api.model.operation.OperationFlowsView;
import com.sequenceiq.flow.api.model.operation.OperationResource;
import com.sequenceiq.flow.api.model.operation.OperationType;
import com.sequenceiq.flow.api.model.operation.OperationView;
import com.sequenceiq.flow.converter.OperationDetailsPopulator;
import com.sequenceiq.flow.core.stats.FlowOperationStatisticsService;

@Service
public class OperationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(OperationService.class);

    private final EnvironmentService environmentService;

    private final FlowOperationStatisticsService flowOperationStatisticsService;

    private final FreeIpaService freeIpaService;

    private final OperationDetailsPopulator operationDetailsPopulator;

    public OperationService(EnvironmentService environmentService, FlowOperationStatisticsService flowOperationStatisticsService, FreeIpaService freeIpaService,
            OperationDetailsPopulator operationDetailsPopulator) {
        this.environmentService = environmentService;
        this.flowOperationStatisticsService = flowOperationStatisticsService;
        this.freeIpaService = freeIpaService;
        this.operationDetailsPopulator = operationDetailsPopulator;
    }

    public OperationView getOperationProgressByResourceCrn(String resourceCrn, boolean detailed) {
        OperationView response = new OperationView();
        Optional<OperationFlowsView> operationFlowsViewOpt = flowOperationStatisticsService.getLastFlowOperationByResourceCrn(resourceCrn);
        if (operationFlowsViewOpt.isPresent()) {
            OperationFlowsView operationFlowsView = operationFlowsViewOpt.get();
            OperationType operationType = operationFlowsView.getOperationType();
            response = operationDetailsPopulator.createOperationView(operationFlowsView, OperationResource.ENVIRONMENT);
            if (OperationType.PROVISION.equals(operationType)) {
                if (detailed) {
                    handleProvisionOperation(resourceCrn, response, detailed);
                } else {
                    LOGGER.debug("Skipping detailed environment provision operation response");
                }
            }
        }
        return response;
    }

    private void handleProvisionOperation(String resourceCrn, OperationView response, boolean detailed) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        EnvironmentDto envDto = environmentService.getByCrnAndAccountId(resourceCrn, accountId);
        boolean needsFreeipa = envDto.getFreeIpaCreation().isCreate();
        if (needsFreeipa) {
            Optional<OperationView> freeIpaFlows = freeIpaService.getFreeIpaOperation(resourceCrn, detailed);
            response.setSubOperationConditions(Map.of(OperationResource.FREEIPA, OperationCondition.REQUIRED));
            if (freeIpaFlows.isPresent()) {
                OperationView freeIpaOpView = freeIpaFlows.get();
                if (OperationType.PROVISION.equals(freeIpaOpView.getOperationType())) {
                    response.setSubOperations(Map.of(OperationResource.FREEIPA, freeIpaOpView));
                }
            }
        } else {
            response.setSubOperationConditions(Map.of(OperationResource.FREEIPA, OperationCondition.NONE));
        }
    }
}
