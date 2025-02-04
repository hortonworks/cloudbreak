package com.sequenceiq.environment.environment.flow.enableselinux.freeipa.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.Promise;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.flow.enableselinux.freeipa.event.EnvironmentEnableSeLinuxEvent;
import com.sequenceiq.environment.environment.flow.enableselinux.freeipa.event.EnvironmentEnableSeLinuxFailedEvent;
import com.sequenceiq.environment.environment.flow.enableselinux.freeipa.event.EnvironmentEnableSeLinuxHandlerSelectors;
import com.sequenceiq.environment.environment.flow.enableselinux.freeipa.event.EnvironmentEnableSeLinuxStateSelectors;
import com.sequenceiq.flow.reactor.api.event.EventSender;
import com.sequenceiq.flow.reactor.api.handler.EventSenderAwareHandler;

@Component
public class EnvironmentValidateEnableSeLinuxHandler extends EventSenderAwareHandler<EnvironmentEnableSeLinuxEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentValidateEnableSeLinuxHandler.class);

    protected EnvironmentValidateEnableSeLinuxHandler(EventSender eventSender) {
        super(eventSender);
    }

    @Override
    public String selector() {
        return EnvironmentEnableSeLinuxHandlerSelectors.ENABLE_SELINUX_FREEIPA_VALIDATION_HANDLER.selector();
    }

    @Override
    public void accept(Event<EnvironmentEnableSeLinuxEvent> enableSeLinuxEventEvent) {
        LOGGER.debug("In EnvironmentValidateEnableSeLinuxHandler.accept");
        try {
            EnvironmentEnableSeLinuxEvent result = EnvironmentEnableSeLinuxEvent.builder()
                    .withAccepted(new Promise<>())
                    .withSelector(EnvironmentEnableSeLinuxStateSelectors.ENABLE_SELINUX_FREEIPA_EVENT.selector())
                    .withResourceCrn(enableSeLinuxEventEvent.getData().getResourceCrn())
                    .withResourceId(enableSeLinuxEventEvent.getData().getResourceId())
                    .withResourceName(enableSeLinuxEventEvent.getData().getResourceName())
                    .build();

            eventSender().sendEvent(result, enableSeLinuxEventEvent.getHeaders());
            LOGGER.debug("ENABLE_SELINUX_FREEIPA_EVENT event sent");
        } catch (Exception e) {
            EnvironmentEnableSeLinuxFailedEvent failedEvent =
                    new EnvironmentEnableSeLinuxFailedEvent(enableSeLinuxEventEvent.getData(), e, EnvironmentStatus.VERTICAL_SCALE_FAILED);
            eventSender().sendEvent(failedEvent, enableSeLinuxEventEvent.getHeaders());
            LOGGER.debug("ENABLE_SELINUX_ON_FREEIPA_FAILED event sent");
        }
    }
}
