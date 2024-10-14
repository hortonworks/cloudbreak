package com.sequenceiq.cloudbreak.core.flow2.stack.rootvolumeupdate.handler;

import static com.sequenceiq.cloudbreak.core.flow2.stack.rootvolumeupdate.CoreProviderTemplateUpdateFlowEvent.CORE_PROVIDER_TEMPLATE_UPDATE_FINISHED_EVENT;

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
import com.sequenceiq.cloudbreak.core.flow2.stack.rootvolumeupdate.ProviderTemplateUpdateHandlerRequest;
import com.sequenceiq.cloudbreak.core.flow2.stack.rootvolumeupdate.event.CoreProviderTemplateUpdateEvent;
import com.sequenceiq.cloudbreak.core.flow2.stack.rootvolumeupdate.event.CoreProviderTemplateUpdateFailureEvent;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.stack.flow.RootDiskValidationService;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class CoreProviderTemplateUpdateHandler extends ExceptionCatcherEventHandler<ProviderTemplateUpdateHandlerRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CoreProviderTemplateUpdateHandler.class);

    @Inject
    private RootDiskValidationService rootDiskValidationService;

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(ProviderTemplateUpdateHandlerRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<ProviderTemplateUpdateHandlerRequest> event) {
        return new CoreProviderTemplateUpdateFailureEvent(resourceId, "Exception in Launch Template Update Handler", e);
    }

    @Override
    public Selectable doAccept(HandlerEvent<ProviderTemplateUpdateHandlerRequest> launchTemplateUpdateRequestEvent) {
        LOGGER.debug("Starting CoreLaunchTemplateUpdateHandler with request: {}", launchTemplateUpdateRequestEvent);
        ProviderTemplateUpdateHandlerRequest payload = launchTemplateUpdateRequestEvent.getData();
        StackDto stackDto = stackDtoService.getById(payload.getResourceId());
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
        return new CoreProviderTemplateUpdateEvent(CORE_PROVIDER_TEMPLATE_UPDATE_FINISHED_EVENT.selector(), stackDto.getId());
    }
}
