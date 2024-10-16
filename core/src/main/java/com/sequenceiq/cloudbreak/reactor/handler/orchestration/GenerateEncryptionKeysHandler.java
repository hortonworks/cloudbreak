package com.sequenceiq.cloudbreak.reactor.handler.orchestration;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.encryption.GenerateEncryptionKeysFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.encryption.GenerateEncryptionKeysRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.encryption.GenerateEncryptionKeysSuccess;
import com.sequenceiq.cloudbreak.service.encryption.EncryptionKeyService;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class GenerateEncryptionKeysHandler extends ExceptionCatcherEventHandler<GenerateEncryptionKeysRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(GenerateEncryptionKeysHandler.class);

    @Inject
    private EncryptionKeyService encryptionKeyService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(GenerateEncryptionKeysRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<GenerateEncryptionKeysRequest> event) {
        LOGGER.error("Generating Encryption Keys has failed with unexpected error", e);
        return new GenerateEncryptionKeysFailed(resourceId, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<GenerateEncryptionKeysRequest> event) {
        StackEvent request = event.getData();
        encryptionKeyService.generateEncryptionKeys(request.getResourceId());
        return new GenerateEncryptionKeysSuccess(request.getResourceId());
    }
}
