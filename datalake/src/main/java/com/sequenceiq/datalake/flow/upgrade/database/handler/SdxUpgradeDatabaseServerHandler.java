package com.sequenceiq.datalake.flow.upgrade.database.handler;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.upgrade.database.event.SdxUpgradeDatabaseServerFailedEvent;
import com.sequenceiq.datalake.flow.upgrade.database.event.SdxUpgradeDatabaseServerSuccessEvent;
import com.sequenceiq.datalake.flow.upgrade.database.event.UpgradeDatabaseServerRequest;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.datalake.service.upgrade.database.SdxDatabaseServerUpgradeService;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class SdxUpgradeDatabaseServerHandler extends ExceptionCatcherEventHandler<UpgradeDatabaseServerRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxUpgradeDatabaseServerHandler.class);

    @Inject
    private SdxDatabaseServerUpgradeService sdxDatabaseServerUpgradeService;

    @Inject
    private SdxService sdxService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(UpgradeDatabaseServerRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<UpgradeDatabaseServerRequest> event) {
        LOGGER.warn("Exception during upgrade of database server in SDX: ", e);
        return new SdxUpgradeDatabaseServerFailedEvent(resourceId, event.getData().getUserId(), e, "");
    }

    @Override
    protected Selectable doAccept(HandlerEvent<UpgradeDatabaseServerRequest> event) {
        LOGGER.debug("Entering upgrade of database server in SDX, event: {}", event);
        UpgradeDatabaseServerRequest request = event.getData();
        SdxCluster sdxCluster = sdxService.getById(request.getResourceId());
        Long sdxId = request.getResourceId();
        String userId = request.getUserId();
        try {
            sdxDatabaseServerUpgradeService.initUpgradeInCb(sdxCluster, request.getTargetMajorVersion(), request.isForced());
            LOGGER.debug("Database server upgrade was called successfully in core.");
            return new SdxUpgradeDatabaseServerSuccessEvent(sdxId, userId);
        } catch (Exception e) {
            LOGGER.warn("Calling database server upgrade failed in core: ", e);
            return new SdxUpgradeDatabaseServerFailedEvent(sdxId, userId, e, e.getMessage());
        }
    }

}
