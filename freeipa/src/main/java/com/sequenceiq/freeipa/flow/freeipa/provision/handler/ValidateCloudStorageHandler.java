package com.sequenceiq.freeipa.flow.freeipa.provision.handler;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.cloudstorage.ValidateCloudStorageFailed;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.cloudstorage.ValidateCloudStorageRequest;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.cloudstorage.ValidateCloudStorageSuccess;
import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaCloudStorageValidationService;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class ValidateCloudStorageHandler extends ExceptionCatcherEventHandler<ValidateCloudStorageRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ValidateCloudStorageHandler.class);

    @Inject
    private EventBus eventBus;

    @Inject
    private FreeIpaCloudStorageValidationService freeIpaCloudStorageValidationService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(ValidateCloudStorageRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<ValidateCloudStorageRequest> event) {
        return new ValidateCloudStorageFailed(resourceId, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent event) {
        ValidateCloudStorageRequest request = event.getData();
        Selectable response;
        try {
            freeIpaCloudStorageValidationService.validate(request.getResourceId());
            response = new ValidateCloudStorageSuccess(request.getResourceId());
        } catch (Exception e) {
            LOGGER.error("FreeIPA cloud storage validation failed", e);
            response = new ValidateCloudStorageFailed(request.getResourceId(), e);
        }
        return response;
    }
}
