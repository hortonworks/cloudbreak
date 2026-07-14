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
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.UpgradeRdsGetLatestCertsRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.UpgradeRdsGetLatestCertsResult;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class GetLatestRdsCertsHandler extends ExceptionCatcherEventHandler<UpgradeRdsGetLatestCertsRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(GetLatestRdsCertsHandler.class);

    @Inject
    private RotateRdsCertificateService rotateRdsCertificateService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(UpgradeRdsGetLatestCertsRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<UpgradeRdsGetLatestCertsRequest> event) {
        LOGGER.error("Fetching the latest RDS SSL certificate during RDS upgrade has failed", e);
        return new UpgradeRdsFailedEvent(resourceId, e, DetailedStackStatus.DATABASE_UPGRADE_FAILED);
    }

    @Override
    public Selectable doAccept(HandlerEvent<UpgradeRdsGetLatestCertsRequest> event) {
        UpgradeRdsGetLatestCertsRequest request = event.getData();
        Long stackId = request.getResourceId();
        LOGGER.info("Fetching the latest RDS SSL certificate bundle during RDS upgrade for stack {}", stackId);
        rotateRdsCertificateService.getLatestRdsCertificate(stackId);
        return new UpgradeRdsGetLatestCertsResult(stackId, request.getVersion());
    }
}
