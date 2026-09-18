package com.sequenceiq.datalake.flow.encryptionprofile.handler;

import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.SdxEvent;
import com.sequenceiq.datalake.service.sdx.PollingConfig;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.datalake.service.sdx.flowcheck.CloudbreakFlowService;
import com.sequenceiq.datalake.service.sdx.flowwait.SdxWaitService;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

abstract class AbstractSdxEncryptionProfileHandler<T extends SdxEvent> extends ExceptionCatcherEventHandler<T> {

    protected static final long WORKSPACE_ID_DEFAULT = 0L;

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractSdxEncryptionProfileHandler.class);

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

    protected abstract String operationName();

    protected abstract FlowIdentifier triggerOnStack(StackV4Endpoint stackV4Endpoint, SdxCluster sdxCluster, T event);

    protected abstract String successEventSelector();

    @Override
    protected Selectable doAccept(HandlerEvent<T> handlerEvent) {
        T event = handlerEvent.getData();
        Long sdxId = event.getResourceId();
        SdxCluster sdxCluster = sdxService.getById(sdxId);
        LOGGER.info("Triggering {} on stack for Data Lake: {}", operationName(), sdxId);
        FlowIdentifier flowIdentifier = ThreadBasedUserCrnProvider.doAsInternalActor(() -> triggerOnStack(stackV4Endpoint, sdxCluster, event));
        cloudbreakFlowService.saveLastCloudbreakFlowChainId(sdxCluster, flowIdentifier);
        LOGGER.info("Polling {} flow in core, flowIdentifier: {}", operationName(), flowIdentifier);
        PollingConfig pollingConfig = new PollingConfig(SLEEP_INTERVAL_IN_SECONDS, TimeUnit.SECONDS, DURATION_IN_MINUTES, TimeUnit.MINUTES);
        sdxWaitService.waitForCloudbreakFlow(sdxId, pollingConfig, operationName());
        LOGGER.info("{} on Data Lake stack completed, flowIdentifier: {}", operationName(), flowIdentifier);
        return new SdxEvent(successEventSelector(), sdxId, event.getUserId());
    }
}
