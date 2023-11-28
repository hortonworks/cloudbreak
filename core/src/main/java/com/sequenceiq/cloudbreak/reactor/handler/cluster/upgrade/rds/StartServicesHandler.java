package com.sequenceiq.cloudbreak.reactor.handler.cluster.upgrade.rds;

import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_POSTGRES_UPGRADE_SKIP_SERVICE_STOP;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.rds.upgrade.UpgradeRdsService;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.UpgradeRdsFailedEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.UpgradeRdsStartCMServicesRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.UpgradeRdsStartCMServicesResult;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class StartServicesHandler extends ExceptionCatcherEventHandler<UpgradeRdsStartCMServicesRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StartServicesHandler.class);

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private ClusterApiConnectors clusterApiConnectors;

    @Inject
    private UpgradeRdsService upgradeRdsService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(UpgradeRdsStartCMServicesRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<UpgradeRdsStartCMServicesRequest> event) {
        LOGGER.error("Starting services for RDS upgrade has failed", e);
        return new UpgradeRdsFailedEvent(resourceId, e, DetailedStackStatus.DATABASE_UPGRADE_FAILED);
    }

    @Override
    public Selectable doAccept(HandlerEvent<UpgradeRdsStartCMServicesRequest> event) {
        UpgradeRdsStartCMServicesRequest request = event.getData();
        Long stackId = request.getResourceId();
        StackDto stackDto = stackDtoService.getById(stackId);
        StackView stack = stackDto.getStack();
        try {
            if (upgradeRdsService.shouldStopStartServices(stack)) {
                LOGGER.info("Starting services after RDS upgrade...");
                clusterApiConnectors.getConnector(stackDto).startCluster();
            } else {
                LOGGER.info("Skip starting services as {} entitlement is enabled.", CDP_POSTGRES_UPGRADE_SKIP_SERVICE_STOP);
            }
        } catch (Exception ex) {
            LOGGER.warn("Start services has failed", ex);
            return new UpgradeRdsFailedEvent(stackId, ex, DetailedStackStatus.DATABASE_UPGRADE_FAILED);
        }
        return new UpgradeRdsStartCMServicesResult(stackId, request.getVersion());
    }
}
