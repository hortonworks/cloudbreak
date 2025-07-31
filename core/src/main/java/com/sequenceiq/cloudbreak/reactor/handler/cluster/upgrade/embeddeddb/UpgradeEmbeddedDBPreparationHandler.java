package com.sequenceiq.cloudbreak.reactor.handler.cluster.upgrade.embeddeddb;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.embeddeddb.UpgradeEmbeddedDBPreparationFailedEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.embeddeddb.UpgradeEmbeddedDBPreparationRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.embeddeddb.UpgradeEmbeddedDBPreparationResult;
import com.sequenceiq.cloudbreak.service.upgrade.rds.RdsUpgradeOrchestratorService;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class UpgradeEmbeddedDBPreparationHandler extends ExceptionCatcherEventHandler<UpgradeEmbeddedDBPreparationRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(UpgradeEmbeddedDBPreparationHandler.class);

    @Inject
    private RdsUpgradeOrchestratorService rdsUpgradeOrchestratorService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(UpgradeEmbeddedDBPreparationRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<UpgradeEmbeddedDBPreparationRequest> event) {
        LOGGER.error("Embedded database upgrade preparation has failed", e);
        return new UpgradeEmbeddedDBPreparationFailedEvent(resourceId, e, DetailedStackStatus.AVAILABLE);
    }

    @Override
    public Selectable doAccept(HandlerEvent<UpgradeEmbeddedDBPreparationRequest> event) {
        UpgradeEmbeddedDBPreparationRequest request = event.getData();
        Long stackId = request.getResourceId();
        try {
            LOGGER.info("Checking that the attached db volume size is enough for db upgrade...");
            rdsUpgradeOrchestratorService.validateDbDirectorySpace(stackId);
            LOGGER.info("Starting embedded database preparation for upgrade...");
            rdsUpgradeOrchestratorService.prepareUpgradeEmbeddedDatabase(stackId, request.getVersion());
            return new UpgradeEmbeddedDBPreparationResult(stackId, request.getVersion());
        } catch (CloudbreakOrchestratorException e) {
            LOGGER.warn("Embedded database upgrade preparation has failed", e);
            return new UpgradeEmbeddedDBPreparationFailedEvent(stackId, e, DetailedStackStatus.AVAILABLE);
        }
    }
}
