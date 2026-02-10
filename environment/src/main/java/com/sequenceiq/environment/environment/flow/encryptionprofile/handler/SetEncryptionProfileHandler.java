package com.sequenceiq.environment.environment.flow.encryptionprofile.handler;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.environment.encryptionprofile.service.EncryptionProfileService;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.flow.encryptionprofile.event.EnableEncryptionProfileEvent;
import com.sequenceiq.environment.environment.flow.encryptionprofile.event.EnableEncryptionProfileFailedEvent;
import com.sequenceiq.environment.environment.flow.encryptionprofile.event.EnableEncryptionProfileStateSelectors;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class SetEncryptionProfileHandler extends ExceptionCatcherEventHandler<EnableEncryptionProfileEvent> {

    private final EnvironmentService environmentService;

    private final EncryptionProfileService encryptionProfileService;

    protected SetEncryptionProfileHandler(EnvironmentService environmentService, EncryptionProfileService encryptionProfileService) {
        this.environmentService = environmentService;
        this.encryptionProfileService = encryptionProfileService;
    }

    @Override
    public String selector() {
        return EnableEncryptionProfileStateSelectors.SET_ENCRYPTION_PROFILE_HANDLER_EVENT.name();
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
            Optional<Environment> environmentOptional = environmentService.findEnvironmentById(payload.getResourceId());
            if (environmentOptional.isPresent()) {
                Environment environment = environmentOptional.get();
                encryptionProfileService.setEncryptionProfile(environment, payload.getEncryptionProfileCrn());
                environmentService.save(environment);
            }

            return EnableEncryptionProfileEvent
                    .builder()
                    .withSelector(EnableEncryptionProfileStateSelectors.UPDATE_SSL_CONFIG_FREEIPA_EVENT.selector())
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
