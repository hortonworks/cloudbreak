package com.sequenceiq.cloudbreak.reactor.handler.cluster.upgrade.rds.validation;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseConnectionProperties;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.validation.ValidateRdsUpgradeConnectionRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.validation.ValidateRdsUpgradeConnectionResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.validation.ValidateRdsUpgradeFailedEvent;
import com.sequenceiq.cloudbreak.service.upgrade.rds.RdsUpgradeOrchestratorService;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class ValidateConnectionHandler extends ExceptionCatcherEventHandler<ValidateRdsUpgradeConnectionRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ValidateConnectionHandler.class);

    @Inject
    private RdsUpgradeOrchestratorService rdsUpgradeOrchestratorService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(ValidateRdsUpgradeConnectionRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<ValidateRdsUpgradeConnectionRequest> event) {
        LOGGER.error("Validation of connection for RDS upgrade has failed", e);
        return new ValidateRdsUpgradeFailedEvent(resourceId, e, DetailedStackStatus.EXTERNAL_DATABASE_UPGRADE_VALIDATION_FAILED);
    }

    @Override
    public Selectable doAccept(HandlerEvent<ValidateRdsUpgradeConnectionRequest> event) {
        ValidateRdsUpgradeConnectionRequest request = event.getData();
        Long stackId = request.getResourceId();
        try {
            LOGGER.info("Testing RDS connection...");
            DatabaseConnectionProperties canaryProperties = request.getCanaryProperties();
            rdsUpgradeOrchestratorService.validateDbConnection(stackId, canaryProperties.getConnectionUrl(), canaryProperties.getUsername());
            return new ValidateRdsUpgradeConnectionResult(stackId, null);
        } catch (CloudbreakOrchestratorException e) {
            LOGGER.warn("Testing RDS connection has failed, proceeding with canary RDS cleanup...", e);
            return new ValidateRdsUpgradeConnectionResult(stackId, e.getMessage());
        }
    }
}