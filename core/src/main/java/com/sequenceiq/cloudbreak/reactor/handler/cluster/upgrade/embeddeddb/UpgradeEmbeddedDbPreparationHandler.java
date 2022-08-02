package com.sequenceiq.cloudbreak.reactor.handler.cluster.upgrade.embeddeddb;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.rds.upgrade.UpgradeRdsService;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.embeddeddb.UpgradeEmbeddedDbPreparationFailedEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.embeddeddb.UpgradeEmbeddedDbPrepareRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.embeddeddb.UpgradeEmbeddedDbPrepareResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.UpgradeRdsFailedEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.UpgradeRdsUpgradeDatabaseServerRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.UpgradeRdsUpgradeDatabaseServerResult;
import com.sequenceiq.cloudbreak.service.upgrade.rds.RdsUpgradeOrchestratorService;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

import reactor.bus.Event;

@Component
public class UpgradeEmbeddedDbPreparationHandler extends ExceptionCatcherEventHandler<UpgradeEmbeddedDbPrepareRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpgradeEmbeddedDbPreparationHandler.class);

    @Inject
    private UpgradeRdsService upgradeRdsService;

    @Inject
    private RdsUpgradeOrchestratorService rdsUpgradeOrchestratorService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(UpgradeEmbeddedDbPrepareRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<UpgradeEmbeddedDbPrepareRequest> event) {
        LOGGER.error("Embedded database upgrade preparation has failed", e);
        return new UpgradeEmbeddedDbPreparationFailedEvent(resourceId, e, DetailedStackStatus.AVAILABLE);
    }

    @Override
    public Selectable doAccept(HandlerEvent<UpgradeEmbeddedDbPrepareRequest> event) {
        try {
            UpgradeEmbeddedDbPrepareRequest request = event.getData();
            Long stackId = request.getResourceId();
            LOGGER.info("Starting embedded database preparation for upgrade...");
            rdsUpgradeOrchestratorService.prepareUpgradeEmbeddedDatabase(stackId);
            return new UpgradeEmbeddedDbPrepareResult(stackId, request.getVersion());
        } catch (CloudbreakOrchestratorException e) {
            e.printStackTrace();
            return null;
        }
    }
}
