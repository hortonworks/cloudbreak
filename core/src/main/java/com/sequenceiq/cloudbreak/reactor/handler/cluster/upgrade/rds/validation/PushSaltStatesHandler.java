package com.sequenceiq.cloudbreak.reactor.handler.cluster.upgrade.rds.validation;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.validation.ValidateRdsUpgradeFailedEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.validation.ValidateRdsUpgradePushSaltStatesRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.validation.ValidateRdsUpgradePushSaltStatesResult;
import com.sequenceiq.cloudbreak.service.upgrade.UpgradeOrchestratorService;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

import reactor.bus.Event;

@Component
public class PushSaltStatesHandler extends ExceptionCatcherEventHandler<ValidateRdsUpgradePushSaltStatesRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(PushSaltStatesHandler.class);

    @Inject
    private UpgradeOrchestratorService upgradeOrchestratorService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(ValidateRdsUpgradePushSaltStatesRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<ValidateRdsUpgradePushSaltStatesRequest> event) {
        LOGGER.error("Pushing Salt states has failed", e);
        return new ValidateRdsUpgradeFailedEvent(resourceId, e, DetailedStackStatus.EXTERNAL_DATABASE_UPGRADE_VALIDATION_FAILED);
    }

    @Override
    public Selectable doAccept(HandlerEvent<ValidateRdsUpgradePushSaltStatesRequest> event) {
        ValidateRdsUpgradePushSaltStatesRequest request = event.getData();
        Long stackId = request.getResourceId();
        LOGGER.info("Pushing Salt states");
        upgradeOrchestratorService.pushSaltState(stackId);
        return new ValidateRdsUpgradePushSaltStatesResult(stackId);
    }
}
