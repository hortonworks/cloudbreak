package com.sequenceiq.cloudbreak.core.flow2.cluster.enableselinux.handler;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.enableselinux.event.CoreEnableSeLinuxHandlerSelectors.ENABLE_SELINUX_CORE_HANDLER;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.enableselinux.event.CoreEnableSeLinuxStateSelectors.FINISH_ENABLE_SELINUX_CORE_EVENT;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.flow2.cluster.enableselinux.event.CoreEnableSeLinuxEvent;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.flow.reactor.api.event.EventSender;
import com.sequenceiq.flow.reactor.api.handler.EventSenderAwareHandler;

@Component
public class CoreEnableSeLinuxHandler extends EventSenderAwareHandler<CoreEnableSeLinuxEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CoreEnableSeLinuxHandler.class);

    protected CoreEnableSeLinuxHandler(EventSender eventSender) {
        super(eventSender);
    }

    @Override
    public String selector() {
        return ENABLE_SELINUX_CORE_HANDLER.selector();
    }

    @Override
    public void accept(Event<CoreEnableSeLinuxEvent> enableSeLinuxEventEvent) {
        CoreEnableSeLinuxEvent enableSeLinuxEvent = enableSeLinuxEventEvent.getData();
        CoreEnableSeLinuxEvent result = CoreEnableSeLinuxEvent.builder()
                .withSelector(FINISH_ENABLE_SELINUX_CORE_EVENT.selector())
                .withResourceCrn(enableSeLinuxEvent.getResourceCrn())
                .withResourceId(enableSeLinuxEvent.getResourceId())
                .withResourceName(enableSeLinuxEvent.getResourceName())
                .build();

        eventSender().sendEvent(result, enableSeLinuxEventEvent.getHeaders());
    }

}
