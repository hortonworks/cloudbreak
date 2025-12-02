package com.sequenceiq.freeipa.flow.freeipa.provision.handler;

import static com.sequenceiq.freeipa.flow.freeipa.common.FailureType.ERROR;
import static com.sequenceiq.freeipa.flow.freeipa.common.FailureType.VALIDATION;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.cloudstorage.ValidateCloudStorageFailed;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.cloudstorage.ValidateCloudStorageRequest;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.cloudstorage.ValidateCloudStorageSuccess;
import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaCloudStorageValidationService;
import com.sequenceiq.freeipa.service.stack.StackService;

@Component
public class ValidateCloudStorageHandler extends ExceptionCatcherEventHandler<ValidateCloudStorageRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ValidateCloudStorageHandler.class);

    @Inject
    private StackService stackService;

    @Inject
    private FreeIpaCloudStorageValidationService freeIpaCloudStorageValidationService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(ValidateCloudStorageRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<ValidateCloudStorageRequest> event) {
        return new ValidateCloudStorageFailed(resourceId, e, ERROR);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<ValidateCloudStorageRequest> event) {
        ValidateCloudStorageRequest request = event.getData();
        Selectable response;
        try {
            Stack stack = stackService.getByIdWithListsInTransaction(request.getResourceId());
            freeIpaCloudStorageValidationService.validate(stack);
            response = new ValidateCloudStorageSuccess(request.getResourceId());
        } catch (Exception e) {
            LOGGER.error("FreeIPA cloud storage validation failed", e);
            response = new ValidateCloudStorageFailed(request.getResourceId(), e, VALIDATION);
        }
        return response;
    }
}
