package com.sequenceiq.cloudbreak.reactor.handler.cluster.upgrade.rds.validation;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.validation.ValidateRdsUpgradeBackupValidationRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.validation.ValidateRdsUpgradeBackupValidationResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.validation.ValidateRdsUpgradeFailedEvent;
import com.sequenceiq.cloudbreak.service.upgrade.rds.RdsUpgradeOrchestratorService;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class ValidateBackupSpaceHandler extends ExceptionCatcherEventHandler<ValidateRdsUpgradeBackupValidationRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ValidateBackupSpaceHandler.class);

    @Inject
    private RdsUpgradeOrchestratorService rdsUpgradeOrchestratorService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(ValidateRdsUpgradeBackupValidationRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<ValidateRdsUpgradeBackupValidationRequest> event) {
        LOGGER.error("Determination of backup location and validation of backup for RDS upgrade has failed", e);
        return new ValidateRdsUpgradeFailedEvent(resourceId, e, DetailedStackStatus.EXTERNAL_DATABASE_UPGRADE_VALIDATION_FAILED);
    }

    @Override
    public Selectable doAccept(HandlerEvent<ValidateRdsUpgradeBackupValidationRequest> event) {
        ValidateRdsUpgradeBackupValidationRequest request = event.getData();
        Long stackId = request.getResourceId();
        try {
            LOGGER.info("Determining backup location and validating if there is enough space for RDS backup");
            rdsUpgradeOrchestratorService.determineDbBackupLocation(stackId);
            return new ValidateRdsUpgradeBackupValidationResult(stackId);
        } catch (CloudbreakOrchestratorException e) {
            LOGGER.warn("Determining backup location and validating if there is enough space for RDS backup has failed.", e);
            return new ValidateRdsUpgradeFailedEvent(stackId, e, DetailedStackStatus.EXTERNAL_DATABASE_UPGRADE_VALIDATION_FAILED);
        }
    }
}
