package com.sequenceiq.cloudbreak.core.flow2.cluster.encryptionprofile.handler;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.encryptionprofile.EnableEncryptionProfileOnClusterStateSelectors.UPDATE_CM_POLICY_HANDLER_EVENT;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.cluster.ClusterBuilderService;
import com.sequenceiq.cloudbreak.core.flow2.cluster.encryptionprofile.EnableEncryptionProfileOnClusterStateSelectors;
import com.sequenceiq.cloudbreak.core.flow2.cluster.encryptionprofile.event.EnableEncryptionProfileFailedEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.encryptionprofile.event.EnableEncryptionProfileOnClusterEvent;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class UpdateClouderaManagerPolicyHandler extends ExceptionCatcherEventHandler<EnableEncryptionProfileOnClusterEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateClouderaManagerPolicyHandler.class);

    @Inject
    private ClusterBuilderService clusterBuilderService;

    @Override
    public String selector() {
        return UPDATE_CM_POLICY_HANDLER_EVENT.selector();
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<EnableEncryptionProfileOnClusterEvent> event) {
        return new EnableEncryptionProfileFailedEvent(resourceId, e);
    }

    @Override
    public Selectable doAccept(HandlerEvent<EnableEncryptionProfileOnClusterEvent> event) {
        EnableEncryptionProfileOnClusterEvent eventData = event.getData();
        clusterBuilderService.configurePolicy(eventData.getResourceId());
        LOGGER.info("Cloudera Manager policy updated for stack {}", eventData.getResourceId());
        return new EnableEncryptionProfileOnClusterEvent(EnableEncryptionProfileOnClusterStateSelectors.GENERATE_ALTERNATIVE_CERTIFICATE_EVENT.selector(),
                eventData.getResourceId(),
                eventData.getEncryptionProfileCrn());
    }
}
