package com.sequenceiq.cloudbreak.service.freeipa;

import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.polling.SimpleStatusCheckerTask;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.FailureDetails;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationState;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationStatus;

public class FreeIpaOperationCheckerTask<T extends FreeIpaOperationPollerObject> extends SimpleStatusCheckerTask<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaOperationCheckerTask.class);

    @Override
    public boolean checkStatus(T operationPollerObject) {
        OperationStatus operationStatus = ThreadBasedUserCrnProvider.doAsInternalActor(
                operationPollerObject.getRegionalAwareInternalCrnGeneratorFactory().iam().getInternalCrnForServiceAsString(),
                () ->
                operationPollerObject.getOperationV1Endpoint().getOperationStatus(operationPollerObject.getOperationId(), operationPollerObject.getAccountId()));
        LOGGER.debug("OperationStatus for operationId[{}]: {}", operationPollerObject.getOperationId(), operationStatus);
        if (OperationState.COMPLETED.equals(operationStatus.getStatus())) {
            return true;
        } else if (OperationState.RUNNING.equals(operationStatus.getStatus())) {
            return false;
        } else {
            String failureDetails = extractFailureDetails(operationStatus);
            throw new FreeIpaOperationFailedException(String.format("FreeIPA [%s] operation [%s] failed with state [%s] and error message [%s]. "
                            + "Failure details: [%s]",
                    operationStatus.getOperationType(), operationPollerObject.getOperationId(), operationStatus.getStatus(), operationStatus.getError(),
                    failureDetails));
        }
    }

    private String extractFailureDetails(OperationStatus operationStatus) {
        String failureDetails = null;
        try {
            failureDetails = operationStatus.getFailure() != null && !operationStatus.getFailure().isEmpty() ?
                    operationStatus.getFailure().stream()
                            .map(FailureDetails::getAdditionalDetails)
                            .map(Object::toString)
                            .collect(Collectors.joining(","))
                    : "empty";
        } catch (Exception e) {
            LOGGER.error("Cannot evaluate Failure details [{}]", failureDetails, e);
            failureDetails = "empty";
        }
        return failureDetails;
    }

    @Override
    public void handleTimeout(T operationPollerObject) {
        OperationStatus operationStatus = ThreadBasedUserCrnProvider.doAsInternalActor(
                operationPollerObject.getRegionalAwareInternalCrnGeneratorFactory().iam().getInternalCrnForServiceAsString(),
                () ->
                operationPollerObject.getOperationV1Endpoint().getOperationStatus(operationPollerObject.getOperationId(), operationPollerObject.getAccountId()));
        throw new FreeIpaOperationFailedException(String.format("FreeIPA [%s] operation [%s] timed out. Current state is [%s]",
                operationStatus.getOperationType(), operationPollerObject.getOperationId(), operationStatus.getStatus()));
    }

    @Override
    public String successMessage(T operationPollerObject) {
        return String.format("FreeIPA [%s] operation [%s] finished successfully",
                operationPollerObject.getOperationType(), operationPollerObject.getOperationId());
    }

    @Override
    public boolean exitPolling(T operationPollerObject) {
        return false;
    }
}
