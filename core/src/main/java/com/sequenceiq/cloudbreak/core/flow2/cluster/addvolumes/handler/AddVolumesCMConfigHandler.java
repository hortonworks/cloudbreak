package com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.handler;

import java.util.List;
import java.util.Set;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.event.AddVolumesCMConfigFinishedEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.event.AddVolumesCMConfigHandlerEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.event.AddVolumesFailedEvent;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.service.ConfigUpdateUtilService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.template.model.ServiceComponent;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class AddVolumesCMConfigHandler extends ExceptionCatcherEventHandler<AddVolumesCMConfigHandlerEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AddVolumesCMConfigHandler.class);

    @Inject
    private ConfigUpdateUtilService configUpdateUtilService;

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private CmTemplateProcessorFactory cmTemplateProcessorFactory;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(AddVolumesCMConfigHandlerEvent.class);
    }

    @Override
    public Selectable doAccept(HandlerEvent<AddVolumesCMConfigHandlerEvent> addVolumesCMConfigHandlerEvent) {
        AddVolumesCMConfigHandlerEvent payload = addVolumesCMConfigHandlerEvent.getData();
        Long stackId = payload.getResourceId();
        String requestGroup = payload.getInstanceGroup();
        LOGGER.debug("Starting to update CM config after orchestration on group {}", payload.getInstanceGroup());
        Selectable response;
        try {
            StackDto stack = stackDtoService.getById(stackId);
            LOGGER.debug("Updating CM config!");
            String blueprintText = stack.getBlueprintJsonText();
            CmTemplateProcessor processor = cmTemplateProcessorFactory.get(blueprintText);
            Set<ServiceComponent> hostTemplateServiceComponents = processor.getServiceComponentsByHostGroup().get(requestGroup);
            List<String> hostTemplateRoleGroupNames = processor.getHostTemplateRoleNames(requestGroup);
            configUpdateUtilService.updateCMConfigsForComputeAndStartServices(stack, hostTemplateServiceComponents,
                    hostTemplateRoleGroupNames, requestGroup);
            response = new AddVolumesCMConfigFinishedEvent(stackId, requestGroup, payload.getNumberOfDisks(), payload.getType(),
                    payload.getSize(), payload.getCloudVolumeUsageType());
        } catch (Exception e) {
            LOGGER.error("Failed to add disks", e);
            response = new AddVolumesFailedEvent(stackId, e);
        }
        return response;
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<AddVolumesCMConfigHandlerEvent> event) {
        return new AddVolumesFailedEvent(resourceId, e);
    }
}