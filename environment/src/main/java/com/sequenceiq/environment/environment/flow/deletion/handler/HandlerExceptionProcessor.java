package com.sequenceiq.environment.environment.flow.deletion.handler;

import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteFailedEvent;
import com.sequenceiq.flow.reactor.api.event.EventSender;

@Component
public class HandlerExceptionProcessor {

    public void handle(HandlerFailureConjoiner conjoiner, Logger loggerFromCaller, EventSender eventSender, String selector) {
        if (conjoiner.getEnvironmentDeletionDto().isForceDelete()) {
            loggerFromCaller.warn("The {} was not successful but the environment deletion was requested as force delete so " +
                    "continue the deletion flow", selector);
            eventSender.sendEvent(conjoiner.getEnvDeleteEvent(), conjoiner.getEnvironmentDtoEvent().getHeaders());
        } else {
            EnvDeleteFailedEvent failedEvent = EnvDeleteFailedEvent.builder()
                    .withEnvironmentID(conjoiner.getEnvironmentDto().getId())
                    .withException(conjoiner.getException())
                    .withResourceCrn(conjoiner.getEnvironmentDto().getResourceCrn())
                    .withResourceName(conjoiner.getEnvironmentDto().getName())
                    .build();
            eventSender.sendEvent(failedEvent, conjoiner.getEnvironmentDtoEvent().getHeaders());
        }
    }

}
