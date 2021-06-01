package com.sequenceiq.cloudbreak.reactor.handler.orchestration;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.provision.service.CloudStorageValidationService;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.ValidateCloudStorageFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.ValidateCloudStorageRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.ValidateCloudStorageSuccess;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.EventHandler;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class CloudStorageVmValidationHandler implements EventHandler<ValidateCloudStorageRequest> {

    @Inject
    private EventBus eventBus;

    @Inject
    private CloudStorageValidationService cloudStorageValidationService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(ValidateCloudStorageRequest.class);
    }

    @Override
    public void accept(Event<ValidateCloudStorageRequest> event) {
        Long stackId = event.getData().getResourceId();
        Selectable response;
        try {
            cloudStorageValidationService.validateCloudStorage(stackId);
            response = new ValidateCloudStorageSuccess(stackId);
        } catch (Exception e) {
            response = new ValidateCloudStorageFailed(stackId, e);
        }
        eventBus.notify(response.selector(), new Event<>(event.getHeaders(), response));
    }
}
