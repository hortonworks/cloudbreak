package com.sequenceiq.datalake.flow.enableselinux.handler;

import static com.sequenceiq.datalake.flow.enableselinux.event.DatalakeEnableSeLinuxStateSelectors.ENABLE_SELINUX_DATALAKE_EVENT;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.Promise;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.flow.enableselinux.event.DatalakeEnableSeLinuxEvent;
import com.sequenceiq.datalake.flow.enableselinux.event.DatalakeEnableSeLinuxFailedEvent;
import com.sequenceiq.datalake.flow.enableselinux.event.DatalakeEnableSeLinuxHandlerSelectors;
import com.sequenceiq.flow.reactor.api.event.EventSender;
import com.sequenceiq.flow.reactor.api.handler.EventSenderAwareHandler;

@Component
public class DatalakeEnableSeLinuxHandler extends EventSenderAwareHandler<DatalakeEnableSeLinuxEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatalakeEnableSeLinuxHandler.class);

    protected DatalakeEnableSeLinuxHandler(EventSender eventSender) {
        super(eventSender);
    }

    @Override
    public String selector() {
        return DatalakeEnableSeLinuxHandlerSelectors.ENABLE_SELINUX_DATALAKE_HANDLER.selector();
    }

    @Override
    public void accept(Event<DatalakeEnableSeLinuxEvent> enableSeLinuxEventEvent) {
        DatalakeEnableSeLinuxEvent enableSeLinuxEvent = enableSeLinuxEventEvent.getData();
        try {
            DatalakeEnableSeLinuxEvent result = DatalakeEnableSeLinuxEvent.builder()
                    .withAccepted(new Promise<>())
                    .withSelector(ENABLE_SELINUX_DATALAKE_EVENT.selector())
                    .withResourceCrn(enableSeLinuxEventEvent.getData().getResourceCrn())
                    .withResourceId(enableSeLinuxEventEvent.getData().getResourceId())
                    .withResourceName(enableSeLinuxEventEvent.getData().getResourceName())
                    .build();

            eventSender().sendEvent(result, enableSeLinuxEventEvent.getHeaders());
        } catch (Exception e) {
            DatalakeEnableSeLinuxFailedEvent failedEvent =
                    new DatalakeEnableSeLinuxFailedEvent(enableSeLinuxEvent, e, DatalakeStatusEnum.DATALAKE_ENABLE_SELINUX_FAILED);
            eventSender().sendEvent(failedEvent, enableSeLinuxEventEvent.getHeaders());
        }
    }

}
