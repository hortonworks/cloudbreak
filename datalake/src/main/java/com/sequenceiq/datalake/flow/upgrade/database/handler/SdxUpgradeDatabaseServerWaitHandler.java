package com.sequenceiq.datalake.flow.upgrade.database.handler;

import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.upgrade.database.event.SdxUpgradeDatabaseServerFailedEvent;
import com.sequenceiq.datalake.flow.upgrade.database.event.SdxUpgradeDatabaseServerWaitSuccessEvent;
import com.sequenceiq.datalake.flow.upgrade.database.event.UpgradeDatabaseServerWaitRequest;
import com.sequenceiq.datalake.service.sdx.PollingConfig;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.datalake.service.sdx.poller.PollerRunner;
import com.sequenceiq.datalake.service.sdx.poller.PollerRunnerResult;
import com.sequenceiq.datalake.service.upgrade.database.SdxDatabaseServerUpgradeService;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class SdxUpgradeDatabaseServerWaitHandler extends ExceptionCatcherEventHandler<UpgradeDatabaseServerWaitRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxUpgradeDatabaseServerWaitHandler.class);

    @Inject
    private SdxUpgradeDatabaseServerWaitParametersService waitParametersService;

    @Inject
    private SdxDatabaseServerUpgradeService sdxDatabaseServerUpgradeService;

    @Inject
    private SdxService sdxService;

    @Inject
    private PollerRunner pollerRunner;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(UpgradeDatabaseServerWaitRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<UpgradeDatabaseServerWaitRequest> event) {
        return new SdxUpgradeDatabaseServerFailedEvent(resourceId, event.getData().getUserId(), e, "");
    }

    @Override
    protected Selectable doAccept(HandlerEvent<UpgradeDatabaseServerWaitRequest> event) {
        UpgradeDatabaseServerWaitRequest request = event.getData();
        SdxCluster sdxCluster = sdxService.getById(request.getResourceId());
        Long sdxId = request.getResourceId();
        String userId = request.getUserId();
        PollingConfig pollingConfig = new PollingConfig(waitParametersService.getSleepTimeInSec(), TimeUnit.SECONDS,
                waitParametersService.getDurationInMinutes(), TimeUnit.MINUTES);
        PollerRunnerResult result = pollerRunner.run(pollingConfig,
                config -> upgradeDatabaseServerAndPoll(sdxCluster, config),
                "Upgrade database server",
                sdxCluster
        );
        LOGGER.debug("Result from pollerRunner for task upgrading the database server is {}", result);
        return result.isSuccess()
                ? new SdxUpgradeDatabaseServerWaitSuccessEvent(sdxId, userId)
                : new SdxUpgradeDatabaseServerFailedEvent(sdxId, userId, result.getException(), result.getMessage());
    }

    private void upgradeDatabaseServerAndPoll(SdxCluster sdxCluster, PollingConfig config) {
        LOGGER.debug("Waiting for database server upgrade for SDX: {}", sdxCluster.getName());
        sdxDatabaseServerUpgradeService.waitDatabaseUpgradeInCb(sdxCluster, config);
        sdxDatabaseServerUpgradeService.updateDatabaseServerEngineVersion(sdxCluster);
    }

}
