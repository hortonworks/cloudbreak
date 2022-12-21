package com.sequenceiq.environment.environment.service.freeipa;

import java.util.Optional;
import java.util.Set;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.common.exception.ExceptionResponse;
import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionMessageExtractor;
import com.sequenceiq.environment.events.EventSenderService;
import com.sequenceiq.environment.exception.FreeIpaOperationFailedException;
import com.sequenceiq.flow.api.model.FlowCheckResponse;
import com.sequenceiq.flow.api.model.FlowLogResponse;
import com.sequenceiq.flow.api.model.operation.OperationView;
import com.sequenceiq.freeipa.api.v1.freeipa.flow.FreeIpaV1FlowEndpoint;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.FreeIpaV1Endpoint;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.attachchildenv.AttachChildEnvironmentRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.create.CreateFreeIpaRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.detachchildenv.DetachChildEnvironmentRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.health.HealthDetailsFreeIpaResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.scale.VerticalScaleRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.scale.VerticalScaleResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.user.UserV1Endpoint;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SyncOperationStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SynchronizationStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SynchronizeAllUsersRequest;
import com.sequenceiq.freeipa.api.v1.operation.OperationV1Endpoint;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationStatus;

@Service
public class FreeIpaService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaService.class);

    private final FreeIpaV1Endpoint freeIpaV1Endpoint;

    private final OperationV1Endpoint operationV1Endpoint;

    private final UserV1Endpoint userV1Endpoint;

    private final WebApplicationExceptionMessageExtractor webApplicationExceptionMessageExtractor;

    private final RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    private final EventSenderService eventService;

    private final FreeIpaV1FlowEndpoint flowEndpoint;

    public FreeIpaService(FreeIpaV1Endpoint freeIpaV1Endpoint,
            OperationV1Endpoint operationV1Endpoint,
            FreeIpaV1FlowEndpoint flowEndpoint,
            UserV1Endpoint userV1Endpoint,
            WebApplicationExceptionMessageExtractor webApplicationExceptionMessageExtractor,
            RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory,
            EventSenderService eventService) {
        this.freeIpaV1Endpoint = freeIpaV1Endpoint;
        this.operationV1Endpoint = operationV1Endpoint;
        this.userV1Endpoint = userV1Endpoint;
        this.webApplicationExceptionMessageExtractor = webApplicationExceptionMessageExtractor;
        this.regionAwareInternalCrnGeneratorFactory = regionAwareInternalCrnGeneratorFactory;
        this.eventService = eventService;
        this.flowEndpoint = flowEndpoint;
    }

    public DescribeFreeIpaResponse create(CreateFreeIpaRequest createFreeIpaRequest) {
        try {
            return freeIpaV1Endpoint.create(createFreeIpaRequest);
        } catch (WebApplicationException e) {
            String errorMessage = webApplicationExceptionMessageExtractor.getErrorMessage(e);
            LOGGER.error(String.format("Failed to create FreeIpa cluster for environment '%s' due to: '%s'",
                    createFreeIpaRequest.getEnvironmentCrn(), errorMessage), e);
            throw new FreeIpaOperationFailedException(errorMessage, e);
        }
    }

    public Optional<DescribeFreeIpaResponse> describe(String envCrn) {
        try {
            return Optional.of(freeIpaV1Endpoint.describe(envCrn));
        } catch (NotFoundException e) {
            LOGGER.warn("Could not find freeipa with envCrn: " + envCrn);
            return Optional.empty();
        } catch (WebApplicationException e) {
            String errorMessage = webApplicationExceptionMessageExtractor.getErrorMessage(e);
            LOGGER.error(String.format("Failed to describe FreeIpa cluster for environment '%s' due to: '%s'.", envCrn, errorMessage), e);
            throw new FreeIpaOperationFailedException(errorMessage, e);
        }
    }

    public Optional<DescribeFreeIpaResponse> internalDescribe(String envCrn, String accountId) {
        try {
            return Optional.of(freeIpaV1Endpoint.describeInternal(envCrn, accountId));
        } catch (NotFoundException e) {
            try {
                ExceptionResponse exceptionResponse = e.getResponse().readEntity(ExceptionResponse.class);
                LOGGER.info("Freeipa not found, reason: {}", exceptionResponse.getMessage());
                return Optional.empty();
            } catch (RuntimeException convertException) {
                LOGGER.info("Conversion failed", convertException);
                throw new FreeIpaOperationFailedException("Freeipa internal describe response is NOT FOUND, but response reason is not the expected type", e);
            }
        } catch (WebApplicationException e) {
            String errorMessage = webApplicationExceptionMessageExtractor.getErrorMessage(e);
            LOGGER.error(String.format("Failed to describe FreeIpa cluster for environment '%s' due to: '%s'.", envCrn, errorMessage), e);
            throw new FreeIpaOperationFailedException(errorMessage, e);
        }
    }

    public void delete(String environmentCrn, boolean forced) {
        try {
            freeIpaV1Endpoint.delete(environmentCrn, forced);
        } catch (WebApplicationException e) {
            String errorMessage = webApplicationExceptionMessageExtractor.getErrorMessage(e);
            LOGGER.error(String.format("Failed to delete FreeIpa cluster for environment '%s' due to: '%s'", environmentCrn, errorMessage), e);
            throw new FreeIpaOperationFailedException(errorMessage, e);
        }
    }

    public void attachChildEnvironment(AttachChildEnvironmentRequest attachChildEnvironmentRequest) {
        try {
            freeIpaV1Endpoint.attachChildEnvironment(attachChildEnvironmentRequest);
        } catch (WebApplicationException e) {
            String errorMessage = webApplicationExceptionMessageExtractor.getErrorMessage(e);
            LOGGER.error(String.format("Failed to attach child environment '%s' for '%s' due to: '%s'",
                    attachChildEnvironmentRequest.getChildEnvironmentCrn(), attachChildEnvironmentRequest.getParentEnvironmentCrn(), errorMessage), e);
            throw new FreeIpaOperationFailedException(errorMessage, e);
        }
    }

    public void detachChildEnvironment(DetachChildEnvironmentRequest detachChildEnvironmentRequest) {
        try {
            freeIpaV1Endpoint.detachChildEnvironment(detachChildEnvironmentRequest);
        } catch (WebApplicationException e) {
            String errorMessage = webApplicationExceptionMessageExtractor.getErrorMessage(e);
            LOGGER.error(String.format("Failed to detach child environment '%s' due to: '%s'",
                    detachChildEnvironmentRequest.getChildEnvironmentCrn(), errorMessage), e);
            throw new FreeIpaOperationFailedException(errorMessage, e);
        }
    }

    public SyncOperationStatus getSyncOperationStatus(String environmentCrn, String operationId) {
        try {
            return ThreadBasedUserCrnProvider.doAsInternalActor(
                    regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                    () -> userV1Endpoint.getSyncOperationStatusInternal(Crn.fromString(environmentCrn).getAccountId(), operationId));
        } catch (WebApplicationException e) {
            String errorMessage = webApplicationExceptionMessageExtractor.getErrorMessage(e);
            LOGGER.error(String.format("Failed to get user synchronization status from FreeIpa for environment '%s' due to: '%s'",
                    environmentCrn, errorMessage), e);
            throw new FreeIpaOperationFailedException(errorMessage, e);
        }
    }

    public HealthDetailsFreeIpaResponse getHealthDetails(String environmentCrn) {
        try {
            return freeIpaV1Endpoint.healthDetails(environmentCrn);
        } catch (WebApplicationException e) {
            String errorMessage = webApplicationExceptionMessageExtractor.getErrorMessage(e);
            LOGGER.error(String.format("Failed to get health details for the freeIpa of the given environment: '%s' due to: '%s'",
                    environmentCrn, errorMessage), e);
            throw new FreeIpaOperationFailedException(errorMessage, e);
        }
    }

    public Optional<OperationView> getFreeIpaOperation(String environmentCrn, boolean detailed) {
        try {
            return Optional.ofNullable(operationV1Endpoint.getOperationProgressByEnvironmentCrn(environmentCrn, detailed));
        } catch (WebApplicationException e) {
            return Optional.empty();
        }
    }

    public OperationStatus getOperationStatus(String operationId) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        try {
            LOGGER.debug("Getting FreeIPA Operation status for operation {}", operationId);
            return operationV1Endpoint.getOperationStatus(operationId, accountId);
        } catch (WebApplicationException e) {
            String errorMessage = webApplicationExceptionMessageExtractor.getErrorMessage(e);
            LOGGER.error("Failed to get operation status '{}' in account {} due to: '{}'", operationId, accountId, errorMessage, e);
            throw new FreeIpaOperationFailedException(errorMessage, e);
        }
    }

    public FlowLogResponse getFlowStatus(String flowId) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        try {
            LOGGER.debug("Getting FreeIPA Operation status for flowId {}", flowId);
            return flowEndpoint.getLastFlowById(flowId);
        } catch (WebApplicationException e) {
            String errorMessage = webApplicationExceptionMessageExtractor.getErrorMessage(e);
            LOGGER.error("Failed to get operation status '{}' in account {} due to: '{}'", flowId, accountId, errorMessage, e);
            throw new FreeIpaOperationFailedException(errorMessage, e);
        }
    }

    public FlowCheckResponse getFlowCheckStatus(String flowId) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        try {
            LOGGER.debug("Getting FreeIPA Operation status for flowId {}", flowId);
            return flowEndpoint.hasFlowRunningByFlowId(flowId);
        } catch (WebApplicationException e) {
            String errorMessage = webApplicationExceptionMessageExtractor.getErrorMessage(e);
            LOGGER.error("Failed to get operation status '{}' in account {} due to: '{}'", flowId, accountId, errorMessage, e);
            throw new FreeIpaOperationFailedException(errorMessage, e);
        }
    }

    public OperationStatus upgradeCcm(String environmentCrn) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        try {
            LOGGER.debug("Calling FreeIPA CCM upgrade for environment {}", environmentCrn);
            return ThreadBasedUserCrnProvider.doAsInternalActor(
                    regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                    () -> freeIpaV1Endpoint.upgradeCcmInternal(environmentCrn, userCrn));
        } catch (WebApplicationException e) {
            String errorMessage = webApplicationExceptionMessageExtractor.getErrorMessage(e);
            LOGGER.error("Failed to upgrade CCM on FreeIpa for environment {} due to: {}", environmentCrn, errorMessage, e);
            throw new FreeIpaOperationFailedException(errorMessage, e);
        }
    }

    public OperationStatus modifyProxyConfig(String environmentCrn) {
        try {
            LOGGER.debug("Calling FreeIPA modify proxy config for environment {}", environmentCrn);
            return ThreadBasedUserCrnProvider.doAsInternalActor(
                    regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                    initiatorUserCrn -> freeIpaV1Endpoint.modifyProxyConfigInternal(environmentCrn, initiatorUserCrn));
        } catch (WebApplicationException e) {
            String errorMessage = webApplicationExceptionMessageExtractor.getErrorMessage(e);
            LOGGER.error("Failed to modify proxy config on FreeIpa for environment {} due to: {}", environmentCrn, errorMessage, e);
            throw new FreeIpaOperationFailedException(errorMessage, e);
        }
    }

    public VerticalScaleResponse verticalScale(String environmentCrn, VerticalScaleRequest freeIPAVerticalScaleRequest) {
        try {
            LOGGER.debug("Calling FreeIPA CCM upgrade for environment {}", environmentCrn);
            return ThreadBasedUserCrnProvider.doAsInternalActor(
                    regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                    () -> freeIpaV1Endpoint.verticalScalingByCrn(environmentCrn, freeIPAVerticalScaleRequest));
        } catch (WebApplicationException e) {
            String errorMessage = webApplicationExceptionMessageExtractor.getErrorMessage(e);
            LOGGER.error("Failed to vertical scale on FreeIpa for environment {} due to: {}", environmentCrn, errorMessage, e);
            throw new FreeIpaOperationFailedException(errorMessage, e);
        }
    }

    SyncOperationStatus synchronizeAllUsersInEnvironment(String environmentCrn) {
        try {
            SyncOperationStatus lastSyncOperationStatus = userV1Endpoint.getLastSyncOperationStatus(environmentCrn);
            if (!SynchronizationStatus.RUNNING.equals(lastSyncOperationStatus.getStatus())) {
                SynchronizeAllUsersRequest request = new SynchronizeAllUsersRequest(Set.of(environmentCrn), Set.of());
                return userV1Endpoint.synchronizeAllUsers(request);
            } else {
                LOGGER.debug("There is already an ongoing user sync operation for environment {} with operationId {}, " +
                        "thus there is no need to trigger another one.", environmentCrn, lastSyncOperationStatus.getOperationId());
                return lastSyncOperationStatus;
            }
        } catch (WebApplicationException e) {
            String errorMessage = webApplicationExceptionMessageExtractor.getErrorMessage(e);
            LOGGER.error(String.format("Failed to synchronize users with FreeIpa for environment '%s' due to: '%s'", environmentCrn, errorMessage), e);
            throw new FreeIpaOperationFailedException(errorMessage, e);
        }
    }

    void startFreeIpa(String environmentCrn) {
        try {
            LOGGER.info("Starting freeipa");
            freeIpaV1Endpoint.start(environmentCrn);
        } catch (WebApplicationException e) {
            String errorMessage = webApplicationExceptionMessageExtractor.getErrorMessage(e);
            LOGGER.error(String.format("Failed to start FreeIpa cluster for environment '%s' due to: '%s'", environmentCrn, errorMessage), e);
            throw new FreeIpaOperationFailedException(errorMessage, e);
        }
    }

    void stopFreeIpa(String environmentCrn) {
        try {
            LOGGER.info("Stopping freeipa");
            freeIpaV1Endpoint.stop(environmentCrn);
        } catch (WebApplicationException e) {
            String errorMessage = webApplicationExceptionMessageExtractor.getErrorMessage(e);
            LOGGER.error(String.format("Failed to stop FreeIpa cluster for environment '%s' due to: '%s'", environmentCrn, errorMessage), e);
            throw new FreeIpaOperationFailedException(errorMessage, e);
        }
    }
}
