package com.sequenceiq.datalake.flow.sku.handler;

import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.sku.DataLakeSkuMigrationFailedEvent;
import com.sequenceiq.datalake.service.sdx.PollingConfig;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.datalake.service.sdx.flowwait.SdxWaitService;
import com.sequenceiq.datalake.service.sdx.flowwait.exception.SdxWaitException;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class WaitDataLakeSkuMigrationHandler extends ExceptionCatcherEventHandler<WaitDataLakeSkuMigrationRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(WaitDataLakeSkuMigrationHandler.class);

    @Value("${sdx.stack.migratesku.sleeptime-sec}")
    private int sleepTimeInSec;

    @Value("${sdx.stack.migratesku.duration-min}")
    private int durationInMinutes;

    @Inject
    private SdxWaitService sdxWaitService;

    @Inject
    private SdxService sdxService;

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<WaitDataLakeSkuMigrationRequest> event) {
        return new DataLakeSkuMigrationFailedEvent(resourceId, event.getData().getUserId(), e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<WaitDataLakeSkuMigrationRequest> event) {
        LOGGER.info("Start polling migrating skus for datalake, event: {}", event);
        WaitDataLakeSkuMigrationRequest request = event.getData();
        Long sdxId = request.getResourceId();
        String userId = request.getUserId();
        try {
            SdxCluster sdxCluster = sdxService.getById(sdxId);
            PollingConfig pollingConfig = new PollingConfig(sleepTimeInSec, TimeUnit.SECONDS, durationInMinutes, TimeUnit.MINUTES)
                    .withStopPollingIfExceptionOccurred(true);
            sdxWaitService.waitForCloudbreakFlow(sdxCluster, pollingConfig, "Migrating skus");
            LOGGER.info("Migrating Skus finished for SDX stack {}. Force: {}", sdxId, request.isForce());
            return new WaitDataLakeSkuMigrationResult(sdxId, userId);
        } catch (SdxWaitException e) {
            LOGGER.warn("Migrating skus failed for SDX stack {}", sdxId, e);
            return new DataLakeSkuMigrationFailedEvent(sdxId, userId, e);
        }
    }

    @Override
    public String selector() {
        return EventSelectorUtil.selector(WaitDataLakeSkuMigrationRequest.class);
    }
}
