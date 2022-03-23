package com.sequenceiq.cloudbreak.core.flow2.validate.kerberosconfig.handler;

import static com.sequenceiq.cloudbreak.core.flow2.validate.kerberosconfig.config.KerberosConfigValidationEvent.BIND_USER_CREATION_STARTED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.validate.kerberosconfig.config.KerberosConfigValidationEvent.VALIDATE_KERBEROS_CONFIG_FAILED_EVENT;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.core.flow2.validate.kerberosconfig.event.PollBindUserCreationEvent;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.retry.RetryException;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.FreeIpaV1Endpoint;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.binduser.BindUserCreateRequest;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationState;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationStatus;

@Service
public class StartBindUserCreationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StartBindUserCreationService.class);

    @Inject
    private FreeIpaV1Endpoint freeIpaV1Endpoint;

    @Inject
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    /**
     * In very rare cases it's possible the Operation will be rejected because there is another with the same parameters is running.
     * As it's not possible to get the conflicting operation ID right now, we wait for it to finish and start a new as the result should be the same.
     * The retry is necessary to avoid provision failures which could be handled with pushing a the retry button.
     */
    @Retryable(value = RetryException.class, maxAttempts = 6, backoff = @Backoff(delay = 10000))
    public StackEvent startBindUserCreation(StackView stackView) throws RetryException {
        OperationStatus operationStatus = invokeCreateBindUser(stackView);
        return handleOperationStatus(stackView, operationStatus);
    }

    private StackEvent handleOperationStatus(StackView stackView, OperationStatus operationStatus) throws RetryException {
        if (OperationState.FAILED == operationStatus.getStatus()) {
            LOGGER.error("Failed to start bind user creation operation: {}", operationStatus);
            return new StackFailureEvent(VALIDATE_KERBEROS_CONFIG_FAILED_EVENT.event(), stackView.getId(),
                    new Exception("Failed to start bind user creation operation: " + operationStatus.getError()));
        } else if (OperationState.REJECTED == operationStatus.getStatus()) {
            LOGGER.error("Bind user creation operation rejected: {}", operationStatus);
            throw new RetryException("Bind user creation operation rejected with: " + operationStatus.getError());
        } else {
            LOGGER.info("Bind user creation operation started: {}", operationStatus);
            return new PollBindUserCreationEvent(BIND_USER_CREATION_STARTED_EVENT.event(), stackView.getId(), operationStatus.getOperationId(),
                    Crn.safeFromString(stackView.getResourceCrn()).getAccountId());
        }
    }

    private OperationStatus invokeCreateBindUser(StackView stackView) {
        BindUserCreateRequest request = createRequest(stackView);
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        return ThreadBasedUserCrnProvider.doAsInternalActor(
                regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                () -> freeIpaV1Endpoint.createBindUser(request, userCrn));
    }

    private BindUserCreateRequest createRequest(StackView stackView) {
        BindUserCreateRequest request = new BindUserCreateRequest();
        request.setEnvironmentCrn(stackView.getEnvironmentCrn());
        request.setBindUserNameSuffix(stackView.getName());
        return request;
    }
}
