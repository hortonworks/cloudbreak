package com.sequenceiq.freeipa.service.stack;

import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus.MODIFY_PROXY_CONFIG_REQUESTED;
import static com.sequenceiq.freeipa.flow.chain.FlowChainTriggers.MODIFY_PROXY_CHAIN_TRIGGER_EVENT;

import java.util.List;

import jakarta.inject.Inject;

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
import com.sequenceiq.freeipa.flow.stack.modify.proxy.event.ModifyProxyFlowChainTriggerEvent;
import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaFlowManager;
import com.sequenceiq.freeipa.service.operation.OperationService;

@Service
public class FreeipaModifyProxyConfigService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeipaModifyProxyConfigService.class);

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

    public OperationStatus modifyProxyConfig(String environmentCrn, String previousProxyCrn, String accountId) {
        MDCBuilder.addEnvCrn(environmentCrn);
        Stack stack = stackService.getByEnvironmentCrnAndAccountIdWithListsAndMdcContext(environmentCrn, accountId);
        validate(stack);
        return startModifyProxyConfigOperation(stack, previousProxyCrn);
    }

    private void validate(Stack stack) {
        if (!stack.getStackStatus().getStatus().isProxyConfigModifiablePhase()) {
            throw new BadRequestException("Proxy config modification is not supported in current FreeIpa status: " + stack.getStackStatus().getStatus());
        }
    }

    private OperationStatus startModifyProxyConfigOperation(Stack stack, String previousProxyCrn) {
        LOGGER.info("Start 'MODIFY_PROXY_CONFIG' operation");
        Operation operation = operationService.startOperation(stack.getAccountId(), OperationType.MODIFY_PROXY_CONFIG,
                List.of(stack.getEnvironmentCrn()), List.of());
        if (OperationState.RUNNING == operation.getStatus()) {
            operation = triggerModifyProxyConfigFlowChain(operation, stack, previousProxyCrn);
        } else {
            LOGGER.info("Operation isn't in RUNNING state: {}", operation);
        }
        return operationConverter.convert(operation);
    }

    private Operation triggerModifyProxyConfigFlowChain(Operation operation, Stack stack, String previousProxyCrn) {
        try {
            LOGGER.info("Starting modify proxy config flow chain, new status: {}", MODIFY_PROXY_CONFIG_REQUESTED);
            stackUpdater.updateStackStatus(stack, MODIFY_PROXY_CONFIG_REQUESTED, "Starting proxy config modification");
            ModifyProxyFlowChainTriggerEvent event = new ModifyProxyFlowChainTriggerEvent(MODIFY_PROXY_CHAIN_TRIGGER_EVENT,
                    stack.getId(), operation.getOperationId(), previousProxyCrn);
            flowManager.notify(MODIFY_PROXY_CHAIN_TRIGGER_EVENT, event);
            LOGGER.info("Started modify proxy config flow");
            return operation;
        } catch (Exception e) {
            LOGGER.error("Couldn't start modify proxy config flow", e);
            return operationService.failOperation(stack.getAccountId(), operation.getOperationId(),
                    "Couldn't start modify proxy config flow: " + e.getMessage());
        }
    }
}
