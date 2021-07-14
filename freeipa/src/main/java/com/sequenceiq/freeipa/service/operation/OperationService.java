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

import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.FailureDetails;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SuccessDetails;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationState;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationType;
import com.sequenceiq.freeipa.entity.Operation;
import com.sequenceiq.freeipa.repository.OperationRepository;
import com.sequenceiq.freeipa.service.freeipa.user.AcceptResult;

@Service
public class OperationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(OperationService.class);

    @Inject
    private OperationRepository operationRepository;

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
        Operation requestedOperation = requestOperation(accountId, operationType, environmentCrns, userCrns);
        OperationAcceptor acceptor = operationAcceptorMap.get(operationType);
        AcceptResult acceptResult = acceptor.accept(requestedOperation);
        if (acceptResult.isAccepted()) {
            return acceptOperation(requestedOperation);
        } else {
            return rejectOperation(requestedOperation, acceptResult.getRejectionMessage().orElse("Rejected for unspecified reason."));
        }
    }

    public Operation completeOperation(String accountId, String operationId, Collection<SuccessDetails> success, Collection<FailureDetails> failure) {
        Operation operation = getOperationForAccountIdAndOperationId(accountId, operationId);
        operation.setStatus(OperationState.COMPLETED);
        operation.setSuccessList(List.copyOf(success));
        operation.setFailureList(List.copyOf(failure));
        operation.setEndTime(System.currentTimeMillis());
        LOGGER.info("Operation completed: {}. Operation duration was {} ms.", operation, operation.getEndTime() - operation.getStartTime());
        return operationRepository.save(operation);
    }

    public Operation failOperation(String accountId, String operationId, String failureMessage, Collection<SuccessDetails> success,
            Collection<FailureDetails> failure) {
        Operation operation = getOperationForAccountIdAndOperationId(accountId, operationId);
        operation.setStatus(OperationState.FAILED);
        operation.setError(failureMessage);
        operation.setEndTime(System.currentTimeMillis());
        operation.setSuccessList(List.copyOf(success));
        operation.setFailureList(List.copyOf(failure));
        LOGGER.warn("Operation failed: {}. Operation duration was {} ms.", operation, operation.getEndTime() - operation.getStartTime());
        return operationRepository.save(operation);
    }

    public Operation failOperation(String accountId, String operationId, String failureMessage) {
        return failOperation(accountId, operationId, failureMessage, Collections.emptyList(), Collections.emptyList());
    }

    public Operation getOperationForAccountIdAndOperationId(String accountId, String operationId) {
        Optional<Operation> operationOptional = operationRepository.findByOperationIdAndAccountId(operationId, accountId);
        if (!operationOptional.isPresent()) {
            LOGGER.info("Operation [{}] in account [{}] not found", operationId, accountId);
            throw NotFoundException.notFound("Operation", operationId).get();
        } else {
            Operation operation = operationOptional.get();
            LOGGER.debug("Operation found: {}", operation);
            return operation;
        }
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
        LOGGER.info("Operation requested: {}", operation);
        return operationRepository.save(operation);
    }

    private Operation acceptOperation(Operation operation) {
        operation.setStatus(OperationState.RUNNING);
        LOGGER.info("Operation accepted: {}", operation);
        return operationRepository.save(operation);
    }

    private Operation rejectOperation(Operation operation, String reason) {
        operation.setStatus(OperationState.REJECTED);
        operation.setEndTime(System.currentTimeMillis());
        operation.setError(reason);
        LOGGER.warn("Operation rejected: {}. Operation duration was {} ms.", operation, operation.getEndTime() - operation.getStartTime());
        return operationRepository.save(operation);
    }
}