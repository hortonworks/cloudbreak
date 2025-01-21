package com.sequenceiq.cloudbreak.core.flow2.cluster.deletevolumes.handler;

import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.Set;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.base.Enums;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackDeleteVolumesRequest;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.converter.spi.ResourceToCloudResourceConverter;
import com.sequenceiq.cloudbreak.core.flow2.cluster.deletevolumes.BlackListedDeleteVolumesRole;
import com.sequenceiq.cloudbreak.core.flow2.cluster.deletevolumes.DeleteVolumesValidationRequest;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.DeleteVolumesFailedEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.DeleteVolumesRequest;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.template.model.ServiceComponent;
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
        String blueprintText = stack.getBlueprint().getBlueprintJsonText();
        CmTemplateProcessor processor = cmTemplateProcessorFactory.get(blueprintText);
        Set<String> hostTemplateComponents = processor.getComponentsInHostGroup(requestGroup);
        Set<ServiceComponent> hostTemplateServiceComponents = processor.getServiceComponentsByHostGroup().get(requestGroup);
        boolean computeInstance = true;
        StringBuilder blackListedServices = new StringBuilder();
        for (String service : hostTemplateComponents) {
            com.google.common.base.Optional<BlackListedDeleteVolumesRole> enumValue =
                    Enums.getIfPresent(BlackListedDeleteVolumesRole.class, service);
            if (enumValue.isPresent()) {
                if (!blackListedServices.toString().isEmpty()) {
                    blackListedServices.append(", ");
                }
                blackListedServices.append(enumValue.get());
                computeInstance = false;
            }
        }
        if (!computeInstance) {
            LOGGER.warn("Deleting volumes flow is being stopped, as instance being scaled is not a compute instance.");
            String statusReason = "BadRequestException: Instance group being scaled isn't a compute instance. Non-compliant services: " +
                    blackListedServices.toString();
            return new DeleteVolumesFailedEvent(statusReason, new BadRequestException(statusReason), stack.getId());
        } else {
            List<CloudResource> cloudResourcesToBeDeleted = stack.getResources().stream().filter(resource -> null != resource.getInstanceGroup()
                            && resource.getInstanceGroup().equals(requestGroup)
                            && resource.getResourceType().name().contains("VOLUMESET"))
                    .map(s -> cloudResourceConverter.convert(s)).collect(toList());
            long numVolumesToDelete = cloudResourcesToBeDeleted.stream()
                    .map(this::getVolumeSetAttributes)
                    .map(VolumeSetAttributes::getVolumes)
                    .flatMap(List::stream)
                    .count();
            if (numVolumesToDelete == 0) {
                String errorMessage = String.format("BadRequestException: There are no volumes attached to %s instance group", requestGroup);
                LOGGER.warn(errorMessage);
                return new DeleteVolumesFailedEvent(
                        errorMessage,
                        new BadRequestException(errorMessage),
                        stack.getId());
            }
            String cloudPlatform = stack.getCloudPlatform();
            return new DeleteVolumesRequest(cloudResourcesToBeDeleted, stackDeleteVolumesRequest, cloudPlatform,
                        hostTemplateServiceComponents);
        }
    }

    private VolumeSetAttributes getVolumeSetAttributes(CloudResource volumeSet) {
        return volumeSet.getParameter(CloudResource.ATTRIBUTES, VolumeSetAttributes.class);
    }

    @Override
    public String selector() {
        return EventSelectorUtil.selector(DeleteVolumesValidationRequest.class);
    }
}
