package com.sequenceiq.cloudbreak.core.flow2.cluster.encryptionprofile.handler;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.encryptionprofile.EnableEncryptionProfileOnClusterStateSelectors.GENERATE_ALTERNATIVE_CERTIFICATE_HANDLER_EVENT;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.encryptionprofile.EnableEncryptionProfileOnClusterStateSelectors;
import com.sequenceiq.cloudbreak.core.flow2.cluster.encryptionprofile.event.EnableEncryptionProfileFailedEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.encryptionprofile.event.EnableEncryptionProfileOnClusterEvent;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.service.publicendpoint.GatewayPublicEndpointManagementService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class GenerateAlternativeCertificateHandler extends ExceptionCatcherEventHandler<EnableEncryptionProfileOnClusterEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(GenerateAlternativeCertificateHandler.class);

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private GatewayPublicEndpointManagementService gatewayPublicEndpointManagementService;

    @Override
    public String selector() {
        return GENERATE_ALTERNATIVE_CERTIFICATE_HANDLER_EVENT.selector();
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<EnableEncryptionProfileOnClusterEvent> event) {
        return new EnableEncryptionProfileFailedEvent(resourceId, e);
    }

    @Override
    public Selectable doAccept(HandlerEvent<EnableEncryptionProfileOnClusterEvent> event) {
        EnableEncryptionProfileOnClusterEvent eventData = event.getData();
        StackDto stack = stackDtoService.getById(eventData.getResourceId());
        if (gatewayPublicEndpointManagementService.isCertRenewalTriggerable(stack.getStack())) {
            gatewayPublicEndpointManagementService.generateAlternativeCertAndSaveForStack(stack);
            LOGGER.info("Alternative certificate generated for {}", stack.getName());
        }
        return new EnableEncryptionProfileOnClusterEvent(
                EnableEncryptionProfileOnClusterStateSelectors.FINISH_ENABLE_ENCRYPTION_PROFILE_ON_CLUSTER_EVENT.selector(), eventData.getResourceId(),
                eventData.getEncryptionProfileCrn());
    }
}
