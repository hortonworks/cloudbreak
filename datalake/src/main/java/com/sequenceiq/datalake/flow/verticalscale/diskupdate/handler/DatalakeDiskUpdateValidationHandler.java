package com.sequenceiq.datalake.flow.verticalscale.diskupdate.handler;

import static com.sequenceiq.datalake.flow.verticalscale.diskupdate.event.DatalakeDiskUpdateStateSelectors.DATALAKE_DISK_UPDATE_VALIDATION_HANDLER_EVENT;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.DiskUpdateEndpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.DiskUpdateRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.resource.ResourceType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.resource.ResourceV4Response;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.cloud.model.Volume;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.Promise;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.flow.verticalscale.diskupdate.event.DatalakeDiskUpdateEvent;
import com.sequenceiq.datalake.flow.verticalscale.diskupdate.event.DatalakeDiskUpdateFailedEvent;
import com.sequenceiq.datalake.flow.verticalscale.diskupdate.event.DatalakeDiskUpdateStateSelectors;
import com.sequenceiq.flow.reactor.api.event.EventSender;
import com.sequenceiq.flow.reactor.api.handler.EventSenderAwareHandler;

@Component
public class DatalakeDiskUpdateValidationHandler extends EventSenderAwareHandler<DatalakeDiskUpdateEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatalakeDiskUpdateValidationHandler.class);

    private static final long WORKSPACE_ID = 0L;

    @Inject
    private StackV4Endpoint stackV4Endpoint;

    @Inject
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @Inject
    private DiskUpdateEndpoint diskUpdateEndpoint;

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
            StackV4Response stackV4Response = stackV4Endpoint.getWithResources(WORKSPACE_ID, payload.getClusterName(), Set.of(), payload.getAccountId());
            DiskUpdateRequest diskUpdateRequest = payload.getDatalakeDiskUpdateRequest();
            List<ResourceV4Response> resourcesList = stackV4Response.getResources().stream()
                    .filter(res -> null != res.getInstanceId() && res.getInstanceGroup().equals(diskUpdateRequest.getGroup())
                            && res.getResourceType().equals(ResourceType.AWS_VOLUMESET)).toList();
            ResourceV4Response resource = CollectionUtils.isEmpty(resourcesList) ? new ResourceV4Response() : resourcesList.get(0);
            VolumeSetAttributes volumeSetAttributes = new Json(resource.getAttributes()).get(VolumeSetAttributes.class);
            LOGGER.debug("Checking if the attached volumes have size less than the requested volumes.");
            List<VolumeSetAttributes.Volume> attachedVolumes = volumeSetAttributes.getVolumes().stream()
                    .filter(volume -> volume.getSize() < diskUpdateRequest.getSize()).collect(Collectors.toList());
            boolean requestedSizeGreaterThanAvailable = attachedVolumes.size() > 0;
            boolean diskTypeChangeSupported = ThreadBasedUserCrnProvider.doAsInternalActor(
                    regionAwareInternalCrnGeneratorFactory.sdxAdmin().getInternalCrnForServiceAsString(),
                    () -> diskUpdateEndpoint.isDiskTypeChangeSupported(stackV4Response.getCloudPlatform().toString()));
            if (!requestedSizeGreaterThanAvailable || !diskTypeChangeSupported) {
                String exceptionMessage = !diskTypeChangeSupported ? "Disk Type Change/ Resize not supported for the requested platform" :
                        String.format("Requested size for %s group is lesser than available", diskUpdateRequest.getGroup());
                Exception ex = new Exception("Validation Failed: " + exceptionMessage);
                DatalakeDiskUpdateFailedEvent failedEvent =
                        new DatalakeDiskUpdateFailedEvent(payload, ex, DatalakeStatusEnum.DATALAKE_DISK_UPDATE_VALIDATION_FAILED);
                LOGGER.error("Disk update failed with validation error: {} on stack {}", ex.getMessage(), payload.getStackCrn());
                eventSender().sendEvent(failedEvent, datalakeDiskUpdateEvent.getHeaders());
            } else {
                List<Volume> volumesToBeUpdated = convertVolumeSetAttributesVolumesToVolumes(attachedVolumes);
                DatalakeDiskUpdateEvent diskUpdateEvent = DatalakeDiskUpdateEvent.builder()
                        .withAccepted(new Promise<>())
                        .withResourceCrn(payload.getResourceCrn())
                        .withResourceId(payload.getResourceId())
                        .withResourceName(payload.getResourceName())
                        .withDatalakeDiskUpdateRequest(payload.getDatalakeDiskUpdateRequest())
                        .withStackCrn(payload.getStackCrn())
                        .withClusterName(payload.getClusterName())
                        .withAccountId(payload.getAccountId())
                        .withSelector(DatalakeDiskUpdateStateSelectors.DATALAKE_DISK_UPDATE_EVENT.selector())
                        .withVolumesToBeUpdated(volumesToBeUpdated)
                        .withCloudPlatform(stackV4Response.getCloudPlatform().toString())
                        .withStackId(stackV4Response.getId())
                        .build();
                eventSender().sendEvent(diskUpdateEvent, datalakeDiskUpdateEvent.getHeaders());
            }
        } catch (Exception e) {
            DatalakeDiskUpdateFailedEvent failedEvent =
                    new DatalakeDiskUpdateFailedEvent(payload, e, DatalakeStatusEnum.DATALAKE_DISK_UPDATE_VALIDATION_FAILED);
            LOGGER.error("Parsing attributes JSON in Validation of disk update failed: {} on stack {}, because: {}", e.getMessage(),
                    payload.getStackCrn(), e.getMessage());
            eventSender().sendEvent(failedEvent, datalakeDiskUpdateEvent.getHeaders());
        }
    }

    private List<Volume> convertVolumeSetAttributesVolumesToVolumes(List<VolumeSetAttributes.Volume> attachedVolumes) {
        return attachedVolumes.stream().map(volSetAttrVol -> {
            Volume vol = new Volume("", volSetAttrVol.getType(), volSetAttrVol.getSize(), volSetAttrVol.getCloudVolumeUsageType());
            vol.setId(volSetAttrVol.getId());
            return vol;
        }).collect(Collectors.toList());
    }
}