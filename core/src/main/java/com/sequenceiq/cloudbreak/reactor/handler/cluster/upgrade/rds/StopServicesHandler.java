package com.sequenceiq.cloudbreak.reactor.handler.cluster.upgrade.rds;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.UpgradeRdsFailedEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.UpgradeRdsStopServicesRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.UpgradeRdsStopServicesResult;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

import reactor.bus.Event;

@Component
public class StopServicesHandler extends ExceptionCatcherEventHandler<UpgradeRdsStopServicesRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StopServicesHandler.class);

    @Override
    public String selector() {
        return EventSelectorUtil.selector(UpgradeRdsStopServicesRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<UpgradeRdsStopServicesRequest> event) {
        LOGGER.error("Stopping services for RDS upgrade has failed", e);
        return new UpgradeRdsFailedEvent(resourceId, e, DetailedStackStatus.DATABASE_UPGRADE_FAILED);
    }

    @Override
    public Selectable doAccept(HandlerEvent<UpgradeRdsStopServicesRequest> event) {
        UpgradeRdsStopServicesRequest request = event.getData();
        Long stackId = request.getResourceId();
        LOGGER.info("Stopping services for RDS upgrade...");
        // TODO: Implement
        return new UpgradeRdsStopServicesResult(stackId, request.getVersion());
    }
}
