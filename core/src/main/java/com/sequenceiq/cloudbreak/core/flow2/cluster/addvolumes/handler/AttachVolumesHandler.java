package com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.handler;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudVolumeStatus;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.cluster.util.ResourceAttributeUtil;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.type.TemporaryStorage;
import com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.event.AddVolumesFailedEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.event.AttachVolumesFinishedEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.event.AttachVolumesHandlerEvent;
import com.sequenceiq.cloudbreak.core.flow2.service.AddVolumesService;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.VolumeTemplate;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.cloudbreak.service.stack.InstanceGroupService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.template.TemplateService;
import com.sequenceiq.cloudbreak.view.InstanceGroupView;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class AttachVolumesHandler extends ExceptionCatcherEventHandler<AttachVolumesHandlerEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AttachVolumesHandler.class);

    @Inject
    private StackService stackService;

    @Inject
    private ResourceService resourceService;

    @Inject
    private AddVolumesService addVolumesService;

    @Inject
    private InstanceGroupService instanceGroupService;

    @Inject
    private TemplateService templateService;

    @Inject
    private ResourceAttributeUtil resourceAttributeUtil;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(AttachVolumesHandlerEvent.class);
    }

    @Override
    public Selectable doAccept(HandlerEvent<AttachVolumesHandlerEvent> attachVolumesHandlerEvent) {
        LOGGER.debug("Starting to add additional volumes on DiskUpdateService");
        AttachVolumesHandlerEvent payload = attachVolumesHandlerEvent.getData();
        Long stackId = payload.getResourceId();
        String instanceGroup = payload.getInstanceGroup();
        Set<Resource> resources = new HashSet<>();
        try {
            Stack stack = stackService.getById(stackId);
            resources = new HashSet<>(resourceService.findAllByStackIdAndInstanceGroupAndResourceTypeIn(stackId, instanceGroup,
                    List.of(stack.getDiskResourceType())));
            addVolumesService.attachVolumes(resources, stackId);
            addVolumesService.updateResourceVolumeStatus(resources, CloudVolumeStatus.ATTACHED);
            saveUpdatedTemplate(stackId, payload, resources);
            LOGGER.info("Successfully attached volumes from request {} to all instances", payload);
            return new AttachVolumesFinishedEvent(stackId, payload.getNumberOfDisks(), payload.getType(), payload.getSize(),
                    payload.getCloudVolumeUsageType(), payload.getInstanceGroup());
        } catch (Exception e) {
            if (!resources.isEmpty()) {
                LOGGER.warn("Rolling back created volumes because attachment failed. Resources - {}", resources);
                try {
                    addVolumesService.rollbackCreatedVolumes(resources, stackId);
                } catch (Exception ex) {
                    LOGGER.warn("Exception while detaching and deleting orphaned volumes.", ex);
                }
            }
            LOGGER.warn("Failed to attach disks to the stack", e);
            return new AddVolumesFailedEvent(stackId, e);
        }
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<AttachVolumesHandlerEvent> event) {
        return new AddVolumesFailedEvent(resourceId, e);
    }

    private void saveUpdatedTemplate(Long stackId, AttachVolumesHandlerEvent payload, Set<Resource> updatedResources) {
        Optional<InstanceGroupView> optionalGroup = instanceGroupService
                .findInstanceGroupViewByStackIdAndGroupName(stackId, payload.getInstanceGroup());
        if (optionalGroup.isPresent()) {
            InstanceGroupView instanceGroup = optionalGroup.get();
            Template template = instanceGroup.getTemplate();
            if (template.getTemporaryStorage().equals(TemporaryStorage.EPHEMERAL_VOLUMES_ONLY)) {
                LOGGER.info("Template update for temporary storage from EPHEMERAL_VOLUMES_ONLY to EPHEMERAL_VOLUMES");
                template.setTemporaryStorage(TemporaryStorage.EPHEMERAL_VOLUMES);
            }
            int attachedNumberOfDisksFromResources = getAttachedNumberOfDisksFromResources(payload, updatedResources);
            boolean savedVolume = false;
            for (VolumeTemplate volumeTemplateInTheDatabase : template.getVolumeTemplates()) {
                if (volumeTemplateInTheDatabase.getVolumeSize() == payload.getSize().intValue() &&
                        volumeTemplateInTheDatabase.getVolumeType().equals(payload.getType())) {
                    LOGGER.info("Template update for attached volumes count from {} to {}", volumeTemplateInTheDatabase.getVolumeCount(),
                            attachedNumberOfDisksFromResources);
                    volumeTemplateInTheDatabase.setVolumeCount(attachedNumberOfDisksFromResources);
                    savedVolume = true;
                }
            }
            if (!savedVolume) {
                VolumeTemplate volumeTemplate = new VolumeTemplate();
                volumeTemplate.setVolumeCount(attachedNumberOfDisksFromResources);
                volumeTemplate.setVolumeSize(payload.getSize().intValue());
                if (StringUtils.isNotBlank(payload.getType())) {
                    volumeTemplate.setVolumeType(payload.getType());
                }
                volumeTemplate.setTemplate(template);
                template.getVolumeTemplates().add(volumeTemplate);
                LOGGER.info("Added new Volume Template with volume count - {}", attachedNumberOfDisksFromResources);
            }
            templateService.savePure(template);
        }
    }

    private int getAttachedNumberOfDisksFromResources(AttachVolumesHandlerEvent payload, Set<Resource> updatedResources) {
        LOGGER.debug("Getting the actual count of attached volumes from one of the instances in resources table, as the payload count may be wrong " +
                "if the flow errored out during attach step. Updated resources here will only have affected group instances.");
        Optional<VolumeSetAttributes> volumeSetOptional = resourceAttributeUtil.getTypedAttributes(updatedResources.stream().findFirst().get(),
                VolumeSetAttributes.class);
        int count = 0;
        if (volumeSetOptional.isPresent()) {
            VolumeSetAttributes volumeSet = volumeSetOptional.get();
            List<VolumeSetAttributes.Volume> volumes = volumeSet.getVolumes();
            for (VolumeSetAttributes.Volume volume : volumes) {
                if (volume.getSize() == payload.getSize().intValue() && volume.getType().equals(payload.getType())) {
                    count++;
                }
            }
        }
        LOGGER.info("Returning the total count of volumes attached which are of same size and type as requested, count - {}", count);
        return count;
    }
}