package com.sequenceiq.cloudbreak.core.flow2.cluster.encryptionprofile.handler;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.encryptionprofile.DisableEncryptionProfileOnClusterStateSelectors.UNSET_ENCRYPTION_PROFILE_HANDLER_EVENT;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.core.flow2.cluster.encryptionprofile.DisableEncryptionProfileOnClusterStateSelectors;
import com.sequenceiq.cloudbreak.core.flow2.cluster.encryptionprofile.event.DisableEncryptionProfileFailedEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.encryptionprofile.event.DisableEncryptionProfileOnClusterEvent;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class UnsetEncryptionProfileHandler extends ExceptionCatcherEventHandler<DisableEncryptionProfileOnClusterEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UnsetEncryptionProfileHandler.class);

    @Inject
    private ClusterService clusterService;

    @Override
    public String selector() {
        return UNSET_ENCRYPTION_PROFILE_HANDLER_EVENT.selector();
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<DisableEncryptionProfileOnClusterEvent> event) {
        return new DisableEncryptionProfileFailedEvent(resourceId, e);
    }

    @Override
    public Selectable doAccept(HandlerEvent<DisableEncryptionProfileOnClusterEvent> event) {
        DisableEncryptionProfileOnClusterEvent eventData = event.getData();
        Long clusterId = clusterService.findClusterIdByStackId(eventData.getResourceId())
                .orElseThrow(() -> new NotFoundException(String.format("Cluster not found for stack %s", eventData.getResourceId())));
        LOGGER.info("Unsetting encryption profile CRN for cluster {}", clusterId);
        clusterService.disableEncryptionProfile(clusterId);
        return new DisableEncryptionProfileOnClusterEvent(
                DisableEncryptionProfileOnClusterStateSelectors.UNSET_ENCRYPTION_PROFILE_FINISHED_EVENT.selector(),
                eventData.getResourceId());
    }
}
