package com.sequenceiq.cloudbreak.core.flow2.cluster.encryptionprofile.handler;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.encryptionprofile.UpdateSslConfigsOnClusterStateSelectors.SET_ENCRYPTION_PROFILE_HANDLER_EVENT;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.encryptionprofile.UpdateSslConfigsOnClusterStateSelectors;
import com.sequenceiq.cloudbreak.core.flow2.cluster.encryptionprofile.event.UpdateSslConfigEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.encryptionprofile.event.UpdateSslConfigFailedEvent;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.service.encryptionprofile.EncryptionProfileService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class SetEncryptionProfileHandler extends ExceptionCatcherEventHandler<UpdateSslConfigEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SetEncryptionProfileHandler.class);

    @Inject
    private StackService stackService;

    @Inject
    private EncryptionProfileService encryptionProfileService;

    @Override
    public String selector() {
        return SET_ENCRYPTION_PROFILE_HANDLER_EVENT.selector();
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<UpdateSslConfigEvent> event) {
        return new UpdateSslConfigFailedEvent(resourceId, e);
    }

    @Override
    public Selectable doAccept(HandlerEvent<UpdateSslConfigEvent> event) {
        UpdateSslConfigEvent eventData = event.getData();
        try {
            Stack stack = stackService.getByIdWithListsInTransaction(eventData.getResourceId());
            LOGGER.debug("Updating SSL config for {}", stack.getName());
            encryptionProfileService.setEncryptionProfile(eventData.getEncryptionProfileCrn(), stack);
            return new UpdateSslConfigEvent(UpdateSslConfigsOnClusterStateSelectors.UPDATE_CM_POLICY_EVENT.selector(),
                    eventData.getResourceId(),
                    eventData.getEncryptionProfileCrn());
        } catch (Exception e) {
            LOGGER.error("Exception set encryption profile", e);
            return new UpdateSslConfigFailedEvent(eventData.getResourceId(), e);
        }
    }
}
