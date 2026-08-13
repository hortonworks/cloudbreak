package com.sequenceiq.datalake.flow.encryptionprofile.handler;

import static com.sequenceiq.datalake.flow.encryptionprofile.SdxEnableEncryptionProfileEvent.SDX_ENABLE_ENCRYPTION_PROFILE_SUCCESS_EVENT;

import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.SdxEvent;
import com.sequenceiq.datalake.flow.encryptionprofile.event.SdxEnableEncryptionProfileFailedEvent;
import com.sequenceiq.datalake.flow.encryptionprofile.event.SdxEnableEncryptionProfileHandlerEvent;
import com.sequenceiq.datalake.service.sdx.PollingConfig;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.datalake.service.sdx.flowcheck.CloudbreakFlowService;
import com.sequenceiq.datalake.service.sdx.flowwait.SdxWaitService;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class SdxEnableEncryptionProfileHandler extends ExceptionCatcherEventHandler<SdxEnableEncryptionProfileHandlerEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxEnableEncryptionProfileHandler.class);

    private static final long WORKSPACE_ID_DEFAULT = 0L;

    private static final int SLEEP_INTERVAL_IN_SECONDS = 30;

    private static final int DURATION_IN_MINUTES = 30;

    @Inject
    private StackV4Endpoint stackV4Endpoint;

    @Inject
    private CloudbreakFlowService cloudbreakFlowService;

    @Inject
    private SdxWaitService sdxWaitService;

    @Inject
    private SdxService sdxService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(SdxEnableEncryptionProfileHandlerEvent.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<SdxEnableEncryptionProfileHandlerEvent> event) {
        LOGGER.warn("Exception during Enable Encryption Profile in SDX: ", e);
        return new SdxEnableEncryptionProfileFailedEvent(resourceId, event.getData().getUserId(), e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<SdxEnableEncryptionProfileHandlerEvent> handlerEvent) {
        SdxEnableEncryptionProfileHandlerEvent event = handlerEvent.getData();
        Long sdxId = event.getResourceId();
        String userId = event.getUserId();
        String encryptionProfileCrn = event.getEncryptionProfileCrn();
        SdxCluster sdxCluster = sdxService.getById(sdxId);
        LOGGER.info("Triggering Enable Encryption Profile on stack for SDX: {}, encryptionProfileCrn: {}", sdxId, encryptionProfileCrn);
        FlowIdentifier flowIdentifier = ThreadBasedUserCrnProvider.doAsInternalActor(
                () -> stackV4Endpoint.updateSslConfigurationsByCrn(WORKSPACE_ID_DEFAULT, sdxCluster.getCrn(), encryptionProfileCrn));
        cloudbreakFlowService.saveLastCloudbreakFlowChainId(sdxCluster, flowIdentifier);
        LOGGER.info("Polling Enable Encryption Profile flow in core, flowIdentifier: {}", flowIdentifier);
        PollingConfig pollingConfig = new PollingConfig(SLEEP_INTERVAL_IN_SECONDS, TimeUnit.SECONDS, DURATION_IN_MINUTES, TimeUnit.MINUTES);
        sdxWaitService.waitForCloudbreakFlow(sdxId, pollingConfig, "Enable encryption profile");
        LOGGER.info("Enable Encryption Profile on SDX stack completed, flowIdentifier: {}", flowIdentifier);
        return new SdxEvent(SDX_ENABLE_ENCRYPTION_PROFILE_SUCCESS_EVENT.event(), sdxId, userId);
    }
}
