package com.sequenceiq.cloudbreak.reactor.handler.cluster.upgrade.rds;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.UpgradeRdsFailedEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.UpgradeRdsStartServicesRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.UpgradeRdsStartServicesResult;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

import reactor.bus.Event;

@Component
public class StartServicesHandler extends ExceptionCatcherEventHandler<UpgradeRdsStartServicesRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StartServicesHandler.class);

    @Override
    public String selector() {
        return EventSelectorUtil.selector(UpgradeRdsStartServicesRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<UpgradeRdsStartServicesRequest> event) {
        LOGGER.error("Starting services for RDS upgrade has failed", e);
        return new UpgradeRdsFailedEvent(resourceId, e, DetailedStackStatus.EXTERNAL_DATABASE_UPGRADE_FAILED);
    }

    @Override
    public Selectable doAccept(HandlerEvent<UpgradeRdsStartServicesRequest> event) {
        UpgradeRdsStartServicesRequest request = event.getData();
        Long stackId = request.getResourceId();
        LOGGER.info("Starting services for RDS upgrade...");
        // TODO: Implement
        return new UpgradeRdsStartServicesResult(stackId, request.getVersion());
    }
}
