package com.sequenceiq.environment.environment.flow.deletion.handler.freeipa;

import static com.sequenceiq.cloudbreak.util.NullUtil.getIfNotNull;
import static com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteHandlerSelectors.DELETE_FREEIPA_EVENT;
import static com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteStateSelectors.START_RDBMS_DELETE_EVENT;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import jakarta.ws.rs.NotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.polling.ExtendedPollingResult;
import com.sequenceiq.cloudbreak.polling.PollingService;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.dto.EnvironmentDeletionDto;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.creation.handler.freeipa.FreeIpaPollerObject;
import com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteEvent;
import com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteFailedEvent;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.environment.environment.service.freeipa.FreeIpaService;
import com.sequenceiq.environment.environment.service.recipe.EnvironmentRecipeService;
import com.sequenceiq.environment.exception.FreeIpaOperationFailedException;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowType;
import com.sequenceiq.flow.core.FlowConstants;
import com.sequenceiq.flow.core.FlowLogService;
import com.sequenceiq.flow.reactor.api.event.EventSender;
import com.sequenceiq.flow.reactor.api.handler.EventSenderAwareHandler;
import com.sequenceiq.freeipa.api.v1.dns.DnsV1Endpoint;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.detachchildenv.DetachChildEnvironmentRequest;

@Component
public class FreeIpaDeletionHandler extends EventSenderAwareHandler<EnvironmentDeletionDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaDeletionHandler.class);

    private final EnvironmentService environmentService;

    private final FreeIpaService freeIpaService;

    private final PollingService<FreeIpaPollerObject> freeIpaPollingService;

    private final DnsV1Endpoint dnsV1Endpoint;

    private final EnvironmentRecipeService environmentRecipeService;

    private final FlowLogService flowLogService;

    protected FreeIpaDeletionHandler(
            EventSender eventSender,
            EnvironmentService environmentService,
            FreeIpaService freeIpaService,
            PollingService<FreeIpaPollerObject> freeIpaPollingService,
            DnsV1Endpoint dnsV1Endpoint,
            EnvironmentRecipeService environmentRecipeService,
            FlowLogService flowLogService) {
        super(eventSender);
        this.environmentService = environmentService;
        this.freeIpaService = freeIpaService;
        this.freeIpaPollingService = freeIpaPollingService;
        this.dnsV1Endpoint = dnsV1Endpoint;
        this.environmentRecipeService = environmentRecipeService;
        this.flowLogService = flowLogService;
    }

    @Override
    public void accept(Event<EnvironmentDeletionDto> environmentDtoEvent) {
        LOGGER.debug("FreeIPA deletion flow step started.");
        EnvironmentDeletionDto environmentDeletionDto = environmentDtoEvent.getData();
        String flowId = environmentDtoEvent.getHeaders().get(FlowConstants.FLOW_ID);
        EnvironmentDto environmentDto = environmentDeletionDto.getEnvironmentDto();
        Environment environment = environmentService.findEnvironmentById(environmentDto.getId()).orElse(null);
        try {
            if (shouldRemoveFreeIpa(environment)) {
                environmentRecipeService.deleteRecipes(environmentDto.getId());
                if (Objects.nonNull(environment.getParentEnvironment())) {
                    detachChildEnvironmentFromFreeIpa(environment);
                } else {
                    deleteFreeIpa(environment, environmentDeletionDto.isForceDelete(), flowId, environmentDeletionDto.getResourceId());
                }
            }
            eventSender().sendEvent(getNextStepEvent(environmentDeletionDto), environmentDtoEvent.getHeaders());
        } catch (Exception e) {
            LOGGER.error("FreeIPA deletion failed", e);
            EnvDeleteFailedEvent failedEvent = EnvDeleteFailedEvent.builder()
                    .withEnvironmentId(environmentDto.getId())
                    .withException(e)
                    .withResourceCrn(environmentDto.getResourceCrn())
                    .withResourceName(environmentDto.getName())
                    .build();
            eventSender().sendEvent(failedEvent, environmentDtoEvent.getHeaders());
        }
        LOGGER.debug("FreeIPA deletion flow step completed.");
    }

    private boolean shouldRemoveFreeIpa(Environment environment) {
        return Objects.nonNull(environment)
                && freeIpaExistsForEnvironment(environment);
    }

    private boolean freeIpaExistsForEnvironment(Environment env) {
        LOGGER.debug("About to call freeipa describe with env crn '{}'.", env.getResourceCrn());
        Optional<DescribeFreeIpaResponse> freeIpaResponse = freeIpaService.describe(env.getResourceCrn());
        if (freeIpaResponse.isEmpty()) {
            LOGGER.debug("Exception occurred during freeipa describe. Probably the resource does not exists, but worth a check.");
            return false;
        }
        return true;
    }

    private void detachChildEnvironmentFromFreeIpa(Environment environment) {
        try {
            DetachChildEnvironmentRequest detachChildEnvironmentRequest = new DetachChildEnvironmentRequest();
            detachChildEnvironmentRequest.setParentEnvironmentCrn(environment.getParentEnvironment().getResourceCrn());
            detachChildEnvironmentRequest.setChildEnvironmentCrn(environment.getResourceCrn());
            freeIpaService.detachChildEnvironment(detachChildEnvironmentRequest);

            if (lastChildEnvironmentInNetworkIsGettingDeleted(environment)) {
                try {
                    dnsV1Endpoint.deleteDnsZoneBySubnet(environment.getParentEnvironment().getResourceCrn(), environment.getNetwork().getNetworkCidr());
                } catch (Exception e) {
                    LOGGER.warn("Failed to delete dns zone of child environment.", e);
                }
            }
        } catch (FreeIpaOperationFailedException e) {
            if (e.getCause() instanceof NotFoundException) {
                LOGGER.warn("Child FreeIpa is already detached.", e);
            } else {
                throw e;
            }
        }
    }

    private boolean lastChildEnvironmentInNetworkIsGettingDeleted(Environment environment) {
        List<Environment> siblingEnvironments = environmentService.findAllByAccountIdAndParentEnvIdAndArchivedIsFalse(
                environment.getAccountId(),
                environment.getParentEnvironment().getId());
        return siblingEnvironments.stream()
                .filter(sibling -> notTheSameEnvironment(environment, sibling))
                .noneMatch(sibling -> isInSameNetwork(environment, sibling));
    }

    private boolean notTheSameEnvironment(Environment environment, Environment sibling) {
        return !Objects.equals(sibling.getId(), environment.getId());
    }

    private boolean isInSameNetwork(Environment environment, Environment sibling) {
        if (environment.getNetwork() != null && sibling.getNetwork() != null) {
            return Objects.equals(sibling.getNetwork().getNetworkCidr(), environment.getNetwork().getNetworkCidr());
        } else {
            return false;
        }
    }

    private void deleteFreeIpa(Environment environment, boolean forced, String flowId, Long resourceId) {
        freeIpaService.delete(environment.getResourceCrn(), forced);
        FlowIdentifier flowIdentifier = flowId != null ? new FlowIdentifier(FlowType.FLOW, flowId) : null;
        ExtendedPollingResult result = freeIpaPollingService.pollWithTimeout(
                new FreeIpaDeletionRetrievalTask(freeIpaService, flowLogService),
                new FreeIpaPollerObject(environment.getId(), environment.getResourceCrn(), flowIdentifier, resourceId),
                FreeIpaDeletionRetrievalTask.FREEIPA_RETRYING_INTERVAL,
                FreeIpaDeletionRetrievalTask.FREEIPA_RETRYING_COUNT,
                FreeIpaDeletionRetrievalTask.FREEIPA_FAILURE_COUNT);
        if (!result.isSuccess()) {
            String message = "Failed to delete FreeIpa! (" + result.getPollingResult().name() + ") "
                    + getIfNotNull(result.getException(), Throwable::getMessage);
            LOGGER.info(message);
            throw new FreeIpaOperationFailedException(message);
        }
    }

    @Override
    public String selector() {
        return DELETE_FREEIPA_EVENT.selector();
    }

    private EnvDeleteEvent getNextStepEvent(EnvironmentDeletionDto environmentDeletionDto) {
        EnvironmentDto environmentDto = environmentDeletionDto.getEnvironmentDto();
        return EnvDeleteEvent.builder()
                .withForceDelete(environmentDeletionDto.isForceDelete())
                .withResourceId(environmentDto.getResourceId())
                .withResourceName(environmentDto.getName())
                .withResourceCrn(environmentDto.getResourceCrn())
                .withSelector(START_RDBMS_DELETE_EVENT.selector())
                .build();
    }

}
