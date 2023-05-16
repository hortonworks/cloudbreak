package com.sequenceiq.datalake.flow.verticalscale.diskupdate.handler;

import static com.sequenceiq.datalake.flow.verticalscale.diskupdate.event.DatalakeDiskUpdateStateSelectors.DATALAKE_DISK_UPDATE_HANDLER_EVENT;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.DiskUpdateEndpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.DiskModificationRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.Promise;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.verticalscale.diskupdate.event.DatalakeDiskUpdateEvent;
import com.sequenceiq.datalake.flow.verticalscale.diskupdate.event.DatalakeDiskUpdateFailedEvent;
import com.sequenceiq.datalake.flow.verticalscale.diskupdate.event.DatalakeDiskUpdateStateSelectors;
import com.sequenceiq.datalake.service.sdx.PollingConfig;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.datalake.service.sdx.flowcheck.CloudbreakFlowService;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.reactor.api.event.EventSender;
import com.sequenceiq.flow.reactor.api.handler.EventSenderAwareHandler;

@Component
public class DatalakeDiskUpdateHandler extends EventSenderAwareHandler<DatalakeDiskUpdateEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatalakeDiskUpdateHandler.class);

    private static final int SLEEP_INTERVAL = 30;

    private static final int DURATION_IN_MINUTES = 30;

    @Inject
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @Inject
    private CloudbreakFlowService cloudbreakFlowService;

    @Inject
    private StackV4Endpoint stackV4Endpoint;

    @Inject
    private SdxService sdxService;

    @Inject
    private DiskUpdateEndpoint diskUpdateEndpoint;

    public DatalakeDiskUpdateHandler(EventSender eventSender) {
        super(eventSender);
    }

    @Override
    public String selector() {
        return DATALAKE_DISK_UPDATE_HANDLER_EVENT.selector();
    }

    @Override
    public void accept(Event<DatalakeDiskUpdateEvent> datalakeDiskUpdateEventEvent) {
        LOGGER.debug("In DatalakeDiskUpdateHandler.accept");
        DatalakeDiskUpdateEvent payload = datalakeDiskUpdateEventEvent.getData();
        try {
            Long stackId = payload.getStackId();
            StackV4Response stackV4Response = stackV4Endpoint.getWithResources(1L, payload.getClusterName(), Set.of(), payload.getAccountId());
            SdxCluster sdxCluster = sdxService.getByNameAndStackId(stackV4Response.getCluster().getId(), stackV4Response.getCluster().getName());
            LOGGER.debug("Starting Disk Update for datalake.");
            DiskModificationRequest diskModificationRequest = new DiskModificationRequest();
            diskModificationRequest.setVolumesToUpdate(payload.getVolumesToBeUpdated());
            diskModificationRequest.setStackId(stackId);
            diskModificationRequest.setDiskUpdateRequest(payload.getDatalakeDiskUpdateRequest());
            LOGGER.debug("Calling updateDiskTypeAndSize with request :: {}", diskModificationRequest);
            ThreadBasedUserCrnProvider.doAsInternalActor(
                    regionAwareInternalCrnGeneratorFactory.sdxAdmin().getInternalCrnForServiceAsString(),
                    () -> {
                        try {
                            diskUpdateEndpoint.updateDiskTypeAndSize(diskModificationRequest);
                        } catch (Exception e) {
                            throw new CloudbreakServiceException(e);
                        }
                    });
            LOGGER.debug("Resizing Disks for datalake using orchestrator.");
            FlowIdentifier flowIdentifier = ThreadBasedUserCrnProvider.doAsInternalActor(
                            regionAwareInternalCrnGeneratorFactory.sdxAdmin().getInternalCrnForServiceAsString(),
                    () -> {
                        try {
                            return diskUpdateEndpoint.resizeDisks(stackId, payload.getDatalakeDiskUpdateRequest().getGroup());
                        } catch (Exception e) {
                            throw new CloudbreakServiceException(e);
                        }
                    });
            cloudbreakFlowService.saveLastCloudbreakFlowChainId(sdxCluster, flowIdentifier);
            LOGGER.debug("Starting polling for datalake orchestrator resizing flow :: {}", flowIdentifier);
            PollingConfig pollingConfig = new PollingConfig(SLEEP_INTERVAL, TimeUnit.SECONDS, DURATION_IN_MINUTES, TimeUnit.MINUTES);
            sdxService.waitCloudbreakClusterResize(sdxCluster.getId(), pollingConfig);
            LOGGER.debug("Disk Resize flow is complete :: {}", flowIdentifier);
            DatalakeDiskUpdateEvent datalakeDiskUpdateEvent = DatalakeDiskUpdateEvent.builder()
                .withAccepted(new Promise<>())
                .withResourceCrn(payload.getResourceCrn())
                .withResourceId(payload.getResourceId())
                .withResourceName(payload.getResourceName())
                .withDatalakeDiskUpdateRequest(payload.getDatalakeDiskUpdateRequest())
                .withStackCrn(payload.getStackCrn())
                .withClusterName(payload.getClusterName())
                .withAccountId(payload.getAccountId())
                .withSelector(DatalakeDiskUpdateStateSelectors.DATALAKE_DISK_UPDATE_FINISH_EVENT.selector())
                .withVolumesToBeUpdated(payload.getVolumesToBeUpdated())
                .withCloudPlatform(payload.getCloudPlatform())
                .withStackId(stackId)
                .build();
            eventSender().sendEvent(datalakeDiskUpdateEvent, datalakeDiskUpdateEventEvent.getHeaders());
        } catch (Exception ex) {
            DatalakeDiskUpdateFailedEvent failedEvent =
                    new DatalakeDiskUpdateFailedEvent(payload, ex, DatalakeStatusEnum.DATALAKE_DISK_UPDATE_FAILED);
            LOGGER.error("FAILED_DATALAKE_DISK_UPDATE_EVENT event sent with error: {} on stack {}", ex.getMessage(), payload.getStackCrn());
            eventSender().sendEvent(failedEvent, datalakeDiskUpdateEventEvent.getHeaders());
        }
    }
}
