package com.sequenceiq.datalake.flow.modifyselinux.handler;

import static com.sequenceiq.datalake.flow.modifyselinux.event.DatalakeModifySeLinuxStateSelectors.FAILED_MODIFY_SELINUX_DATALAKE_EVENT;
import static com.sequenceiq.datalake.flow.modifyselinux.event.DatalakeModifySeLinuxStateSelectors.FINISH_MODIFY_SELINUX_DATALAKE_EVENT;

import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.Promise;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.modifyselinux.event.DatalakeModifySeLinuxEvent;
import com.sequenceiq.datalake.flow.modifyselinux.event.DatalakeModifySeLinuxFailedEvent;
import com.sequenceiq.datalake.flow.modifyselinux.event.DatalakeModifySeLinuxHandlerEvent;
import com.sequenceiq.datalake.service.sdx.PollingConfig;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.datalake.service.sdx.flowcheck.CloudbreakFlowService;
import com.sequenceiq.datalake.service.sdx.flowwait.SdxWaitService;
import com.sequenceiq.distrox.api.v1.distrox.endpoint.DistroXV1Endpoint;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class DatalakeEnableSeLinuxHandler extends ExceptionCatcherEventHandler<DatalakeModifySeLinuxHandlerEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatalakeEnableSeLinuxHandler.class);

    private static final int SLEEP_INTERVAL_IN_SECONDS = 30;

    private static final int DURATION_IN_MINUTES = 30;

    @Inject
    private DistroXV1Endpoint distroXV1Endpoint;

    @Inject
    private CloudbreakFlowService cloudbreakFlowService;

    @Inject
    private SdxWaitService sdxWaitService;

    @Inject
    private SdxService sdxService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(DatalakeModifySeLinuxHandlerEvent.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<DatalakeModifySeLinuxHandlerEvent> event) {
        return new DatalakeModifySeLinuxFailedEvent(convertHandlerEventToDatalakeEnableSeLinuxEvent(event.getData()),
                e, DatalakeStatusEnum.DATALAKE_MODIFY_SELINUX_FAILED);
    }

    @Override
    public Selectable doAccept(HandlerEvent<DatalakeModifySeLinuxHandlerEvent> enableSeLinuxEventEvent) {
        DatalakeModifySeLinuxHandlerEvent event = enableSeLinuxEventEvent.getData();
        SdxCluster sdxCluster = sdxService.getById(event.getResourceId());
        LOGGER.debug("Triggering flow for enabling SELinux on stack.");
        FlowIdentifier flowIdentifier = ThreadBasedUserCrnProvider.doAsInternalActor(
                () -> distroXV1Endpoint.modifySeLinuxByCrn(event.getResourceCrn(), event.getSelinuxMode()));
        cloudbreakFlowService.saveLastCloudbreakFlowChainId(sdxCluster, flowIdentifier);
        LOGGER.debug("Starting polling for enabling SELinux on datalake stack - flow identifier :: {}", flowIdentifier);
        PollingConfig pollingConfig = new PollingConfig(SLEEP_INTERVAL_IN_SECONDS, TimeUnit.SECONDS, DURATION_IN_MINUTES, TimeUnit.MINUTES);
        sdxWaitService.waitForCloudbreakFlow(sdxCluster.getId(), pollingConfig, "Polling Resize flow");
        LOGGER.debug("Enabling SELinux on datalake flow is complete :: {}", flowIdentifier);
        return DatalakeModifySeLinuxEvent.builder()
                .withAccepted(new Promise<>())
                .withSelector(FINISH_MODIFY_SELINUX_DATALAKE_EVENT.selector())
                .withResourceCrn(event.getResourceCrn())
                .withResourceId(event.getResourceId())
                .withResourceName(event.getResourceName())
                .withSelinuxMode(event.getSelinuxMode())
                .build();
    }

    private DatalakeModifySeLinuxEvent convertHandlerEventToDatalakeEnableSeLinuxEvent(DatalakeModifySeLinuxHandlerEvent event) {
        return DatalakeModifySeLinuxEvent.builder().withResourceName(event.getResourceName()).withResourceCrn(event.getResourceCrn())
                .withResourceId(event.getResourceId()).withSelector(FAILED_MODIFY_SELINUX_DATALAKE_EVENT.event()).build();
    }
}
