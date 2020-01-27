package com.sequenceiq.freeipa.service.operation;

import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.auth.security.InternalCrnBuilder;
import com.sequenceiq.freeipa.controller.exception.NotFoundException;
import com.sequenceiq.freeipa.entity.Operation;
import com.sequenceiq.freeipa.repository.OperationRepository;

@Service
public class OperationStatusService {
    private static final Logger LOGGER = LoggerFactory.getLogger(OperationStatusService.class);

    @Inject
    private OperationRepository operationRepository;

    public Operation getOperationForAccountIdAndOperationId(String actorCrn, String accountId, String operationId) {
        if (InternalCrnBuilder.isInternalCrn(actorCrn) || Crn.safeFromString(actorCrn).getAccountId().equals(accountId)) {
            Optional<Operation> operationOptional = operationRepository.findByOperationIdAndAccountId(operationId, accountId);
            if (!operationOptional.isPresent()) {
                LOGGER.info("Operation [{}] in account [{}] not found", operationId, accountId);
                throw NotFoundException.notFound("Operation", operationId).get();
            }
            return operationOptional.get();
        } else {
            LOGGER.warn("ActorCRN {} attempting to retrieve operation '{}' in different account '{}' ", actorCrn, operationId, accountId);
            throw NotFoundException.notFound("Operation", operationId).get();
        }
    }
}