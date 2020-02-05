package com.sequenceiq.environment.environment.flow.deletion.handler.freeipa;

import static com.sequenceiq.cloudbreak.polling.PollingResult.isSuccess;
import static com.sequenceiq.cloudbreak.util.NullUtil.getIfNotNull;
import static com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteHandlerSelectors.DELETE_FREEIPA_EVENT;
import static com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteStateSelectors.START_RDBMS_DELETE_EVENT;
import static java.util.Objects.isNull;

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
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.deregchildenv.DeregisterChildEnvironmentRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;

import reactor.bus.Event;

@Component
public class FreeIpaDeletionHandler extends EventSenderAwareHandler<EnvironmentDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaDeletionHandler.class);

    private static final int SINGLE_FAILURE = 1;

    private final EnvironmentService environmentService;

    private final FreeIpaService freeIpaService;

    private final PollingService<FreeIpaPollerObject> freeIpaPollingService;

    protected FreeIpaDeletionHandler(EventSender eventSender, EnvironmentService environmentService, FreeIpaService freeIpaService,
            PollingService<FreeIpaPollerObject> freeIpaPollingService) {
        super(eventSender);
        this.environmentService = environmentService;
        this.freeIpaService = freeIpaService;
        this.freeIpaPollingService = freeIpaPollingService;
    }

    @Override
    public void accept(Event<EnvironmentDto> environmentDtoEvent) {
        EnvironmentDto environmentDto = environmentDtoEvent.getData();
        Optional<Environment> envOptional = environmentService.findEnvironmentById(environmentDto.getId());
        try {
            if (envOptional.isPresent() && freeIpaExistsForEnvironment(envOptional.get())) {
                Environment environment = envOptional.get();
                if (isNull(environment.getParentEnvironment())) {
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
                } else {
                    DeregisterChildEnvironmentRequest deregisterChildEnvironmentRequest = new DeregisterChildEnvironmentRequest();
                    deregisterChildEnvironmentRequest.setParentEnvironmentCrn(environment.getParentEnvironment().getResourceCrn());
                    deregisterChildEnvironmentRequest.setChildEnvironmentCrn(environment.getResourceCrn());
                    freeIpaService.deregisterChildEnvironment(deregisterChildEnvironmentRequest);
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

    private boolean freeIpaExistsForEnvironment(Environment env) {
        LOGGER.debug("About to call freeipa describe with env crn '{}'.", env.getResourceCrn());
        Optional<DescribeFreeIpaResponse> freeIpaResponse = freeIpaService.describe(env.getResourceCrn());
        if (freeIpaResponse.isEmpty()) {
            LOGGER.debug("Exception occurred during freeipa describe. Probably the resource does not exists, but worth a check.");
            return false;
        }
        return true;
    }
}
