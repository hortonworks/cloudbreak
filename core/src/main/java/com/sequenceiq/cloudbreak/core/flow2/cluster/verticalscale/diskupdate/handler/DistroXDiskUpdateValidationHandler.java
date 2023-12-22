package com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.diskupdate.handler;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.DATAHUB_DISK_UPDATE_VALIDATION_FAILED;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.diskupdate.DistroXDiskUpdateHandlerSelectors.DATAHUB_DISK_UPDATE_VALIDATION_HANDLER_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.diskupdate.DistroXDiskUpdateStateSelectors.DATAHUB_DISK_UPDATE_EVENT;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.DiskUpdateRequest;
import com.sequenceiq.cloudbreak.cloud.model.Volume;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.diskupdate.event.DistroXDiskUpdateEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.diskupdate.event.DistroXDiskUpdateFailedEvent;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.datalake.DiskUpdateService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.flow.reactor.api.event.EventSender;
import com.sequenceiq.flow.reactor.api.handler.EventSenderAwareHandler;

@Component
public class DistroXDiskUpdateValidationHandler extends EventSenderAwareHandler<DistroXDiskUpdateEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DistroXDiskUpdateValidationHandler.class);

    private static final Map<String, ResourceType> CLOUD_RESOURCE_TYPE_CONSTANTS = Map.of(CloudPlatform.AZURE.name(), ResourceType.AZURE_VOLUMESET,
            CloudPlatform.AWS.name(), ResourceType.AWS_VOLUMESET);

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private DiskUpdateService diskUpdateService;

    public DistroXDiskUpdateValidationHandler(EventSender eventSender) {
        super(eventSender);
    }

    @Override
    public String selector() {
        return DATAHUB_DISK_UPDATE_VALIDATION_HANDLER_EVENT.selector();
    }

    @Override
    public void accept(Event<DistroXDiskUpdateEvent> distroXDiskUpdateEvent) {
        LOGGER.debug("In DistroXDiskUpdateValidationHandler.accept");
        DistroXDiskUpdateEvent payload = distroXDiskUpdateEvent.getData();
        try {
            StackDto stack = stackDtoService.getById(payload.getResourceId());
            DiskUpdateRequest diskUpdateRequest = payload.getDiskUpdateRequest();
            List<VolumeSetAttributes.Volume> attachedVolumes = getAttachedVolumesList(stack, diskUpdateRequest);
            boolean requestedSizeGreaterThanAvailable = attachedVolumes.size() > 0;
            String cloudPlatform = stack.getCloudPlatform();
            boolean diskTypeChangeSupported = diskUpdateService.isDiskTypeChangeSupported(cloudPlatform);
            if (!requestedSizeGreaterThanAvailable || !diskTypeChangeSupported) {
                String exceptionMessage = !diskTypeChangeSupported ? String.format("Disk Type Change / Resize not supported for the %s platform",
                        cloudPlatform) : String.format("Requested disk size for %s group is less than current size", diskUpdateRequest.getGroup());
                throw new CloudbreakException("Validation Failed: " + exceptionMessage);
            } else {
                List<Volume> volumesToBeUpdated = convertVolumeSetAttributesVolumesToVolumes(attachedVolumes);
                DistroXDiskUpdateEvent diskUpdateEvent = DistroXDiskUpdateEvent.builder()
                        .withResourceCrn(payload.getResourceCrn())
                        .withResourceId(payload.getResourceId())
                        .withDiskUpdateRequest(payload.getDiskUpdateRequest())
                        .withClusterName(payload.getClusterName())
                        .withAccountId(payload.getAccountId())
                        .withSelector(DATAHUB_DISK_UPDATE_EVENT.selector())
                        .withVolumesToBeUpdated(volumesToBeUpdated)
                        .withCloudPlatform(stack.getCloudPlatform())
                        .withStackId(stack.getId())
                        .build();
                eventSender().sendEvent(diskUpdateEvent, distroXDiskUpdateEvent.getHeaders());
            }
        } catch (Exception e) {
            DistroXDiskUpdateFailedEvent failedEvent =
                    new DistroXDiskUpdateFailedEvent(payload, e, DATAHUB_DISK_UPDATE_VALIDATION_FAILED);
            LOGGER.error("Validation of disk update failed on stack {}, because: {}", payload.getResourceCrn(), e.getMessage());
            eventSender().sendEvent(failedEvent, distroXDiskUpdateEvent.getHeaders());
        }
    }

    private List<Volume> convertVolumeSetAttributesVolumesToVolumes(List<VolumeSetAttributes.Volume> attachedVolumes) {
        return attachedVolumes.stream().map(volSetAttrVol -> {
            Volume vol = new Volume("", volSetAttrVol.getType(), volSetAttrVol.getSize(), volSetAttrVol.getCloudVolumeUsageType());
            vol.setId(volSetAttrVol.getId());
            return vol;
        }).collect(Collectors.toList());
    }

    private List<VolumeSetAttributes.Volume> getAttachedVolumesList(StackDto stack, DiskUpdateRequest diskUpdateRequest) throws IOException {
        List<Resource> resources = stack.getResources().stream()
                .filter(res -> null != res.getInstanceId() && null != res.getInstanceGroup() && res.getInstanceGroup().equals(diskUpdateRequest.getGroup())
                        && null != CLOUD_RESOURCE_TYPE_CONSTANTS.get(stack.getCloudPlatform())
                        && CLOUD_RESOURCE_TYPE_CONSTANTS.get(stack.getCloudPlatform()).equals(res.getResourceType())
                ).toList();
        List<VolumeSetAttributes.Volume> attachedVolumes = new ArrayList<>();
        for (Resource resource : resources) {
            LOGGER.debug("Checking if the attached volumes have size less than the requested volumes.");
            VolumeSetAttributes volumeSetAttributes = resource.getAttributes().get(VolumeSetAttributes.class);
            attachedVolumes.addAll(volumeSetAttributes.getVolumes().stream()
                    .filter(volume -> volume.getSize() < diskUpdateRequest.getSize()).toList());
        }
        return attachedVolumes;
    }
}
