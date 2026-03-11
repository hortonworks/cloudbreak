package com.sequenceiq.datalake.flow.datalake.kraftmigration.handler;

import static com.sequenceiq.datalake.flow.datalake.kraftmigration.DatalakeKraftMigrationEvent.DATALAKE_KRAFT_MIGRATION_SUCCESS_EVENT;

import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.dyngr.exception.PollerException;
import com.dyngr.exception.PollerStoppedException;
import com.dyngr.exception.UserBreakException;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.datalake.flow.SdxEvent;
import com.sequenceiq.datalake.flow.datalake.kraftmigration.event.DatalakeKraftMigrationFailedEvent;
import com.sequenceiq.datalake.flow.datalake.kraftmigration.event.DatalakeKraftMigrationWaitRequest;
import com.sequenceiq.datalake.service.sdx.PollingConfig;
import com.sequenceiq.datalake.service.sdx.flowwait.SdxWaitService;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class DatalakeKraftMigrationWaitHandler extends ExceptionCatcherEventHandler<DatalakeKraftMigrationWaitRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatalakeKraftMigrationWaitHandler.class);

    @Value("${sdx.kraft.migration.polling.sleep.time.in.sec:20}")
    private int sleepTimeInSec;

    @Value("${sdx.kraft.migration.polling.duration.in.minutes:60}")
    private int durationInMinutes;

    @Inject
    private SdxWaitService sdxWaitService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(DatalakeKraftMigrationWaitRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<DatalakeKraftMigrationWaitRequest> event) {
        LOGGER.error("ZooKeeper to KRaft migration polling failed unexpectedly for DataLake id {}", resourceId, e);
        return new DatalakeKraftMigrationFailedEvent(resourceId, event.getData().getUserId(), e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<DatalakeKraftMigrationWaitRequest> event) {
        Long sdxId = event.getData().getResourceId();
        String userId = event.getData().getUserId();
        try {
            LOGGER.info("Start polling ZooKeeper to KRaft migration on Cloudbreak for DataLake id {}", sdxId);
            PollingConfig pollingConfig = new PollingConfig(sleepTimeInSec, TimeUnit.SECONDS, durationInMinutes, TimeUnit.MINUTES)
                    .withStopPollingIfExceptionOccurred(true);
            sdxWaitService.waitForCloudbreakFlow(sdxId, pollingConfig, "ZooKeeper to KRaft migration");
            return new SdxEvent(DATALAKE_KRAFT_MIGRATION_SUCCESS_EVENT.event(), sdxId, userId);
        } catch (UserBreakException userBreakException) {
            LOGGER.error("ZooKeeper to KRaft migration polling exited before timeout for DataLake id {}", sdxId, userBreakException);
            return new DatalakeKraftMigrationFailedEvent(sdxId, userId, userBreakException);
        } catch (PollerStoppedException pollerStoppedException) {
            LOGGER.error("ZooKeeper to KRaft migration poller stopped for DataLake id {}", sdxId);
            return new DatalakeKraftMigrationFailedEvent(sdxId, userId,
                    new PollerStoppedException("ZooKeeper to KRaft migration timed out after " + durationInMinutes + " minutes"));
        } catch (PollerException pollerException) {
            LOGGER.error("ZooKeeper to KRaft migration polling failed for DataLake id {}", sdxId);
            return new DatalakeKraftMigrationFailedEvent(sdxId, userId, pollerException);
        }
    }
}
