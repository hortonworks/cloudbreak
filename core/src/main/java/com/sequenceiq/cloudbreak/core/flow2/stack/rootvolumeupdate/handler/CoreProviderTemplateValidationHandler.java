package com.sequenceiq.cloudbreak.core.flow2.stack.rootvolumeupdate.handler;

import static com.sequenceiq.cloudbreak.core.flow2.stack.rootvolumeupdate.CoreProviderTemplateUpdateFlowEvent.CORE_PROVIDER_TEMPLATE_VALIDATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.rootvolumeupdate.CoreProviderTemplateUpdateFlowEvent.CORE_PROVIDER_TEMPLATE_VALIDATION_FINISHED_EVENT;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.stack.rootvolumeupdate.ProviderTemplateUpdateHandlerRequest;
import com.sequenceiq.cloudbreak.core.flow2.stack.rootvolumeupdate.event.CoreProviderTemplateUpdateEvent;
import com.sequenceiq.cloudbreak.core.flow2.stack.rootvolumeupdate.event.CoreProviderTemplateUpdateFailureEvent;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.stack.flow.RootDiskValidationService;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class CoreProviderTemplateValidationHandler extends ExceptionCatcherEventHandler<ProviderTemplateUpdateHandlerRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CoreProviderTemplateValidationHandler.class);

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Inject
    private RootDiskValidationService rootDiskValidationService;

    @Override
    public String selector() {
        return CORE_PROVIDER_TEMPLATE_VALIDATION_EVENT.selector();
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
        try {
            rootDiskValidationService.validateRootDiskAgainstProviderAndUpdateTemplate(
                    stackDto,
                    payload.getVolumeType(),
                    payload.getGroup(),
                    payload.getSize()
            );
        } catch (Exception e) {
            LOGGER.warn("Exception while updating Provider Template - " + e.getMessage());
            throw new RuntimeException(e);
        }
        return new CoreProviderTemplateUpdateEvent(
                CORE_PROVIDER_TEMPLATE_VALIDATION_FINISHED_EVENT.selector(),
                stackDto.getId(),
                payload.getVolumeType(),
                payload.getSize(),
                payload.getGroup(),
                payload.getDiskType()
        );
    }
}
