package com.sequenceiq.environment.environment.flow.encryptionprofile.handler;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.environment.environment.flow.encryptionprofile.event.EnableEncryptionProfileEvent;
import com.sequenceiq.environment.environment.flow.encryptionprofile.event.EnableEncryptionProfileFailedEvent;
import com.sequenceiq.environment.environment.flow.encryptionprofile.event.EnableEncryptionProfileStateSelectors;
import com.sequenceiq.environment.environment.service.freeipa.FreeIpaPollerService;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class EnableEncryptionProfileOnFreeIpaHandler extends ExceptionCatcherEventHandler<EnableEncryptionProfileEvent> {

    private final FreeIpaPollerService freeIpaPollerService;

    protected EnableEncryptionProfileOnFreeIpaHandler(FreeIpaPollerService freeIpaPollerService) {
        this.freeIpaPollerService = freeIpaPollerService;
    }

    @Override
    public String selector() {
        return EnableEncryptionProfileStateSelectors.UPDATE_SSL_CONFIG_FREEIPA_HANDLER_EVENT.name();
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<EnableEncryptionProfileEvent> event) {
        return new EnableEncryptionProfileFailedEvent(event.getData().getResourceId(), event.getData().getResourceName(),
                event.getData().getResourceCrn(), e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<EnableEncryptionProfileEvent> event) {
        EnableEncryptionProfileEvent payload = event.getData();
        try {
            freeIpaPollerService.waitForSaltUpdate(payload.getResourceId(), payload.getResourceCrn());

            return EnableEncryptionProfileEvent
                    .builder()
                    .withSelector(EnableEncryptionProfileStateSelectors.ENABLE_ENCRYPTION_PROFILE_ON_STACKS_EVENT.selector())
                    .withResourceId(payload.getResourceId())
                    .withResourceName(payload.getResourceName())
                    .withResourceCrn(payload.getResourceCrn())
                    .withEncryptionProfileCrn(payload.getEncryptionProfileCrn())
                    .build();

        } catch (Exception e) {
            return new EnableEncryptionProfileFailedEvent(payload.getResourceId(), payload.getResourceName(),
                    payload.getResourceCrn(), e);
        }
    }
}
