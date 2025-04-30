package com.sequenceiq.cloudbreak.service.freeipa;

import static com.sequenceiq.cloudbreak.rotation.common.RotationPollingSvcOutageUtils.pollWithSvcOutageErrorHandling;
import static java.lang.String.format;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import com.dyngr.Polling;
import com.dyngr.core.AttemptResult;
import com.dyngr.core.AttemptResults;
import com.dyngr.exception.PollerStoppedException;
import com.dyngr.exception.UserBreakException;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.common.SecretRotationException;
import com.sequenceiq.flow.api.model.FlowCheckResponse;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowLogResponse;
import com.sequenceiq.flow.api.model.FlowType;
import com.sequenceiq.flow.api.model.StateStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.AvailabilityStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.rotate.FreeIpaSecretRotationRequest;

@Service
public class FreeipaService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeipaService.class);

    private static final long SLEEP_INTERVAL = 10L;

    private static final long TIMEOUT_DURATION  = 20L;

    @Inject
    private FreeipaClientService freeipaClientService;

    @Retryable(value = CloudbreakServiceException.class, maxAttempts = 3, backoff = @Backoff(delay = 200))
    public boolean checkFreeipaRunning(String envCrn) {
        DescribeFreeIpaResponse freeipa = freeipaClientService.getByEnvironmentCrn(envCrn);
        if (freeipa == null || freeipa.getAvailabilityStatus() == null || freeipa.getAvailabilityStatus() == AvailabilityStatus.UNKNOWN) {
            String message = "Freeipa availability cannot be determined currently.";
            LOGGER.info(message);
            throw new CloudbreakServiceException(message);
        } else if (!freeipa.getAvailabilityStatus().isAvailable()) {
            String message = "Freeipa should be in Available state but currently is " + freeipa.getStatus().name();
            LOGGER.info(message);
            return false;
        } else {
            return true;
        }
    }

    @Recover
    public boolean recoverCheckFreeipaRunning(CloudbreakServiceException e, String envCrn) {
        String message = format("Freeipa availability trials exhausted for %s, defaulting to FreeIPA non-available", envCrn);
        LOGGER.warn(message, e);
        return false;
    }

    public void rotateFreeIpaSecret(String environmentCrn, SecretType secretType, RotationFlowExecutionType executionType,
            Map<String, String> additionalProperties) {
        LOGGER.info("Rotating FreeIpa secret: {} for environment {}", secretType, environmentCrn);
        FreeIpaSecretRotationRequest request = new FreeIpaSecretRotationRequest();
        request.setSecrets(List.of(secretType.value()));
        request.setExecutionType(executionType);
        request.setAdditionalProperties(additionalProperties);
        FlowIdentifier flowIdentifier = freeipaClientService.rotateSecret(environmentCrn, request);
        if (flowIdentifier == null) {
            handleUnsuccessfulFlow(environmentCrn, flowIdentifier, null);
        } else {
            pollWithSvcOutageErrorHandling(() -> pollUntilFlowFinished(environmentCrn, flowIdentifier), PollerStoppedException.class);
        }
    }

    public void preValidateFreeIpaSecretRotation(String environmentCrn) {
        if (StringUtils.isEmpty(environmentCrn)) {
            throw new SecretRotationException("No environment crn found, rotation is not possible.", null);
        }
        FlowLogResponse lastFlow = freeipaClientService.getLastFlowId(environmentCrn);
        if (lastFlow != null && lastFlow.getStateStatus() == StateStatus.PENDING) {
            String message = format("Polling in FreeIpa is not possible since last known state of flow for the FreeIpa is %s", lastFlow.getCurrentState());
            throw new SecretRotationException(message, null);
        }
    }

    private void pollUntilFlowFinished(String environmentCrn, FlowIdentifier flowIdentifier) {
        try {
            Boolean success = Polling.waitPeriodly(SLEEP_INTERVAL, TimeUnit.SECONDS)
                    .stopIfException(false)
                    .stopAfterDelay(TIMEOUT_DURATION, TimeUnit.MINUTES)
                    .run(() -> pollFlowState(flowIdentifier));
            if (success == null || !success) {
                String errorDescription;
                try {
                    DescribeFreeIpaResponse describeFreeIpaResponse = freeipaClientService.getByEnvironmentCrn(environmentCrn);
                    LOGGER.info("Response from FreeIpa: {}", describeFreeIpaResponse);
                    errorDescription = describeFreeIpaResponse.getStatusReason();
                } catch (CloudbreakServiceException | NotFoundException e) {
                    errorDescription = e.getMessage();
                    LOGGER.info("Error {} returned for FreeIpa describe request, environment crn: {}", errorDescription, environmentCrn);
                }
                handleUnsuccessfulFlow(environmentCrn, flowIdentifier, new UserBreakException(errorDescription));
            }
        } catch (UserBreakException e) {
            handleUnsuccessfulFlow(environmentCrn, flowIdentifier, e);
        }
    }

    private void handleUnsuccessfulFlow(String environmentCrn, FlowIdentifier flowIdentifier, UserBreakException e) {
        String message = format("FreeIpa flow failed with error: '%s'. Environment crn: %s, flow: %s",
                e != null ? e.getMessage() : "unknown", environmentCrn, flowIdentifier);
        LOGGER.warn(message);
        throw new CloudbreakServiceException(message, e);
    }

    private AttemptResult<Boolean> pollFlowState(FlowIdentifier flowIdentifier) {
        FlowCheckResponse flowState;
        if (flowIdentifier.getType() == FlowType.NOT_TRIGGERED) {
            return AttemptResults.breakFor(format("Flow %s not triggered", flowIdentifier.getPollableId()));
        } else if (flowIdentifier.getType() == FlowType.FLOW) {
            flowState = freeipaClientService.hasFlowRunningByFlowId(flowIdentifier.getPollableId());
        } else if (flowIdentifier.getType() == FlowType.FLOW_CHAIN) {
            flowState = freeipaClientService.hasFlowChainRunningByFlowChainId(flowIdentifier.getPollableId());
        } else {
            String message = format("Unknown flow identifier type %s for flow: %s", flowIdentifier.getType(), flowIdentifier);
            LOGGER.error(message);
            throw new CloudbreakServiceException(message);
        }

        LOGGER.debug("FreeIpa polling has active flow: {}, with latest fail: {}", flowState.getHasActiveFlow(), flowState.getLatestFlowFinalizedAndFailed());
        return flowState.getHasActiveFlow()
                ? AttemptResults.justContinue()
                : AttemptResults.finishWith(!flowState.getLatestFlowFinalizedAndFailed());
    }
}
