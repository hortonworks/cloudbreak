package com.sequenceiq.datalake.flow.verticalscale.rootvolume.handler;

import static com.sequenceiq.datalake.flow.verticalscale.rootvolume.event.DatalakeRootVolumeUpdateStateSelectors.DATALAKE_ROOT_VOLUME_UPDATE_HANDLER_EVENT;

import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.DiskUpdateRequest;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.Promise;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.events.RootVolumeUpdateRequest;
import com.sequenceiq.datalake.flow.verticalscale.rootvolume.event.DatalakeRootVolumeUpdateEvent;
import com.sequenceiq.datalake.flow.verticalscale.rootvolume.event.DatalakeRootVolumeUpdateFailedEvent;
import com.sequenceiq.datalake.flow.verticalscale.rootvolume.event.DatalakeRootVolumeUpdateStateSelectors;
import com.sequenceiq.datalake.service.sdx.PollingConfig;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.datalake.service.sdx.flowcheck.CloudbreakFlowService;
import com.sequenceiq.datalake.service.sdx.flowwait.SdxWaitService;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class DatalakeRootVolumeUpdateHandler extends ExceptionCatcherEventHandler<DatalakeRootVolumeUpdateEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatalakeRootVolumeUpdateHandler.class);

    private static final int SLEEP_INTERVAL_IN_SECONDS = 30;

    private static final int DURATION_IN_MINUTES = 30;

    private static final Long WORKSPACE_ID = 0L;

    @Inject
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @Inject
    private CloudbreakFlowService cloudbreakFlowService;

    @Inject
    private SdxService sdxService;

    @Inject
    private SdxWaitService sdxWaitService;

    @Inject
    private StackV4Endpoint stackV4Endpoint;

    @Override
    public String selector() {
        return DATALAKE_ROOT_VOLUME_UPDATE_HANDLER_EVENT.selector();
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<DatalakeRootVolumeUpdateEvent> event) {
        return DatalakeRootVolumeUpdateFailedEvent.builder().withDatalakeRootVolumeUpdateEvent(event.getData())
                .withException(e)
                .withDatalakeStatus(DatalakeStatusEnum.DATALAKE_DISK_UPDATE_FAILED).build();
    }

    @Override
    public Selectable doAccept(HandlerEvent<DatalakeRootVolumeUpdateEvent> datalakeRootVolumeUpdateEventEvent) {
        LOGGER.debug("In DatalakeRootVolumeUpdateHandler.accept");
        DatalakeRootVolumeUpdateEvent payload = datalakeRootVolumeUpdateEventEvent.getData();
        Selectable response;
        try {
            Long stackId = payload.getStackId();
            SdxCluster sdxCluster = sdxService.getByNameInAccount(ThreadBasedUserCrnProvider.getUserCrn(), payload.getClusterName());
            RootVolumeUpdateRequest rootVolumeUpdateRequest = payload.getRootVolumeUpdateRequest();
            LOGGER.debug("Starting Disk Update for datalake.");
            LOGGER.debug("Calling updateRootVolumeByStackCrnInternal with request :: {}", rootVolumeUpdateRequest);
            FlowIdentifier flowIdentifier = ThreadBasedUserCrnProvider.doAsInternalActor(
                    regionAwareInternalCrnGeneratorFactory.sdxAdmin().getInternalCrnForServiceAsString(),
                    () -> {
                        DiskUpdateRequest diskUpdateRequest = new DiskUpdateRequest(rootVolumeUpdateRequest.getVolumeType(),
                                rootVolumeUpdateRequest.getSize(), rootVolumeUpdateRequest.getGroup(), rootVolumeUpdateRequest.getDiskType());
                        return stackV4Endpoint.updateRootVolumeByStackCrnInternal(WORKSPACE_ID, sdxCluster.getStackCrn(),
                                diskUpdateRequest, payload.getInitiatorUserCrn());
                    });
            cloudbreakFlowService.saveLastCloudbreakFlowChainId(sdxCluster, flowIdentifier);
            LOGGER.debug("Starting polling for datalake root disk update flow :: {}", flowIdentifier);
            PollingConfig pollingConfig = new PollingConfig(SLEEP_INTERVAL_IN_SECONDS, TimeUnit.SECONDS, DURATION_IN_MINUTES, TimeUnit.MINUTES);
            sdxWaitService.waitForCloudbreakFlow(sdxCluster.getId(), pollingConfig, "Polling Root Disk Update Flow");
            LOGGER.debug("Root Disk Update flow is complete :: {}", flowIdentifier);
            response = DatalakeRootVolumeUpdateEvent.builder()
                .withAccepted(new Promise<>())
                .withResourceCrn(payload.getResourceCrn())
                .withResourceId(payload.getResourceId())
                .withResourceName(payload.getResourceName())
                .withRootVolumeUpdateRequest(rootVolumeUpdateRequest)
                .withStackCrn(payload.getStackCrn())
                .withClusterName(payload.getClusterName())
                .withAccountId(payload.getAccountId())
                .withSelector(DatalakeRootVolumeUpdateStateSelectors.DATALAKE_ROOT_VOLUME_UPDATE_FINISH_EVENT.selector())
                .withCloudPlatform(payload.getCloudPlatform())
                .withStackId(stackId)
                .withInitiatorUserCrn(payload.getInitiatorUserCrn())
                .build();
        } catch (Exception ex) {
            LOGGER.error("FAILED_DATALAKE_ROOT_VOLUME_UPDATE_EVENT event sent with error: {} on stack {}", ex.getMessage(), payload.getStackCrn());
            response = DatalakeRootVolumeUpdateFailedEvent.builder()
                    .withDatalakeRootVolumeUpdateEvent(payload)
                    .withException(ex)
                    .withDatalakeStatus(DatalakeStatusEnum.DATALAKE_DISK_UPDATE_FAILED).build();
        }
        return response;
    }
}
