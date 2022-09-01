package com.sequenceiq.datalake.flow.upgrade.database.handler;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.upgrade.database.event.SdxUpgradeDatabaseServerFailedEvent;
import com.sequenceiq.datalake.flow.upgrade.database.event.SdxUpgradeDatabaseServerSuccessEvent;
import com.sequenceiq.datalake.flow.upgrade.database.event.UpgradeDatabaseServerRequest;
import com.sequenceiq.datalake.service.sdx.PollingConfig;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.datalake.service.sdx.poller.PollerRunner;
import com.sequenceiq.datalake.service.sdx.poller.PollerRunnerResult;
import com.sequenceiq.datalake.service.upgrade.database.SdxDatabaseServerUpgradeService;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

import reactor.bus.Event;

@Component
public class UpgradeDatabaseServerHandler extends ExceptionCatcherEventHandler<UpgradeDatabaseServerRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpgradeDatabaseServerHandler.class);

    @Inject
    private UpgradeDatabaseServerWaitParametersService waitParametersService;

    @Inject
    private SdxDatabaseServerUpgradeService sdxDatabaseServerUpgradeService;

    @Inject
    private SdxService sdxService;

    @Inject
    private PollerRunner pollerRunner;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(UpgradeDatabaseServerRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<UpgradeDatabaseServerRequest> event) {
        return new SdxUpgradeDatabaseServerFailedEvent(resourceId, event.getData().getUserId(), e, "");
    }

    @Override
    protected Selectable doAccept(HandlerEvent<UpgradeDatabaseServerRequest> event) {
        UpgradeDatabaseServerRequest request = event.getData();
        SdxCluster sdxCluster = sdxService.getById(request.getResourceId());
        Long sdxId = request.getResourceId();
        String userId = request.getUserId();
        PollingConfig pollingConfig = new PollingConfig(waitParametersService.getSleepTimeInSec(), TimeUnit.SECONDS,
                waitParametersService.getDurationInMinutes(), TimeUnit.MINUTES);
        PollerRunnerResult result = pollerRunner.run(pollingConfig,
                config -> upgradeDatabaseServerAndPoll(request, sdxCluster, config),
                "Upgrade database server",
                sdxCluster
        );
        LOGGER.debug("Result from pollerRunner for task upgrading the database server is {}", result);
        return result.isSuccess()
                ? new SdxUpgradeDatabaseServerSuccessEvent(sdxId, userId)
                : new SdxUpgradeDatabaseServerFailedEvent(sdxId, userId, result.getException(), result.getMessage());
    }

    private void upgradeDatabaseServerAndPoll(UpgradeDatabaseServerRequest request, SdxCluster sdxCluster, PollingConfig config) {
        LOGGER.debug("Initiating database server upgrade for SDX: {}", sdxCluster.getName());
        sdxDatabaseServerUpgradeService.initUpgradeInCb(sdxCluster, request.getTargetMajorVersion());
        sdxDatabaseServerUpgradeService.waitDatabaseUpgradeInCb(sdxCluster, config);
        sdxDatabaseServerUpgradeService.updateDatabaseServerEngineVersion(sdxCluster);
    }

}
