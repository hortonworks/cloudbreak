package com.sequenceiq.cloudbreak.reactor.handler.cluster.upgrade.rds;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.rds.cert.rotate.RotateRdsCertificateService;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.UpgradeRdsFailedEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.UpgradeRdsUpdateLatestCertsRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.UpgradeRdsUpdateLatestCertsResult;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class UpdateLatestRdsCertsHandler extends ExceptionCatcherEventHandler<UpgradeRdsUpdateLatestCertsRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateLatestRdsCertsHandler.class);

    @Inject
    private RotateRdsCertificateService rotateRdsCertificateService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(UpgradeRdsUpdateLatestCertsRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<UpgradeRdsUpdateLatestCertsRequest> event) {
        LOGGER.error("Pushing the latest RDS SSL certificate bundle during RDS upgrade has failed", e);
        return new UpgradeRdsFailedEvent(resourceId, e, DetailedStackStatus.DATABASE_UPGRADE_FAILED);
    }

    @Override
    public Selectable doAccept(HandlerEvent<UpgradeRdsUpdateLatestCertsRequest> event) {
        UpgradeRdsUpdateLatestCertsRequest request = event.getData();
        Long stackId = request.getResourceId();
        LOGGER.info("Pushing the latest RDS SSL certificate bundle during RDS upgrade for stack {}", stackId);
        rotateRdsCertificateService.updateLatestRdsCertificate(stackId);
        return new UpgradeRdsUpdateLatestCertsResult(stackId, request.getVersion());
    }
}
