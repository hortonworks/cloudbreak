package com.sequenceiq.datalake.flow.enableselinux.handler;

import static com.sequenceiq.datalake.entity.DatalakeStatusEnum.DATALAKE_ENABLE_SELINUX_FAILED;
import static com.sequenceiq.datalake.flow.enableselinux.event.DatalakeEnableSeLinuxStateSelectors.ENABLE_SELINUX_DATALAKE_EVENT;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.Promise;
import com.sequenceiq.datalake.flow.enableselinux.event.DatalakeEnableSeLinuxEvent;
import com.sequenceiq.datalake.flow.enableselinux.event.DatalakeEnableSeLinuxFailedEvent;
import com.sequenceiq.datalake.flow.enableselinux.event.DatalakeEnableSeLinuxHandlerSelectors;
import com.sequenceiq.flow.reactor.api.event.EventSender;
import com.sequenceiq.flow.reactor.api.handler.EventSenderAwareHandler;

@Component
public class DatalakeValidateEnableSeLinuxHandler extends EventSenderAwareHandler<DatalakeEnableSeLinuxEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatalakeValidateEnableSeLinuxHandler.class);

    protected DatalakeValidateEnableSeLinuxHandler(EventSender eventSender) {
        super(eventSender);
    }

    @Override
    public String selector() {
        return DatalakeEnableSeLinuxHandlerSelectors.ENABLE_SELINUX_DATALAKE_VALIDATION_HANDLER.selector();
    }

    @Override
    public void accept(Event<DatalakeEnableSeLinuxEvent> enableSeLinuxEventEvent) {
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
                    new DatalakeEnableSeLinuxFailedEvent(enableSeLinuxEventEvent.getData(), e, DATALAKE_ENABLE_SELINUX_FAILED);
            eventSender().sendEvent(failedEvent, enableSeLinuxEventEvent.getHeaders());
        }
    }
}
