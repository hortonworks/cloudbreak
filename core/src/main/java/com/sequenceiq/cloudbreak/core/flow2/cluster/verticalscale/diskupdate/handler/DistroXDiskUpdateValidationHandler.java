package com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.diskupdate.handler;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.DATAHUB_DISK_UPDATE_VALIDATION_FAILED;
import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.AWS;
import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.AZURE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.diskupdate.DistroXDiskUpdateHandlerSelectors.DATAHUB_DISK_UPDATE_VALIDATION_HANDLER_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.diskupdate.DistroXDiskUpdateStateSelectors.DATAHUB_DISK_UPDATE_EVENT;
import static com.sequenceiq.common.api.type.ResourceType.AWS_VOLUMESET;
import static com.sequenceiq.common.api.type.ResourceType.AZURE_VOLUMESET;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.DiskType;
import com.sequenceiq.cloudbreak.cloud.model.CloudVolumeUsageType;
import com.sequenceiq.cloudbreak.cloud.model.DiskTypes;
import com.sequenceiq.cloudbreak.cloud.model.Volume;
import com.sequenceiq.cloudbreak.cloud.model.VolumeParameterType;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.diskupdate.event.DistroXDiskUpdateEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.diskupdate.event.DistroXDiskUpdateFailedEvent;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.VerticalScalingValidatorService;
import com.sequenceiq.cloudbreak.service.datalake.DiskUpdateService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class DistroXDiskUpdateValidationHandler extends ExceptionCatcherEventHandler<DistroXDiskUpdateEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DistroXDiskUpdateValidationHandler.class);

    private static final Map<String, ResourceType> CLOUD_RESOURCE_TYPE_CONSTANTS = Map.of(
            AZURE.name(), AZURE_VOLUMESET,
            AWS.name(), AWS_VOLUMESET
    );

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private StackService stackService;

    @Inject
    private DiskUpdateService diskUpdateService;

    @Inject
    private VerticalScalingValidatorService verticalScalingValidatorService;

    @Override
    public String selector() {
        return DATAHUB_DISK_UPDATE_VALIDATION_HANDLER_EVENT.selector();
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<DistroXDiskUpdateEvent> event) {
        return new DistroXDiskUpdateFailedEvent(event.getData(), e, DATAHUB_DISK_UPDATE_VALIDATION_FAILED);
    }

    @Override
    public Selectable doAccept(HandlerEvent<DistroXDiskUpdateEvent> distroXDiskUpdateEvent) {
        LOGGER.debug("In DistroXDiskUpdateValidationHandler.accept");
        DistroXDiskUpdateEvent payload = distroXDiskUpdateEvent.getData();
        try {
            StackDto stackDto = stackDtoService.getById(payload.getResourceId());
            DiskTypes cloudPlatformDiskTypes = diskUpdateService.getDiskTypes(stackDto);
            List<VolumeSetAttributes.Volume> attachedVolumes = getAttachedVolumesList(
                    stackDto,
                    payload.getGroup(),
                    payload.getSize(),
                    payload.getVolumeType(),
                    payload.getDiskType(),
                    cloudPlatformDiskTypes
            );
            boolean requestedSizeGreaterThanAvailable = attachedVolumes.size() > 0;
            String cloudPlatform = stackDto.getCloudPlatform();
            boolean diskTypeChangeSupported = diskUpdateService.isDiskTypeChangeSupported(cloudPlatform);
            if (!requestedSizeGreaterThanAvailable || !diskTypeChangeSupported) {
                String exceptionMessage = !diskTypeChangeSupported ? String.format("Disk Type Change / Resize not supported for the %s platform",
                        cloudPlatform) : String.format("Requested disk size for %s group is less than current size", payload.getGroup());
                throw new CloudbreakException("Validation Failed: " + exceptionMessage);
            } else {
                List<Volume> volumesToBeUpdated = convertVolumeSetAttributesVolumesToVolumes(attachedVolumes, cloudPlatformDiskTypes);
                ValidationResult validationResult = verticalScalingValidatorService.validateAddVolumesRequest(
                        stackService.getByIdWithLists(payload.getResourceId()),
                        volumesToBeUpdated,
                        payload);
                if (validationResult.hasError()) {
                    throw new CloudbreakServiceException(validationResult.getFormattedErrors());
                }

                return DistroXDiskUpdateEvent.builder()
                        .withResourceId(payload.getResourceId())
                        .withSize(payload.getSize())
                        .withGroup(payload.getGroup())
                        .withDiskType(payload.getDiskType())
                        .withVolumeType(payload.getVolumeType())
                        .withClusterName(payload.getClusterName())
                        .withAccountId(payload.getAccountId())
                        .withSelector(DATAHUB_DISK_UPDATE_EVENT.selector())
                        .withVolumesToBeUpdated(volumesToBeUpdated)
                        .withCloudPlatform(stackDto.getCloudPlatform())
                        .withStackId(stackDto.getId())
                        .build();
            }
        } catch (Exception e) {
            LOGGER.warn("Validation of disk update failed on stack {}.", payload.getStackId(), e);
            return new DistroXDiskUpdateFailedEvent(payload, e, DATAHUB_DISK_UPDATE_VALIDATION_FAILED);
        }
    }

    private List<Volume> convertVolumeSetAttributesVolumesToVolumes(List<VolumeSetAttributes.Volume> attachedVolumes, DiskTypes diskTypes) {
        return attachedVolumes
                .stream()
                .filter(e -> !isEphemeral(diskTypes, e))
                .map(this::convertToVolume)
                .toList();
    }

    private Volume convertToVolume(VolumeSetAttributes.Volume volSetAttrVol) {
        Volume vol = new Volume("", volSetAttrVol.getType(), volSetAttrVol.getSize(), volSetAttrVol.getCloudVolumeUsageType());
        vol.setId(volSetAttrVol.getId());
        return vol;
    }

    private List<VolumeSetAttributes.Volume> getAttachedVolumesList(
            StackDto stack,
            String group,
            int size,
            String volumeType,
            String diskType,
            DiskTypes cloudPlatformDiskTypes) throws IOException {
        List<Resource> resources = stack.getResources().stream()
                .filter(res -> null != res.getInstanceId() && null != res.getInstanceGroup() && res.getInstanceGroup().equals(group)
                        && null != CLOUD_RESOURCE_TYPE_CONSTANTS.get(stack.getCloudPlatform())
                        && CLOUD_RESOURCE_TYPE_CONSTANTS.get(stack.getCloudPlatform()).equals(res.getResourceType())
                ).toList();
        List<VolumeSetAttributes.Volume> attachedVolumes = new ArrayList<>();
        for (Resource resource : resources) {
            LOGGER.debug("Checking if the attached volumes have size less than the requested volumes.");
            VolumeSetAttributes volumeSetAttributes = resource.getAttributes().get(VolumeSetAttributes.class);
            attachedVolumes.addAll(
                    volumeSetAttributes.getVolumes()
                            .stream()
                            .filter(getVolumePredicate(size, volumeType, diskType, cloudPlatformDiskTypes))
                            .toList()
            );
        }
        return attachedVolumes;
    }

    private Predicate<VolumeSetAttributes.Volume> getVolumePredicate(int size, String volumeType, String diskType, DiskTypes cloudPlatformDiskTypes) {
        return volume ->
                (isDataBaseDisk(diskType) && volume.getCloudVolumeUsageType().equals(CloudVolumeUsageType.DATABASE) && volume.getSize() < size)
                || (!isDataBaseDisk(diskType) && (volume.getSize() < size
                    || isEphemeral(cloudPlatformDiskTypes, volume)
                    || modificationNotRequired(volumeType, volume)));
    }

    private boolean isDataBaseDisk(String diskType) {
        return DiskType.DATABASE_DISK.name().equalsIgnoreCase(diskType);
    }

    private boolean modificationNotRequired(String volumeType, VolumeSetAttributes.Volume volume) {
        return null != volumeType
                && !volumeType.equalsIgnoreCase(volume.getType());
    }

    private boolean isEphemeral(DiskTypes diskTypes, VolumeSetAttributes.Volume volume) {
        return VolumeParameterType.EPHEMERAL.equals(diskTypes.diskMapping().get(volume.getType()));
    }
}
