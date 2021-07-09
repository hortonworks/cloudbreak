package com.sequenceiq.datalake.service.sdx.operation;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.operation.OperationV4Endpoint;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.datalake.service.sdx.database.DatabaseService;
import com.sequenceiq.flow.api.model.operation.OperationCondition;
import com.sequenceiq.flow.api.model.operation.OperationFlowsView;
import com.sequenceiq.flow.api.model.operation.OperationResource;
import com.sequenceiq.flow.api.model.operation.OperationType;
import com.sequenceiq.flow.api.model.operation.OperationView;
import com.sequenceiq.flow.converter.OperationDetailsPopulator;
import com.sequenceiq.flow.service.FlowService;

@Component
public class OperationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(OperationService.class);

    private final OperationV4Endpoint operationV4Endpoint;

    private final SdxService sdxService;

    private final DatabaseService databaseService;

    private final FlowService flowService;

    private final OperationDetailsPopulator operationDetailsPopulator;

    public OperationService(OperationV4Endpoint operationV4Endpoint, SdxService sdxService, DatabaseService databaseService, FlowService flowService,
            OperationDetailsPopulator operationDetailsPopulator) {
        this.operationV4Endpoint = operationV4Endpoint;
        this.sdxService = sdxService;
        this.databaseService = databaseService;
        this.flowService = flowService;
        this.operationDetailsPopulator = operationDetailsPopulator;
    }

    public OperationView getOperationProgressByResourceCrn(String resourceCrn, boolean detailed) {
        OperationView sdxOperationView = new OperationView();
        Optional<OperationFlowsView> operationFlowsViewOpt = flowService.getLastFlowOperationByResourceCrn(resourceCrn);
        if (operationFlowsViewOpt.isPresent()) {
            OperationFlowsView operationFlowsView = operationFlowsViewOpt.get();
            OperationType operationType = operationFlowsView.getOperationType();
            sdxOperationView = operationDetailsPopulator.createOperationView(operationFlowsView, OperationResource.DATALAKE);
            if (OperationType.PROVISION.equals(operationType)) {
                if (detailed) {
                    handleProvisionOperation(resourceCrn, sdxOperationView, detailed);
                } else {
                    LOGGER.debug("Skipping detailed SDX provision operation response");
                }
            }
        }
        return sdxOperationView;
    }

    private void handleProvisionOperation(String resourceCrn, OperationView sdxOperationView, boolean detailed) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        SdxCluster sdxCluster = sdxService.getByCrn(userCrn, resourceCrn);
        boolean createDb = sdxCluster.isCreateDatabase();
        Map<OperationResource, OperationView> subOperations = new HashMap<>();
        Map<OperationResource, OperationCondition> conditionMap = new HashMap<>();
        conditionMap.put(OperationResource.DATALAKE, OperationCondition.REQUIRED);
        if (createDb) {
            conditionMap.put(OperationResource.REMOTEDB, OperationCondition.REQUIRED);
            try {
                OperationView rdbOperationView = databaseService.getOperationProgressStatus(sdxCluster.getDatabaseCrn(), detailed);
                if (OperationType.PROVISION.equals(rdbOperationView.getOperationType())) {
                    subOperations.put(OperationResource.REMOTEDB, rdbOperationView);
                }
            } catch (Exception e) {
                LOGGER.warn("Error during fetching provision progress from remote database API. Skip filling remote database response.", e);
            }
        } else {
            conditionMap.put(OperationResource.REMOTEDB, OperationCondition.NONE);
        }
        sdxOperationView.setSubOperationConditions(conditionMap);
        try {
            OperationView stackOperationProgressView = operationV4Endpoint.getOperationProgressByResourceCrn(resourceCrn, detailed);
            if (OperationType.PROVISION.equals(stackOperationProgressView.getOperationType())) {
                subOperations.put(OperationResource.DATALAKE, stackOperationProgressView);
            }
        } catch (Exception e) {
            LOGGER.warn("Error during fetching provision progress from stack API. Skip filling stack progress response.", e);
        }
        sdxOperationView.setSubOperations(subOperations);
    }
}
