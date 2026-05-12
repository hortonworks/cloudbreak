package com.sequenceiq.cloudbreak.core.flow2.cluster.encryptionprofile.handler;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.encryptionprofile.UpdateSslConfigsOnClusterStateSelectors.UPDATE_CM_POLICY_HANDLER_EVENT;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.cluster.ClusterBuilderService;
import com.sequenceiq.cloudbreak.core.flow2.cluster.encryptionprofile.UpdateSslConfigsOnClusterStateSelectors;
import com.sequenceiq.cloudbreak.core.flow2.cluster.encryptionprofile.event.UpdateSslConfigEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.encryptionprofile.event.UpdateSslConfigFailedEvent;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class UpdateClouderaManagerPolicyHandler extends ExceptionCatcherEventHandler<UpdateSslConfigEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateClouderaManagerPolicyHandler.class);

    @Inject
    private ClusterBuilderService clusterBuilderService;

    @Override
    public String selector() {
        return UPDATE_CM_POLICY_HANDLER_EVENT.selector();
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<UpdateSslConfigEvent> event) {
        return new UpdateSslConfigFailedEvent(resourceId, e);
    }

    @Override
    public Selectable doAccept(HandlerEvent<UpdateSslConfigEvent> event) {
        UpdateSslConfigEvent eventData = event.getData();
        try {
            clusterBuilderService.configurePolicy(eventData.getResourceId());
            return new UpdateSslConfigEvent(UpdateSslConfigsOnClusterStateSelectors.GENERATE_ALTERNATIVE_CERTIFICATE_EVENT.selector(),
                    eventData.getResourceId(),
                    eventData.getEncryptionProfileCrn());
        } catch (Exception e) {
            LOGGER.error("Failed to update Cloudera Manager policy: {}", e.getMessage());
            return new UpdateSslConfigFailedEvent(eventData.getResourceId(), e);
        }
    }
}