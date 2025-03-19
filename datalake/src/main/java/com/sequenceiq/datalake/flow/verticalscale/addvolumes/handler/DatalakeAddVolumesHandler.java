package com.sequenceiq.datalake.flow.verticalscale.addvolumes.handler;

import static com.sequenceiq.datalake.flow.verticalscale.addvolumes.event.DatalakeAddVolumesStateSelectors.DATALAKE_ADD_VOLUMES_FINISH_EVENT;
import static com.sequenceiq.datalake.flow.verticalscale.addvolumes.event.DatalakeAddVolumesStateSelectors.DATALAKE_ADD_VOLUMES_HANDLER_EVENT;

import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackAddVolumesRequest;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.verticalscale.addvolumes.event.DatalakeAddVolumesEvent;
import com.sequenceiq.datalake.flow.verticalscale.addvolumes.event.DatalakeAddVolumesFailedEvent;
import com.sequenceiq.datalake.service.sdx.PollingConfig;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.datalake.service.sdx.flowcheck.CloudbreakFlowService;
import com.sequenceiq.datalake.service.sdx.flowwait.SdxWaitService;
import com.sequenceiq.distrox.api.v1.distrox.endpoint.DistroXV1Endpoint;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class DatalakeAddVolumesHandler extends ExceptionCatcherEventHandler<DatalakeAddVolumesEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatalakeAddVolumesHandler.class);

    private static final int SLEEP_INTERVAL = 30;

    private static final int DURATION_IN_MINUTES = 30;

    @Inject
    private SdxService sdxService;

    @Inject
    private SdxWaitService sdxWaitService;

    @Inject
    private DistroXV1Endpoint distroXV1Endpoint;

    @Inject
    private CloudbreakFlowService cloudbreakFlowService;

    @Override
    public String selector() {
        return DATALAKE_ADD_VOLUMES_HANDLER_EVENT.event();
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<DatalakeAddVolumesEvent> event) {
        return new DatalakeAddVolumesFailedEvent(resourceId, null, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<DatalakeAddVolumesEvent> event) {
        DatalakeAddVolumesEvent request = event.getData();
        LOGGER.debug("Starting add volumes for datalake with request: {}", request);
        Long sdxId = request.getResourceId();
        String userId = request.getUserId();
        StackAddVolumesRequest stackAddVolumesRequest = request.getStackAddVolumesRequest();
        try {
            SdxCluster sdxCluster = sdxService.getById(sdxId);
            String stackCrn = sdxCluster.getStackCrn();
            LOGGER.debug("Calling add_volumes with request :: {}", stackAddVolumesRequest);
            FlowIdentifier flowIdentifier = ThreadBasedUserCrnProvider.doAsInternalActor(
                    () -> {
                        try {
                            return distroXV1Endpoint.addVolumesByStackCrn(stackCrn, stackAddVolumesRequest);
                        } catch (Exception e) {
                            throw new CloudbreakServiceException(e);
                        }
                    });
            cloudbreakFlowService.saveLastCloudbreakFlowChainId(sdxCluster, flowIdentifier);
            LOGGER.debug("Starting polling for datalake-core add volumes flow :: {}", flowIdentifier);
            PollingConfig pollingConfig = new PollingConfig(SLEEP_INTERVAL, TimeUnit.SECONDS, DURATION_IN_MINUTES, TimeUnit.MINUTES);
            sdxWaitService.waitForCloudbreakFlow(sdxCluster.getId(), pollingConfig, "Polling add volumes flow");
            LOGGER.debug("Core add volumes flow for datalake is complete :: {}", flowIdentifier);
            return new DatalakeAddVolumesEvent(DATALAKE_ADD_VOLUMES_FINISH_EVENT.selector(), sdxId, userId, stackAddVolumesRequest, request.getSdxName());
        } catch (Exception e) {
            LOGGER.warn("Sdx add volumes failed, sdxId: {}", sdxId, e);
            return new DatalakeAddVolumesFailedEvent(sdxId, userId, e);
        }
    }
}