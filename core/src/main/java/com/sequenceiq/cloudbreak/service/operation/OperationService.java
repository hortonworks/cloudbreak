package com.sequenceiq.cloudbreak.service.operation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.database.DatabaseAvailabilityType;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.externaldatabase.provision.config.ExternalDatabaseCreationFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.validate.cloud.config.CloudConfigValidationFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.validate.kerberosconfig.config.KerberosConfigValidationFlowConfig;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.database.DatabaseService;
import com.sequenceiq.distrox.v1.distrox.StackOperations;
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

    private final FlowService flowService;

    private final DatabaseService databaseService;

    private final StackOperations stackOperations;

    private final OperationDetailsPopulator operationDetailsPopulator;

    public OperationService(FlowService flowService, DatabaseService databaseService, StackOperations stackOperations,
            OperationDetailsPopulator operationDetailsPopulator) {
        this.flowService = flowService;
        this.databaseService = databaseService;
        this.stackOperations = stackOperations;
        this.operationDetailsPopulator = operationDetailsPopulator;
    }

    public OperationView getOperationProgressByResourceCrn(String resourceCrn, boolean detailed) {
        OperationView stackOperationView = new OperationView();
        try {
            OperationResource operationResource = OperationResource.fromCrn(Crn.safeFromString(resourceCrn));
            Optional<OperationFlowsView> operationFlowsViewOpt = flowService.getLastFlowOperationByResourceCrn(resourceCrn);
            if (operationFlowsViewOpt.isPresent()) {
                OperationFlowsView operationFlowsView = operationFlowsViewOpt.get();
                OperationType operationType = operationFlowsView.getOperationType();
                if (OperationType.PROVISION.equals(operationType)) {
                    stackOperationView = handleProvisionOperation(resourceCrn, operationResource, operationFlowsView, detailed);
                } else {
                    stackOperationView = operationDetailsPopulator.createOperationView(operationFlowsView, operationResource);
                }
            }
        } catch (Exception e) {
            LOGGER.debug(String.format("Could not fetch remote database details for stack with crn %s", resourceCrn), e);
        }
        return stackOperationView;
    }

    private OperationView handleProvisionOperation(String resourceCrn, OperationResource operationResource,
            OperationFlowsView operationFlowsView, boolean detailed) {
        OperationView stackOperationView;
        stackOperationView = operationDetailsPopulator.createOperationView(operationFlowsView, operationResource, List.of(
                CloudConfigValidationFlowConfig.class,
                KerberosConfigValidationFlowConfig.class,
                ExternalDatabaseCreationFlowConfig.class,
                StackCreationFlowConfig.class,
                ClusterCreationFlowConfig.class
        ));
        if (detailed && !OperationResource.DATALAKE.equals(operationResource)) {
            Stack stack = stackOperations.getStackByCrn(resourceCrn);
            DatabaseAvailabilityType databaseAvailabilityType = stack.getExternalDatabaseCreationType();
            Map<OperationResource, OperationCondition> conditionMap = new HashMap<>();
            Map<OperationResource, OperationView> subOperations = new HashMap<>();
            if (!DatabaseAvailabilityType.NONE.equals(databaseAvailabilityType)) {
                conditionMap.put(OperationResource.REMOTEDB, OperationCondition.REQUIRED);
                subOperations.put(OperationResource.REMOTEDB, databaseService.getRemoteDatabaseOperationProgress(stack, detailed).orElse(null));
            } else {
                conditionMap.put(OperationResource.REMOTEDB, OperationCondition.NONE);
            }
            stackOperationView.setSubOperationConditions(conditionMap);
            stackOperationView.setSubOperations(subOperations);
        }
        return stackOperationView;
    }
}
