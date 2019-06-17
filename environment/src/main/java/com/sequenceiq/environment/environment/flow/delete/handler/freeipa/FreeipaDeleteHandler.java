package com.sequenceiq.environment.environment.flow.delete.handler.freeipa;

import static com.sequenceiq.cloudbreak.polling.PollingResult.isSuccess;
import static com.sequenceiq.cloudbreak.util.NullUtil.getIfNotNull;
import static com.sequenceiq.environment.environment.flow.delete.event.EnvDeleteHandlerSelectors.DELETE_FREEIPA_EVENT;
import static com.sequenceiq.environment.environment.flow.delete.event.EnvDeleteStateSelectors.START_RDBMS_DELETE_EVENT;

import java.util.Optional;

import javax.ws.rs.NotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.polling.PollingResult;
import com.sequenceiq.cloudbreak.polling.PollingService;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.creation.handler.freeipa.FreeIpaPollerObject;
import com.sequenceiq.environment.environment.flow.delete.event.EnvDeleteEvent;
import com.sequenceiq.environment.environment.flow.delete.event.EnvDeleteFailedEvent;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.environment.exception.FreeIpaOperationFailedException;
import com.sequenceiq.flow.reactor.api.event.EventSender;
import com.sequenceiq.flow.reactor.api.handler.EventSenderAwareHandler;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.FreeIpaV1Endpoint;

import reactor.bus.Event;

@Component
public class FreeipaDeleteHandler extends EventSenderAwareHandler<EnvironmentDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeipaDeleteHandler.class);

    private final EnvironmentService environmentService;

    private final FreeIpaV1Endpoint freeIpaV1Endpoint;

    private final PollingService<FreeIpaPollerObject> freeIpaPollingService;

    protected FreeipaDeleteHandler(EventSender eventSender, EnvironmentService environmentService, FreeIpaV1Endpoint freeIpaV1Endpoint,
            PollingService<FreeIpaPollerObject> freeIpaPollingService) {
        super(eventSender);
        this.environmentService = environmentService;
        this.freeIpaV1Endpoint = freeIpaV1Endpoint;
        this.freeIpaPollingService = freeIpaPollingService;
    }

    @Override
    public void accept(Event<EnvironmentDto> environmentDtoEvent) {
        EnvironmentDto environmentDto = environmentDtoEvent.getData();
        Optional<EnvironmentDto> env = environmentService.findById(environmentDto.getId());
        try {
            if (env.isPresent() && isFreeIpaExistsForEnvironment(env.get())) {
                env.get().setStatus(EnvironmentStatus.FREEIPA_DELETE_IN_PROGRESS);
                environmentService.save(env.get());

                freeIpaV1Endpoint.delete(env.get().getResourceCrn());

                PollingResult result = freeIpaPollingService.pollWithTimeoutSingleFailure(
                        new FreeIpaDeleteRetrievalTask(),
                        new FreeIpaPollerObject(env.get().getResourceCrn(), freeIpaV1Endpoint),
                        FreeIpaDeleteRetrievalTask.FREEIPA_RETRYING_INTERVAL,
                        FreeIpaDeleteRetrievalTask.FREEIPA_RETRYING_COUNT);
                if (isSuccess(result)) {
                    eventSender().sendEvent(getNextStepObject(environmentDto), environmentDtoEvent.getHeaders());
                } else {
                    throw new FreeIpaOperationFailedException("Failed to delete FreeIpa! Result was " + getIfNotNull(result, Enum::name));
                }
            } else {
                eventSender().sendEvent(getNextStepObject(environmentDto), environmentDtoEvent.getHeaders());
            }
        } catch (Exception ex) {
            EnvDeleteFailedEvent failedEvent = new EnvDeleteFailedEvent(environmentDto.getId(), environmentDto.getName(), ex);
            eventSender().sendEvent(failedEvent, environmentDtoEvent.getHeaders());
        }
    }

    @Override
    public String selector() {
        return DELETE_FREEIPA_EVENT.selector();
    }

    private EnvDeleteEvent getNextStepObject(EnvironmentDto environmentDto) {
        return EnvDeleteEvent.EnvDeleteEventBuilder.anEnvDeleteEvent()
                .withResourceId(environmentDto.getResourceId())
                .withSelector(START_RDBMS_DELETE_EVENT.selector())
                .build();
    }

    private boolean isFreeIpaExistsForEnvironment(EnvironmentDto env) {
        try {
            LOGGER.debug("About to call freeipa describe with ");
            freeIpaV1Endpoint.describe(env.getResourceCrn());
            return true;
        } catch (NotFoundException probablyResourseDoesNotExists) {
            LOGGER.debug("Exception occured during freeipa describe. Probably the resource does not exists, but worth to check", probablyResourseDoesNotExists);
            return false;
        }
    }

}
