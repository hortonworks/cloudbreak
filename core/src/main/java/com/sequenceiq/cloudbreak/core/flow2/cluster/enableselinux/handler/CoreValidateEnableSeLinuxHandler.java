package com.sequenceiq.cloudbreak.core.flow2.cluster.enableselinux.handler;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.enableselinux.event.CoreEnableSeLinuxHandlerSelectors.ENABLE_SELINUX_CORE_VALIDATION_HANDLER;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.enableselinux.event.CoreEnableSeLinuxStateSelectors.ENABLE_SELINUX_CORE_EVENT;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.flow2.cluster.enableselinux.event.CoreEnableSeLinuxEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.enableselinux.event.CoreEnableSeLinuxFailedEvent;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.Promise;
import com.sequenceiq.flow.reactor.api.event.EventSender;
import com.sequenceiq.flow.reactor.api.handler.EventSenderAwareHandler;

@Component
public class CoreValidateEnableSeLinuxHandler extends EventSenderAwareHandler<CoreEnableSeLinuxEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CoreValidateEnableSeLinuxHandler.class);

    protected CoreValidateEnableSeLinuxHandler(EventSender eventSender) {
        super(eventSender);
    }

    @Override
    public String selector() {
        return ENABLE_SELINUX_CORE_VALIDATION_HANDLER.selector();
    }

    @Override
    public void accept(Event<CoreEnableSeLinuxEvent> enableSeLinuxEventEvent) {
        try {
            CoreEnableSeLinuxEvent result = CoreEnableSeLinuxEvent.builder()
                    .withAccepted(new Promise<>())
                    .withSelector(ENABLE_SELINUX_CORE_EVENT.selector())
                    .withResourceCrn(enableSeLinuxEventEvent.getData().getResourceCrn())
                    .withResourceId(enableSeLinuxEventEvent.getData().getResourceId())
                    .withResourceName(enableSeLinuxEventEvent.getData().getResourceName())
                    .build();

            eventSender().sendEvent(result, enableSeLinuxEventEvent.getHeaders());
        } catch (Exception e) {
            CoreEnableSeLinuxFailedEvent failedEvent =
                    new CoreEnableSeLinuxFailedEvent(enableSeLinuxEventEvent.getData(), e);
            eventSender().sendEvent(failedEvent, enableSeLinuxEventEvent.getHeaders());
        }
    }
}
