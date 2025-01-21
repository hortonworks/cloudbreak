package com.sequenceiq.cloudbreak.reactor.handler.orchestration;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.HostMetadataSetupSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.ValidateCloudStorageRequest;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.EventHandler;

@Component
public class CloudStorageVmValidationHandler implements EventHandler<ValidateCloudStorageRequest> {

    @Inject
    private EventBus eventBus;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(ValidateCloudStorageRequest.class);
    }

    @Override
    public void accept(Event<ValidateCloudStorageRequest> event) {
        Selectable response = new HostMetadataSetupSuccess(event.getData().getResourceId());
        eventBus.notify(response.selector(), new Event<>(event.getHeaders(), response));
    }
}
