package com.sequenceiq.freeipa.service.operation;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.freeipa.api.v1.freeipa.user.model.FailureDetails;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SuccessDetails;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SyncOperationStatus;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationState;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationStatus;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationType;
import com.sequenceiq.freeipa.controller.exception.NotFoundException;
import com.sequenceiq.freeipa.converter.freeipa.user.OperationToSyncOperationStatus;
import com.sequenceiq.freeipa.converter.operation.OperationToOperationStatusConverter;
import com.sequenceiq.freeipa.entity.Operation;
import com.sequenceiq.freeipa.repository.OperationRepository;
import com.sequenceiq.freeipa.service.freeipa.user.AcceptResult;

@Service
public class OperationStatusService {
    private static final Logger LOGGER = LoggerFactory.getLogger(OperationStatusService.class);

    @Inject
    private OperationRepository operationRepository;

    @Inject
    private OperationToSyncOperationStatus operationToSyncOperationStatus;

    @Inject
    private OperationToOperationStatusConverter operationToOperationStatusConverter;

    @Inject
    private List<OperationAcceptor> operationAcceptorList;

    private Map<OperationType, OperationAcceptor> operationAcceptorMap = new HashMap<>();

    @PostConstruct
    public void init() {
        operationAcceptorList.stream()
                .peek(a -> LOGGER.info("Registering acceptor for {}", a.selector()))
                .forEach(a -> operationAcceptorMap.put(a.selector(), a));

        for (OperationType operationType : OperationType.values()) {
            if (!operationAcceptorMap.containsKey(operationType)) {
                throw new IllegalStateException("No Acceptor injected for OperationType " + operationType);
            }
        }
    }

    public Operation startOperation(String accountId, OperationType operationType,
            Collection<String> environmentCrns, Collection<String> userCrns) {
        Operation operation = requestOperation(accountId, operationType, environmentCrns, userCrns);
        OperationAcceptor acceptor = operationAcceptorMap.get(operationType);

        AcceptResult acceptResult = acceptor.accept(operation);
        if (acceptResult.isAccepted()) {
            operation = acceptOperation(operation);
        } else {
            operation = rejectOperation(operation, acceptResult.getRejectionMessage().orElse("Rejected for unspecified reason."));
        }

        return operationRepository.save(operation);
    }

    public Operation completeOperation(String operationId, Collection<SuccessDetails> success, Collection<FailureDetails> failure) {
        Operation operation = getOperationForOperationId(operationId);
        operation.setStatus(OperationState.COMPLETED);
        operation.setSuccessList(List.copyOf(success));
        operation.setFailureList(List.copyOf(failure));
        operation.setEndTime(System.currentTimeMillis());
        return operationRepository.save(operation);
    }

    public Operation failOperation(String operationId, String failureMessage, Collection<SuccessDetails> success, Collection<FailureDetails> failure) {
        Operation operation = getOperationForOperationId(operationId);
        operation.setStatus(OperationState.FAILED);
        operation.setError(failureMessage);
        operation.setEndTime(System.currentTimeMillis());
        operation.setSuccessList(List.copyOf(success));
        operation.setFailureList(List.copyOf(failure));
        return operationRepository.save(operation);
    }

    public Operation failOperation(String operationId, String failureMessage) {
        return failOperation(operationId, failureMessage, Collections.emptyList(), Collections.emptyList());
    }

    public SyncOperationStatus getSyncOperationStatus(String operationId) {
        return operationToSyncOperationStatus.convert(getOperationForOperationId(operationId));
    }

    public OperationStatus getOperationStatus(String operationId, String accountId) {
        return operationToOperationStatusConverter.convert(getOperationForOperationId(operationId));
    }

    private Operation requestOperation(String accountId, OperationType operationType,
            Collection<String> environmentCrns, Collection<String> userCrns) {
        Operation operation = new Operation();
        operation.setOperationId(UUID.randomUUID().toString());
        operation.setStatus(OperationState.REQUESTED);
        operation.setAccountId(accountId);
        operation.setOperationType(operationType);
        operation.setEnvironmentList(List.copyOf(environmentCrns));
        operation.setUserList(List.copyOf(userCrns));
        return operationRepository.save(operation);
    }

    private Operation acceptOperation(Operation operation) {
        operation.setStatus(OperationState.RUNNING);
        return operation;
    }

    private Operation rejectOperation(Operation operation, String reason) {
        operation.setStatus(OperationState.REJECTED);
        operation.setEndTime(System.currentTimeMillis());
        operation.setError(reason);
        return operation;
    }

    private Operation getOperationForOperationId(String operationId) {
        Optional<Operation> syncOperationOptional = operationRepository.findByOperationId(operationId);
        if (!syncOperationOptional.isPresent()) {
            throw NotFoundException.notFound("Operation", "operationId").get();
        }
        return syncOperationOptional.get();
    }

    private Operation getOperationForOperationIdAndAccountId(String operationId, String accountId) {
        Optional<Operation> operationOptional = operationRepository.findByOperationIdAndAccountId(operationId, accountId);
        if (!operationOptional.isPresent()) {
            LOGGER.info("Operation [{}] in account [{}] not found", operationId, accountId);
            throw NotFoundException.notFound("Operation", "operationId").get();
        }
        return operationOptional.get();
    }
}