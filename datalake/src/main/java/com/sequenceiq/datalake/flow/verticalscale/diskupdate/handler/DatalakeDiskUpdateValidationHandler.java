package com.sequenceiq.datalake.flow.verticalscale.diskupdate.handler;

import static com.sequenceiq.datalake.flow.verticalscale.diskupdate.event.DatalakeDiskUpdateStateSelectors.DATALAKE_DISK_UPDATE_VALIDATION_HANDLER_EVENT;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.DiskUpdateRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.resource.ResourceV4Response;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.cloud.model.Volume;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.Promise;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.flow.verticalscale.diskupdate.event.DatalakeDiskUpdateEvent;
import com.sequenceiq.datalake.flow.verticalscale.diskupdate.event.DatalakeDiskUpdateFailedEvent;
import com.sequenceiq.datalake.flow.verticalscale.diskupdate.event.DatalakeDiskUpdateStateSelectors;
import com.sequenceiq.datalake.service.sdx.VerticalScaleService;
import com.sequenceiq.flow.reactor.api.event.EventSender;
import com.sequenceiq.flow.reactor.api.handler.EventSenderAwareHandler;

@Component
public class DatalakeDiskUpdateValidationHandler extends EventSenderAwareHandler<DatalakeDiskUpdateEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatalakeDiskUpdateValidationHandler.class);

    private static final long WORKSPACE_ID = 0L;

    private static final Map<CloudPlatform, ResourceType> CLOUD_RESOURCE_TYPE_CONSTANTS = Map.of(
            CloudPlatform.AZURE, ResourceType.AZURE_VOLUMESET,
            CloudPlatform.AWS, ResourceType.AWS_VOLUMESET
    );

    @Inject
    private StackV4Endpoint stackV4Endpoint;

    @Inject
    private VerticalScaleService verticalScaleService;

    public DatalakeDiskUpdateValidationHandler(EventSender eventSender) {
        super(eventSender);
    }

    @Override
    public String selector() {
        return DATALAKE_DISK_UPDATE_VALIDATION_HANDLER_EVENT.selector();
    }

    @Override
    public void accept(Event<DatalakeDiskUpdateEvent> datalakeDiskUpdateEvent) {
        LOGGER.debug("In DatalakeDiskUpdateValidationHandler.accept");
        DatalakeDiskUpdateEvent payload = datalakeDiskUpdateEvent.getData();
        try {
            StackV4Response stackV4Response = ThreadBasedUserCrnProvider.doAsInternalActor(
                    () -> stackV4Endpoint.getWithResources(WORKSPACE_ID, payload.getClusterName(), Set.of(), payload.getAccountId()));
            DiskUpdateRequest diskUpdateRequest = payload.getDatalakeDiskUpdateRequest();
            List<ResourceV4Response> resources = stackV4Response.getResources().stream()
                    .filter(res -> filterOutNonDiskResources(res, diskUpdateRequest, stackV4Response))
                    .toList();
            List<VolumeSetAttributes.Volume> attachedVolumes = new ArrayList<>();
            for (ResourceV4Response resource : resources) {
                LOGGER.debug("Checking if the attached volumes have size less than the requested volumes.");
                VolumeSetAttributes volumeSetAttributes = new Json(resource.getAttributes()).get(VolumeSetAttributes.class);
                attachedVolumes.addAll(
                        volumeSetAttributes.getVolumes().stream()
                        .filter(volume -> volume.getSize() < diskUpdateRequest.getSize())
                        .toList()
                );
            }
            boolean requestedSizeGreaterThanAvailable = attachedVolumes.size() > 0;
            boolean diskTypeChangeSupported = verticalScaleService.getDiskTypeChangeSupported(stackV4Response.getCloudPlatform().toString());
            if (!requestedSizeGreaterThanAvailable || !diskTypeChangeSupported) {
                String exceptionMessage = !diskTypeChangeSupported ? "Disk Type Change/ Resize not supported for the requested platform" :
                        String.format("Requested size for %s group is smaller than available", diskUpdateRequest.getGroup());
                Exception ex = new Exception("Validation Failed: " + exceptionMessage);
                DatalakeDiskUpdateFailedEvent failedEvent = DatalakeDiskUpdateFailedEvent.builder()
                        .withDatalakeDiskUpdateEvent(payload)
                        .withException(ex)
                        .withDatalakeStatus(DatalakeStatusEnum.DATALAKE_DISK_UPDATE_VALIDATION_FAILED).build();
                LOGGER.error("Disk update failed with validation error: {} on stack {}", ex.getMessage(), payload.getStackCrn());
                eventSender().sendEvent(failedEvent, datalakeDiskUpdateEvent.getHeaders());
            } else {
                List<Volume> volumesToBeUpdated = convertVolumeSetAttributesVolumesToVolumes(attachedVolumes);
                DatalakeDiskUpdateEvent diskUpdateEvent = DatalakeDiskUpdateEvent.builder()
                        .withAccepted(new Promise<>())
                        .withDatalakeDiskUpdateEvent(payload)
                        .withStackV4Response(stackV4Response)
                        .withSelector(DatalakeDiskUpdateStateSelectors.DATALAKE_DISK_UPDATE_EVENT.selector())
                        .withVolumesToBeUpdated(volumesToBeUpdated)
                        .build();
                eventSender().sendEvent(diskUpdateEvent, datalakeDiskUpdateEvent.getHeaders());
            }
        } catch (Exception e) {
            DatalakeDiskUpdateFailedEvent failedEvent = DatalakeDiskUpdateFailedEvent.builder()
                    .withDatalakeDiskUpdateEvent(payload)
                    .withException(e)
                    .withDatalakeStatus(DatalakeStatusEnum.DATALAKE_DISK_UPDATE_VALIDATION_FAILED).build();
            LOGGER.error("Exception while validating datalake disk update: {} on stack {}, because: {}", e.getMessage(),
                    payload.getStackCrn(), e.getMessage());
            eventSender().sendEvent(failedEvent, datalakeDiskUpdateEvent.getHeaders());
        }
    }

    private boolean filterOutNonDiskResources(ResourceV4Response res, DiskUpdateRequest diskUpdateRequest, StackV4Response stackV4Response) {
        return null != res.getInstanceId()
                && null != res.getInstanceGroup()
                && res.getInstanceGroup().equals(diskUpdateRequest.getGroup())
                && null != CLOUD_RESOURCE_TYPE_CONSTANTS.get(stackV4Response.getCloudPlatform())
                && CLOUD_RESOURCE_TYPE_CONSTANTS.get(stackV4Response.getCloudPlatform()).equals(res.getResourceType());
    }

    private List<Volume> convertVolumeSetAttributesVolumesToVolumes(List<VolumeSetAttributes.Volume> attachedVolumes) {
        return attachedVolumes.stream().map(volSetAttrVol -> {
            Volume vol = new Volume("", volSetAttrVol.getType(), volSetAttrVol.getSize(), volSetAttrVol.getCloudVolumeUsageType());
            vol.setId(volSetAttrVol.getId());
            return vol;
        }).collect(Collectors.toList());
    }
}
