package com.sequenceiq.freeipa.service.binduser;

import static com.sequenceiq.freeipa.api.v1.operation.model.OperationState.RUNNING;
import static com.sequenceiq.freeipa.flow.freeipa.binduser.create.event.CreateBindUserFlowEvent.CREATE_BIND_USER_EVENT;

import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.binduser.BindUserCreateRequest;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationStatus;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationType;
import com.sequenceiq.freeipa.converter.operation.OperationToOperationStatusConverter;
import com.sequenceiq.freeipa.entity.Operation;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.freeipa.binduser.create.event.CreateBindUserEvent;
import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaFlowManager;
import com.sequenceiq.freeipa.service.operation.OperationService;
import com.sequenceiq.freeipa.service.stack.StackService;

@Service
public class BindUserCreateService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BindUserCreateService.class);

    @Inject
    private OperationService operationService;

    @Inject
    private FreeIpaFlowManager flowManager;

    @Inject
    private OperationToOperationStatusConverter operationConverter;

    @Inject
    private StackService stackService;

    public OperationStatus createBindUser(String accountId, BindUserCreateRequest request) {
        Stack stack = stackService.getByEnvironmentCrnAndAccountId(request.getEnvironmentCrn(), accountId);
        MDCBuilder.buildMdcContext(stack);
        LOGGER.info("Start 'BIND_USER_CREATE' operation from request: {}", request);
        Operation operation = operationService.startOperation(accountId, OperationType.BIND_USER_CREATE, List.of(request.getEnvironmentCrn()),
                List.of(request.getBindUserNameSuffix()));
        if (RUNNING == operation.getStatus()) {
            return operationConverter.convert(startCreateBindUserFLow(accountId, request, stack.getId(), operation));
        } else {
            LOGGER.info("Operation isn't in RUNNING state: {}", operation);
            return operationConverter.convert(operation);
        }
    }

    private Operation startCreateBindUserFLow(String accountId, BindUserCreateRequest request, Long stackId, Operation operation) {
        CreateBindUserEvent event = new CreateBindUserEvent(CREATE_BIND_USER_EVENT.event(), stackId, accountId, operation.getOperationId(),
                request.getBindUserNameSuffix(), request.getEnvironmentCrn());
        try {
            LOGGER.info("Start CreateBindUserFlow");
            flowManager.notify(CREATE_BIND_USER_EVENT.event(), event);
            LOGGER.info("Started CreateBindUserFlow");
            return operation;
        } catch (Exception e) {
            LOGGER.error("Couldn't start create bind user flow", e);
            return operationService.failOperation(accountId, operation.getOperationId(), "Couldn't start create bind user flow: " + e.getMessage());
        }
    }
}
