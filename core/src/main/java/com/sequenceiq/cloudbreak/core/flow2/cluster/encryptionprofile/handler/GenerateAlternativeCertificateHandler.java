package com.sequenceiq.cloudbreak.core.flow2.cluster.encryptionprofile.handler;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.encryptionprofile.UpdateSslConfigsOnClusterStateSelectors.GENERATE_ALTERNATIVE_CERTIFICATE_HANDLER_EVENT;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.encryptionprofile.UpdateSslConfigsOnClusterStateSelectors;
import com.sequenceiq.cloudbreak.core.flow2.cluster.encryptionprofile.event.UpdateSslConfigEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.encryptionprofile.event.UpdateSslConfigFailedEvent;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.service.publicendpoint.GatewayPublicEndpointManagementService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class GenerateAlternativeCertificateHandler extends ExceptionCatcherEventHandler<UpdateSslConfigEvent> {

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
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<UpdateSslConfigEvent> event) {
        return new UpdateSslConfigFailedEvent(resourceId, e);
    }

    @Override
    public Selectable doAccept(HandlerEvent<UpdateSslConfigEvent> event) {
        UpdateSslConfigEvent eventData = event.getData();
        try {
            StackDto stack = stackDtoService.getById(eventData.getResourceId());
            if (gatewayPublicEndpointManagementService.isCertRenewalTriggerable(stack.getStack())) {
                gatewayPublicEndpointManagementService.generateAlternativeCertAndSaveForStack(stack);
                LOGGER.info("Alternative certificate generated for {}", stack.getName());
            }
            return new UpdateSslConfigEvent(UpdateSslConfigsOnClusterStateSelectors.FINISH_UPDATE_SSL_CONFIGS_ON_CLUSTER_EVENT.selector(),
                    eventData.getResourceId(), eventData.getEncryptionProfileCrn());
        } catch (Exception e) {
            LOGGER.error("Exception while generating alternative certificate", e);
            return new UpdateSslConfigFailedEvent(eventData.getResourceId(), e);
        }
    }
}
