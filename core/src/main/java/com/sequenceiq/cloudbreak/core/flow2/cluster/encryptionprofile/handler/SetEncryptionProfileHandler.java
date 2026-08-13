package com.sequenceiq.cloudbreak.core.flow2.cluster.encryptionprofile.handler;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.encryptionprofile.EnableEncryptionProfileOnClusterStateSelectors.SET_ENCRYPTION_PROFILE_HANDLER_EVENT;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.encryptionprofile.EnableEncryptionProfileOnClusterStateSelectors;
import com.sequenceiq.cloudbreak.core.flow2.cluster.encryptionprofile.event.EnableEncryptionProfileFailedEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.encryptionprofile.event.EnableEncryptionProfileOnClusterEvent;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.service.encryptionprofile.EncryptionProfileService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class SetEncryptionProfileHandler extends ExceptionCatcherEventHandler<EnableEncryptionProfileOnClusterEvent> {

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
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<EnableEncryptionProfileOnClusterEvent> event) {
        return new EnableEncryptionProfileFailedEvent(resourceId, e);
    }

    @Override
    public Selectable doAccept(HandlerEvent<EnableEncryptionProfileOnClusterEvent> event) {
        EnableEncryptionProfileOnClusterEvent eventData = event.getData();
        Stack stack = stackService.getByIdWithListsInTransaction(eventData.getResourceId());
        LOGGER.info("Setting encryption profile for stack {}", stack.getName());
        encryptionProfileService.setEncryptionProfile(eventData.getEncryptionProfileCrn(), stack);
        return new EnableEncryptionProfileOnClusterEvent(EnableEncryptionProfileOnClusterStateSelectors.UPDATE_CM_POLICY_EVENT.selector(),
                eventData.getResourceId(),
                eventData.getEncryptionProfileCrn());
    }
}
