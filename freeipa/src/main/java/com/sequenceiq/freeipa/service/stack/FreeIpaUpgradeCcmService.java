package com.sequenceiq.freeipa.service.stack;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationState;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationStatus;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationType;
import com.sequenceiq.freeipa.converter.operation.OperationToOperationStatusConverter;
import com.sequenceiq.freeipa.entity.Operation;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaFlowManager;
import com.sequenceiq.freeipa.service.operation.OperationService;

@Service
public class FreeIpaUpgradeCcmService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaUpgradeCcmService.class);

    private final FreeIpaFlowManager flowManager;

    private final StackService stackService;

    private final StackUpdater stackUpdater;

    private final OperationService operationService;

    private final OperationToOperationStatusConverter operationConverter;

    public FreeIpaUpgradeCcmService(FreeIpaFlowManager flowManager, StackService stackService, StackUpdater stackUpdater, OperationService operationService,
            OperationToOperationStatusConverter operationConverter) {
        this.flowManager = flowManager;
        this.stackService = stackService;
        this.stackUpdater = stackUpdater;
        this.operationService = operationService;
        this.operationConverter = operationConverter;
    }

    public OperationStatus upgradeCcm(String environmentCrn, String accountId) {
        MDCBuilder.addEnvCrn(environmentCrn);
        MDCBuilder.addAccountId(accountId);
        Stack stack = stackService.getByEnvironmentCrnAndAccountIdWithLists(environmentCrn, accountId);
        MDCBuilder.buildMdcContext(stack);
        validateCcmUpgrade(stack);

        LOGGER.info("Start 'UPGRADE_CCM' operation");
        Operation operation = operationService.startOperation(accountId, OperationType.UPGRADE_CCM, List.of(stack.getEnvironmentCrn()), List.of());
        if (OperationState.RUNNING == operation.getStatus()) {
            return operationConverter.convert(triggerCcmUpgradeFlow(accountId, operation));
        } else {
            LOGGER.info("Operation isn't in RUNNING state: {}", operation);
            return operationConverter.convert(operation);
        }
    }

    private void validateCcmUpgrade(Stack stack) {
        if (!stack.isAvailable()) {
            throw new BadRequestException(
                String.format("FreeIPA stack '%s' must be AVAILABLE to start Cluster Connectivity Manager upgrade.", stack.getName()));
        }
    }

    private Operation triggerCcmUpgradeFlow(String accountId, Operation operation) {
        try {
            LOGGER.info("Starting CCM upgrade flow");
            // TODO log & update stack status (if needed), then send event to FreeIpaFlowManager; see CB-14571
            // TODO what to do with the FlowIdentifier?
            LOGGER.info("Started CCM upgrade flow");
            return operation;
        } catch (Exception e) {
            LOGGER.error("Couldn't start CCM upgrade flow", e);
            return operationService.failOperation(accountId, operation.getOperationId(),
                    "Couldn't start Cluster Connectivity Manager upgrade flow: " + e.getMessage());
        }
    }

}
