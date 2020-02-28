package com.sequenceiq.environment.environment.flow.deletion.handler.freeipa;

import static com.sequenceiq.cloudbreak.polling.PollingResult.isSuccess;
import static com.sequenceiq.cloudbreak.util.NullUtil.getIfNotNull;
import static com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteHandlerSelectors.DELETE_FREEIPA_EVENT;
import static com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteStateSelectors.START_RDBMS_DELETE_EVENT;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.polling.PollingResult;
import com.sequenceiq.cloudbreak.polling.PollingService;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.creation.handler.freeipa.FreeIpaPollerObject;
import com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteEvent;
import com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteFailedEvent;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.environment.environment.service.freeipa.FreeIpaService;
import com.sequenceiq.environment.exception.FreeIpaOperationFailedException;
import com.sequenceiq.flow.reactor.api.event.EventSender;
import com.sequenceiq.flow.reactor.api.handler.EventSenderAwareHandler;
import com.sequenceiq.freeipa.api.v1.dns.DnsV1Endpoint;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.detachchildenv.DetachChildEnvironmentRequest;

import reactor.bus.Event;

@Component
public class FreeIpaDeletionHandler extends EventSenderAwareHandler<EnvironmentDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaDeletionHandler.class);

    private static final int SINGLE_FAILURE = 1;

    private final EnvironmentService environmentService;

    private final FreeIpaService freeIpaService;

    private final PollingService<FreeIpaPollerObject> freeIpaPollingService;

    private final DnsV1Endpoint dnsV1Endpoint;

    protected FreeIpaDeletionHandler(
            EventSender eventSender,
            EnvironmentService environmentService,
            FreeIpaService freeIpaService,
            PollingService<FreeIpaPollerObject> freeIpaPollingService,
            DnsV1Endpoint dnsV1Endpoint) {
        super(eventSender);
        this.environmentService = environmentService;
        this.freeIpaService = freeIpaService;
        this.freeIpaPollingService = freeIpaPollingService;
        this.dnsV1Endpoint = dnsV1Endpoint;
    }

    @Override
    public void accept(Event<EnvironmentDto> environmentDtoEvent) {
        EnvironmentDto environmentDto = environmentDtoEvent.getData();
        Environment environment = environmentService.findEnvironmentById(environmentDto.getId()).orElse(null);
        try {
            if (shouldRemoveFreeIpa(environment)) {
                if (Objects.nonNull(environment.getParentEnvironment())) {
                    detachChildEnvironmentFromFreeIpa(environment);
                } else {
                    deleteFreeIpa(environment);
                }
            }
            eventSender().sendEvent(getNextStepObject(environmentDto), environmentDtoEvent.getHeaders());
        } catch (Exception e) {
            EnvDeleteFailedEvent failedEvent = EnvDeleteFailedEvent.builder()
                    .withEnvironmentID(environmentDto.getId())
                    .withException(e)
                    .withResourceCrn(environmentDto.getResourceCrn())
                    .withResourceName(environmentDto.getName())
                    .build();
            eventSender().sendEvent(failedEvent, environmentDtoEvent.getHeaders());
        }
    }

    private boolean shouldRemoveFreeIpa(Environment environment) {
        return Objects.nonNull(environment)
                && (environment.isCreateFreeIpa() || Objects.nonNull(environment.getParentEnvironment()))
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
        return Objects.equals(sibling.getNetwork().getNetworkCidr(), environment.getNetwork().getNetworkCidr());
    }

    private void deleteFreeIpa(Environment environment) {
        freeIpaService.delete(environment.getResourceCrn());
        Pair<PollingResult, Exception> result = freeIpaPollingService.pollWithTimeout(
                new FreeIpaDeletionRetrievalTask(freeIpaService),
                new FreeIpaPollerObject(environment.getId(), environment.getResourceCrn()),
                FreeIpaDeletionRetrievalTask.FREEIPA_RETRYING_INTERVAL,
                FreeIpaDeletionRetrievalTask.FREEIPA_RETRYING_COUNT,
                SINGLE_FAILURE);
        if (!isSuccess(result.getLeft())) {
            throw new FreeIpaOperationFailedException("Failed to delete FreeIpa! " + getIfNotNull(result.getRight(), Throwable::getMessage));
        }
    }

    @Override
    public String selector() {
        return DELETE_FREEIPA_EVENT.selector();
    }

    private EnvDeleteEvent getNextStepObject(EnvironmentDto environmentDto) {
        return EnvDeleteEvent.builder()
                .withResourceId(environmentDto.getResourceId())
                .withResourceName(environmentDto.getName())
                .withResourceCrn(environmentDto.getResourceCrn())
                .withSelector(START_RDBMS_DELETE_EVENT.selector())
                .build();
    }
}
