package com.sequenceiq.freeipa.flow.freeipa.rootvolumeupdate.handler;

import static com.sequenceiq.freeipa.flow.freeipa.common.FailureType.ERROR;
import static com.sequenceiq.freeipa.flow.freeipa.rootvolumeupdate.FreeIpaProviderTemplateUpdateFlowEvent.FREEIPA_PROVIDER_TEMPLATE_UPDATE_FINISHED_EVENT;

import java.util.List;
import java.util.Optional;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.UpdateType;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.freeipa.rootvolumeupdate.event.FreeIpaProviderTemplateUpdateEvent;
import com.sequenceiq.freeipa.flow.freeipa.rootvolumeupdate.event.FreeIpaProviderTemplateUpdateFailureEvent;
import com.sequenceiq.freeipa.flow.freeipa.rootvolumeupdate.event.FreeIpaProviderTemplateUpdateHandlerRequest;
import com.sequenceiq.freeipa.service.stack.StackService;

@Component
public class FreeIpaProviderTemplateUpdateHandler extends ExceptionCatcherEventHandler<FreeIpaProviderTemplateUpdateHandlerRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaProviderTemplateUpdateHandler.class);

    @Inject
    private StackService stackService;

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(FreeIpaProviderTemplateUpdateHandlerRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<FreeIpaProviderTemplateUpdateHandlerRequest> event) {
        return new FreeIpaProviderTemplateUpdateFailureEvent(resourceId, "Exception in Launch Template Update Handler", e, ERROR);
    }

    @Override
    public Selectable doAccept(HandlerEvent<FreeIpaProviderTemplateUpdateHandlerRequest> deploymentTemplateUpdateRequestEvent) {
        LOGGER.debug("Starting DeploymentTemplateUpdateHandler with request: {}", deploymentTemplateUpdateRequestEvent);
        FreeIpaProviderTemplateUpdateHandlerRequest payload = deploymentTemplateUpdateRequestEvent.getData();
        Stack stack = stackService.getByIdWithListsInTransaction(payload.getResourceId());
        CloudStack cloudStack = payload.getCloudStack();
        CloudCredential cloudCredential = payload.getCloudCredential();
        CloudContext cloudContext = payload.getCloudContext();
        CloudConnector cloudConnector = cloudPlatformConnectors.get(cloudContext.getPlatformVariant());
        AuthenticatedContext authenticatedContext = cloudConnector.authentication().authenticate(cloudContext, cloudCredential);
        try {
            cloudConnector.resources().update(authenticatedContext, cloudStack, List.of(),
                    UpdateType.PROVIDER_TEMPLATE_UPDATE, Optional.empty());
        } catch (Exception e) {
            LOGGER.warn("Exception while updating Provider Template - " + e.getMessage());
            throw new RuntimeException(e);
        }
        return new FreeIpaProviderTemplateUpdateEvent(FREEIPA_PROVIDER_TEMPLATE_UPDATE_FINISHED_EVENT.selector(), payload.getOperationId(), stack.getId());
    }
}
