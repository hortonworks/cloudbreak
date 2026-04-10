package com.sequenceiq.environment.environment.flow.encryptionprofile.handler;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.environment.environment.flow.encryptionprofile.event.EnableEncryptionProfileEvent;
import com.sequenceiq.environment.environment.flow.encryptionprofile.event.EnableEncryptionProfileFailedEvent;
import com.sequenceiq.environment.environment.flow.encryptionprofile.event.EnableEncryptionProfileStateSelectors;
import com.sequenceiq.environment.environment.flow.encryptionprofile.validator.EncryptionProfileValidator;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class ValidateEnableEncryptionProfileHandler extends ExceptionCatcherEventHandler<EnableEncryptionProfileEvent> {

    private final EncryptionProfileValidator encryptionProfileValidator;

    protected ValidateEnableEncryptionProfileHandler(EncryptionProfileValidator encryptionProfileValidator) {
        this.encryptionProfileValidator = encryptionProfileValidator;
    }

    @Override
    public String selector() {
        return EnableEncryptionProfileStateSelectors.VALIDATE_ENABLE_ENCRYPTION_PROFILE_HANDLER_EVENT.name();
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
            encryptionProfileValidator.validate(payload.getResourceCrn());

            return EnableEncryptionProfileEvent
                    .builder()
                    .withSelector(EnableEncryptionProfileStateSelectors.SET_ENCRYPTION_PROFILE_EVENT.selector())
                    .withResourceId(payload.getResourceId())
                    .withResourceName(payload.getResourceName())
                    .withResourceCrn(payload.getResourceCrn())
                    .withEncryptionProfileCrn(payload.getEncryptionProfileCrn())
                    .build();

        } catch (Exception e) {
            return new EnableEncryptionProfileFailedEvent(payload.getResourceId(), payload.getResourceName(),
                    payload.getEncryptionProfileCrn(), e);
        }
    }
}
