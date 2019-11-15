package com.sequenceiq.environment.environment.flow.deletion.handler.freeipa;

import static com.sequenceiq.cloudbreak.polling.PollingResult.isSuccess;
import static com.sequenceiq.cloudbreak.util.NullUtil.getIfNotNull;
import static com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteHandlerSelectors.DELETE_FREEIPA_EVENT;
import static com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteStateSelectors.START_RDBMS_DELETE_EVENT;

import java.util.Optional;

import javax.ws.rs.NotFoundException;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionMessageExtractor;
import com.sequenceiq.cloudbreak.polling.PollingResult;
import com.sequenceiq.cloudbreak.polling.PollingService;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.creation.handler.freeipa.FreeIpaPollerObject;
import com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteEvent;
import com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteFailedEvent;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.environment.exception.FreeIpaOperationFailedException;
import com.sequenceiq.flow.reactor.api.event.EventSender;
import com.sequenceiq.flow.reactor.api.handler.EventSenderAwareHandler;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.FreeIpaV1Endpoint;

import reactor.bus.Event;

@Component
public class FreeIpaDeleteHandler extends EventSenderAwareHandler<EnvironmentDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaDeleteHandler.class);

    private static final int SINGLE_FAILURE = 1;

    private final EnvironmentService environmentService;

    private final FreeIpaV1Endpoint freeIpaV1Endpoint;

    private final PollingService<FreeIpaPollerObject> freeIpaPollingService;

    private final WebApplicationExceptionMessageExtractor webApplicationExceptionMessageExtractor;

    protected FreeIpaDeleteHandler(EventSender eventSender, EnvironmentService environmentService, FreeIpaV1Endpoint freeIpaV1Endpoint,
            PollingService<FreeIpaPollerObject> freeIpaPollingService, WebApplicationExceptionMessageExtractor webApplicationExceptionMessageExtractor) {
        super(eventSender);
        this.environmentService = environmentService;
        this.freeIpaV1Endpoint = freeIpaV1Endpoint;
        this.freeIpaPollingService = freeIpaPollingService;
        this.webApplicationExceptionMessageExtractor = webApplicationExceptionMessageExtractor;
    }

    @Override
    public void accept(Event<EnvironmentDto> environmentDtoEvent) {
        EnvironmentDto environmentDto = environmentDtoEvent.getData();
        Optional<Environment> env = environmentService.findEnvironmentById(environmentDto.getId());
        try {
            if (env.isPresent() && isFreeIpaExistsForEnvironment(env.get())) {
                freeIpaV1Endpoint.delete(env.get().getResourceCrn());
                Pair<PollingResult, Exception> result = freeIpaPollingService.pollWithTimeout(
                        new FreeIpaDeleteRetrievalTask(webApplicationExceptionMessageExtractor),
                        new FreeIpaPollerObject(env.get().getId(), env.get().getResourceCrn(), freeIpaV1Endpoint),
                        FreeIpaDeleteRetrievalTask.FREEIPA_RETRYING_INTERVAL,
                        FreeIpaDeleteRetrievalTask.FREEIPA_RETRYING_COUNT,
                        SINGLE_FAILURE);
                if (isSuccess(result.getLeft())) {
                    eventSender().sendEvent(getNextStepObject(environmentDto), environmentDtoEvent.getHeaders());
                } else {
                    throw new FreeIpaOperationFailedException("Failed to delete FreeIpa! " + getIfNotNull(result.getRight(), Throwable::getMessage));
                }
            } else {
                eventSender().sendEvent(getNextStepObject(environmentDto), environmentDtoEvent.getHeaders());
            }
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

    private boolean isFreeIpaExistsForEnvironment(Environment env) {
        try {
            LOGGER.debug("About to call freeipa describe with env crn '{}'.", env.getResourceCrn());
            freeIpaV1Endpoint.describe(env.getResourceCrn());
            return true;
        } catch (NotFoundException probablyResourceDoesNotExists) {
            LOGGER.debug("Exception occurred during freeipa describe. Probably the resource does not exists, but worth a check", probablyResourceDoesNotExists);
            return false;
        }
    }

}
