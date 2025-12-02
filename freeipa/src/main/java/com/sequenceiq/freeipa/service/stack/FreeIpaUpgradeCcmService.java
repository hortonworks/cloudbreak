package com.sequenceiq.freeipa.service.stack;

import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus.UPGRADE_CCM_REQUESTED;
import static com.sequenceiq.freeipa.flow.chain.FlowChainTriggers.UPGRADE_CCM_CHAIN_TRIGGER_EVENT;

import java.util.List;
import java.util.UUID;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SuccessDetails;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationState;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationStatus;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationType;
import com.sequenceiq.freeipa.converter.operation.OperationToOperationStatusConverter;
import com.sequenceiq.freeipa.entity.Operation;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.stack.upgrade.ccm.event.UpgradeCcmFlowChainTriggerEvent;
import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaFlowManager;
import com.sequenceiq.freeipa.service.operation.OperationService;

@Service
public class FreeIpaUpgradeCcmService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaUpgradeCcmService.class);

    @Inject
    private FreeIpaFlowManager flowManager;

    @Inject
    private StackService stackService;

    @Inject
    private StackUpdater stackUpdater;

    @Inject
    private OperationService operationService;

    @Inject
    private OperationToOperationStatusConverter operationConverter;

    public OperationStatus upgradeCcm(String environmentCrn, String accountId) {
        MDCBuilder.addEnvCrn(environmentCrn);
        Stack stack = stackService.getByEnvironmentCrnAndAccountIdWithListsAndMdcContext(environmentCrn, accountId);
        MDCBuilder.addAccountId(accountId);
        if (isAlreadyUpgraded(stack)) {
            LOGGER.info("FreeIPA stack '{}' is already upgraded to {}.", stack.getName(), stack.getTunnel());
            return alreadyFinishedOperationStatus(environmentCrn);
        } else {
            validateCcmUpgrade(stack);
            return startUpgradeOperation(accountId, stack);
        }
    }

    private boolean isAlreadyUpgraded(Stack stack) {
        return stack.getTunnel() == Tunnel.latestUpgradeTarget();
    }

    private OperationStatus alreadyFinishedOperationStatus(String environmentCrn) {
        SuccessDetails successDetails = new SuccessDetails(environmentCrn);
        long timestamp = System.currentTimeMillis();
        return new OperationStatus(UUID.randomUUID().toString(), OperationType.UPGRADE_CCM, OperationState.COMPLETED, List.of(successDetails),
                List.of(), null, timestamp, timestamp);
    }

    private void validateCcmUpgrade(Stack stack) {
        if (!stack.getStackStatus().getStatus().isCcmUpgradeablePhase()) {
            throw new BadRequestException(
                    String.format("FreeIPA stack '%s' must be AVAILABLE to start Cluster Connectivity Manager upgrade.", stack.getName()));
        }

        if (!Tunnel.getUpgradables().contains(stack.getTunnel())) {
            throw new BadRequestException(
                    String.format("FreeIPA stack '%s' has a tunnel type '%s' and thus Cluster Connectivity Manager upgrade is not possible.%n" +
                                    "Allowed original tunnel types are: %s", stack.getName(), stack.getTunnel(), Tunnel.getUpgradables()));
        }
    }

    private OperationStatus startUpgradeOperation(String accountId, Stack stack) {
        LOGGER.info("Start 'UPGRADE_CCM' operation");
        Operation operation = operationService.startOperation(accountId, OperationType.UPGRADE_CCM, List.of(stack.getEnvironmentCrn()), List.of());
        if (OperationState.RUNNING == operation.getStatus()) {
            return operationConverter.convert(triggerCcmUpgradeFlowChain(accountId, operation, stack));
        } else {
            LOGGER.info("Operation isn't in RUNNING state: {}", operation);
            return operationConverter.convert(operation);
        }
    }

    private Operation triggerCcmUpgradeFlowChain(String accountId, Operation operation, Stack stack) {
        try {
            LOGGER.info("Starting CCM upgrade flow chain, new status: {}", UPGRADE_CCM_REQUESTED);
            stackUpdater.updateStackStatus(stack, UPGRADE_CCM_REQUESTED, "Starting of CCM upgrade requested");
            UpgradeCcmFlowChainTriggerEvent event = new UpgradeCcmFlowChainTriggerEvent(UPGRADE_CCM_CHAIN_TRIGGER_EVENT,
                    operation.getOperationId(), stack.getId(), stack.getTunnel());
            flowManager.notify(UPGRADE_CCM_CHAIN_TRIGGER_EVENT, event);
            LOGGER.info("Started CCM upgrade flow");
            return operation;
        } catch (Exception e) {
            LOGGER.error("Couldn't start CCM upgrade flow", e);
            return operationService.failOperation(accountId, operation.getOperationId(),
                    "Couldn't start Cluster Connectivity Manager upgrade flow: " + e.getMessage());
        }
    }

}
