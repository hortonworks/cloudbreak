package com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.diskupdate.handler;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.DATAHUB_DISK_UPDATE_VALIDATION_FAILED;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.diskupdate.DistroXDiskUpdateHandlerSelectors.DATAHUB_DISK_UPDATE_VALIDATION_HANDLER_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.diskupdate.DistroXDiskUpdateStateSelectors.DATAHUB_DISK_UPDATE_EVENT;

import java.util.List;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.Volume;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.diskupdate.event.DistroXDiskUpdateEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.diskupdate.event.DistroXDiskUpdateFailedEvent;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.diskupdate.DiskUpdateService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class DistroXDiskUpdateValidationHandler extends ExceptionCatcherEventHandler<DistroXDiskUpdateEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DistroXDiskUpdateValidationHandler.class);

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private DiskUpdateService diskUpdateService;

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
            // Full validation now runs synchronously in the API path (CB-32862); these guards remain as defense-in-depth
            // and to compute the volumes carried on the event.
            List<Volume> volumesToBeUpdated = diskUpdateService.getVolumesToUpdate(
                    stackDto,
                    payload.getGroup(),
                    payload.getSize(),
                    payload.getVolumeType(),
                    payload.getDiskType()
            );
            String cloudPlatform = stackDto.getCloudPlatform();
            boolean diskTypeChangeSupported = diskUpdateService.isDiskTypeChangeSupported(cloudPlatform);
            if (volumesToBeUpdated.isEmpty() || !diskTypeChangeSupported) {
                String exceptionMessage = !diskTypeChangeSupported ? String.format("Disk Type Change / Resize not supported for the %s platform",
                        cloudPlatform) : String.format("Requested disk size for %s group is less than current size", payload.getGroup());
                throw new CloudbreakException("Validation Failed: " + exceptionMessage);
            } else {
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
}
