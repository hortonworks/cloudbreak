package com.sequenceiq.freeipa.service.stack;

import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus.UPGRADE_DEFAULT_OUTBOUND_REQUESTED;

import java.util.List;
import java.util.UUID;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.NetworkAttributes;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.common.api.type.OutboundType;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SuccessDetails;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationState;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationStatus;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationType;
import com.sequenceiq.freeipa.converter.operation.OperationToOperationStatusConverter;
import com.sequenceiq.freeipa.entity.Operation;
import com.sequenceiq.freeipa.entity.Resource;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaFlowManager;
import com.sequenceiq.freeipa.service.operation.OperationService;
import com.sequenceiq.freeipa.service.resource.ResourceAttributeUtil;
import com.sequenceiq.freeipa.service.resource.ResourceService;

@Service
public class FreeIpaUpgradeDefaultOutboundService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaUpgradeDefaultOutboundService.class);

    @Inject
    private FreeIpaFlowManager flowManager;

    @Inject
    private StackService stackService;

    @Inject
    private StackUpdater stackUpdater;

    @Inject
    private ResourceService resourceService;

    @Inject
    private ResourceAttributeUtil resourceAttributeUtil;

    @Inject
    private OperationService operationService;

    @Inject
    private OperationToOperationStatusConverter operationConverter;

    public OperationStatus upgradeDefaultOutbound(String environmentCrn, String accountId) {
        MDCBuilder.addEnvCrn(environmentCrn);
        Stack stack = stackService.getByEnvironmentCrnAndAccountIdWithListsAndMdcContext(environmentCrn, accountId);
        MDCBuilder.addAccountId(accountId);
        if (getCurrentDefaultOutbound(stack).isUpgradeable()) {
            return startUpgradeOperation(accountId, stack);
        } else {
            LOGGER.info("FreeIPA stack '{}' is already upgraded to {}.", stack.getName(), stack.getTunnel());
            return alreadyFinishedOperationStatus(environmentCrn);
        }
    }

    public OutboundType getCurrentDefaultOutbound(String environmentCrn, String accountId) {
        MDCBuilder.addEnvCrn(environmentCrn);
        Stack stack = stackService.getByEnvironmentCrnAndAccountIdWithListsAndMdcContext(environmentCrn, accountId);
        MDCBuilder.addAccountId(accountId);
        return getCurrentDefaultOutbound(stack);
    }

    private OperationStatus alreadyFinishedOperationStatus(String environmentCrn) {
        SuccessDetails successDetails = new SuccessDetails(environmentCrn);
        long timestamp = System.currentTimeMillis();
        return new OperationStatus(UUID.randomUUID().toString(), OperationType.UPGRADE_DEFAULT_OUTBOUND, OperationState.COMPLETED, List.of(successDetails),
                List.of(), null, timestamp, timestamp);
    }

    private OutboundType getCurrentDefaultOutbound(Stack stack) {

        List<Resource> networkResources = resourceService.findByStackIdAndType(stack.getId(), ResourceType.AZURE_NETWORK);
        if (networkResources.size() == 1) {
            Resource network = networkResources.getFirst();
            OutboundType outboundType = resourceAttributeUtil.getTypedAttributes(network, NetworkAttributes.class)
                    .map(NetworkAttributes::getOutboundType)
                    .orElse(OutboundType.NOT_DEFINED);
            LOGGER.info("Current OutboundType for stack {} is {}", stack.getName(), outboundType);
            return outboundType;
        } else {
            throw new BadRequestException(
                    String.format("FreeIPA stack '%s' has %d network resources, cannot determine OutboundType.", stack.getName(), networkResources.size()));
        }
    }

    private OperationStatus startUpgradeOperation(String accountId, Stack stack) {
        LOGGER.info("Start 'UPGRADE_DEFAULT_OUTBOUND' operation");
        Operation operation = operationService.startOperation(accountId, OperationType.UPGRADE_DEFAULT_OUTBOUND, List.of(stack.getEnvironmentCrn()), List.of());
        if (OperationState.RUNNING == operation.getStatus()) {
            return operationConverter.convert(triggerDefaultOutboundUpgradeFlowChain(accountId, operation, stack));
        } else {
            LOGGER.info("Operation isn't in RUNNING state: {}", operation);
            return operationConverter.convert(operation);
        }
    }

    private Operation triggerDefaultOutboundUpgradeFlowChain(String accountId, Operation operation, Stack stack) {
        try {
            LOGGER.info("Starting DEFAULT OUTBOUND upgrade flow chain, new status: {}", UPGRADE_DEFAULT_OUTBOUND_REQUESTED);
            stackUpdater.updateStackStatus(stack, UPGRADE_DEFAULT_OUTBOUND_REQUESTED, "Starting of DEFAULT OUTBOUND upgrade requested");
            // TODO: Uncomment when the flow manager is ready to handle this event
//            UpgradeDefaultOutboundFlowChainTriggerEvent event = new UpgradeDefaultOutboundFlowChainTriggerEvent(UPGRADE_DEFAULT_OUTBOUND_CHAIN_TRIGGER_EVENT,
//                    operation.getOperationId(), stack.getId(), stack.getTunnel());
//            flowManager.notify(UPGRADE_DEFAULT_OUTBOUND_CHAIN_TRIGGER_EVENT, event);
            LOGGER.info("Started DEFAULT OUTBOUND upgrade flow");
            return operation;
        } catch (Exception e) {
            LOGGER.error("Couldn't start DEFAULT OUTBOUND upgrade flow", e);
            return operationService.failOperation(accountId, operation.getOperationId(),
                    "Couldn't start Cluster Connectivity Manager upgrade flow: " + e.getMessage());
        }
    }

}
