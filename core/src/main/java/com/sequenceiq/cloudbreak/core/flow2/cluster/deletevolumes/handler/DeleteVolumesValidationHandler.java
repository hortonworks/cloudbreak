package com.sequenceiq.cloudbreak.core.flow2.cluster.deletevolumes.handler;

import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.base.Enums;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackDeleteVolumesRequest;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.converter.spi.ResourceToCloudResourceConverter;
import com.sequenceiq.cloudbreak.core.flow2.cluster.deletevolumes.BlackListedDeleteVolumesRole;
import com.sequenceiq.cloudbreak.core.flow2.cluster.deletevolumes.DeleteVolumesService;
import com.sequenceiq.cloudbreak.core.flow2.cluster.deletevolumes.DeleteVolumesValidationRequest;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.DeleteVolumesFailedEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.DeleteVolumesRequest;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.template.model.ServiceComponent;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class DeleteVolumesValidationHandler extends ExceptionCatcherEventHandler<DeleteVolumesValidationRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeleteVolumesValidationHandler.class);

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private CmTemplateProcessorFactory cmTemplateProcessorFactory;

    @Inject
    private ResourceToCloudResourceConverter cloudResourceConverter;

    @Inject
    private DeleteVolumesService deleteVolumesService;

    @Inject
    private EventBus eventBus;

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<DeleteVolumesValidationRequest> event) {
        return new DeleteVolumesFailedEvent(e.getMessage(), e, resourceId);
    }

    @Override
    public Selectable doAccept(HandlerEvent<DeleteVolumesValidationRequest> deleteVolumesValidationEvent) {
        LOGGER.debug("Received event: {}", deleteVolumesValidationEvent);
        DeleteVolumesValidationRequest payload = deleteVolumesValidationEvent.getData();
        StackDeleteVolumesRequest stackDeleteVolumesRequest = payload.getStackDeleteVolumesRequest();
        StackDto stack = stackDtoService.getById(payload.getResourceId());
        String requestGroup = stackDeleteVolumesRequest.getGroup();
        String blueprintText = stack.getBlueprint().getBlueprintText();
        CmTemplateProcessor processor = cmTemplateProcessorFactory.get(blueprintText);
        Set<String> hostTemplateComponents = processor.getComponentsInHostGroup(requestGroup);
        Set<ServiceComponent> hostTemplateServiceComponents = processor.getServiceComponentsByHostGroup().get(requestGroup);
        boolean computeInstance = true;
        for (String service : hostTemplateComponents) {
            com.google.common.base.Optional<BlackListedDeleteVolumesRole> enumValue =
                    Enums.getIfPresent(BlackListedDeleteVolumesRole.class, service);
            if (enumValue.isPresent()) {
                computeInstance = false;
            }
        }
        if (!computeInstance) {
            LOGGER.error("Deleting volumes flow is being stopped, as instance being scaled is not a compute instance.");
            return new DeleteVolumesFailedEvent(
                    "BadRequestException: Instance group being scaled isn't a compute instance",
                    new BadRequestException("BadRequestException: Instance group being scaled isn't a compute instance"),
                    stack.getId());
        } else {
            List<CloudResource> cloudResourcesToBeDeleted = stack.getResources().stream().filter(resource -> null != resource.getInstanceGroup()
                            && resource.getInstanceGroup().equals(requestGroup)
                            && resource.getResourceType().equals(ResourceType.AWS_VOLUMESET))
                    .map(s -> cloudResourceConverter.convert(s)).collect(toList());
            String cloudPlatform = stack.getCloudPlatform();
            try {
                deleteVolumesService.stopClouderaManagerService(stack, hostTemplateServiceComponents);
                return new DeleteVolumesRequest(cloudResourcesToBeDeleted, stackDeleteVolumesRequest, cloudPlatform,
                        hostTemplateServiceComponents);
            } catch (Exception ex) {
                LOGGER.error("Deleting volumes flow is being stopped, Exception while stopping CM services.");
                return new DeleteVolumesFailedEvent(
                        "CloudbreakException: Exception while stopping CM services",
                        new CloudbreakException("CloudbreakException: Exception while stopping CM services"),
                        stack.getId());
            }

        }
    }

    @Override
    public String selector() {
        return EventSelectorUtil.selector(DeleteVolumesValidationRequest.class);
    }
}
