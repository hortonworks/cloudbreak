package com.sequenceiq.cloudbreak.reactor.handler.cluster.upgrade.rds.validation;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.validation.ValidateRdsUpgradeCleanupRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.validation.ValidateRdsUpgradeCleanupResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.validation.ValidateRdsUpgradeFailedEvent;
import com.sequenceiq.cloudbreak.service.rdsconfig.RedbeamsClientService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.UpgradeDatabaseServerV4Response;

@Component
public class ValidateRdsUpgradeCleanupHandler extends ExceptionCatcherEventHandler<ValidateRdsUpgradeCleanupRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ValidateRdsUpgradeCleanupHandler.class);

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private RedbeamsClientService redbeamsClientService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(ValidateRdsUpgradeCleanupRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<ValidateRdsUpgradeCleanupRequest> event) {
        LOGGER.error("Validating RDS upgrade on cloud provider side has failed", e);
        return new ValidateRdsUpgradeFailedEvent(resourceId, e, DetailedStackStatus.EXTERNAL_DATABASE_UPGRADE_VALIDATION_FAILED);
    }

    @Override
    public Selectable doAccept(HandlerEvent<ValidateRdsUpgradeCleanupRequest> event) {
        ValidateRdsUpgradeCleanupRequest request = event.getData();
        Long stackId = request.getResourceId();
        StackDto stack = stackDtoService.getById(stackId);
        try {
            LOGGER.info("Validation cleanup for RDS upgrade on cloud provider side");
            ClusterView cluster = stack.getCluster();
            UpgradeDatabaseServerV4Response response = redbeamsClientService.validateUpgradeCleanup(cluster.getDatabaseServerCrn());
            return new ValidateRdsUpgradeCleanupResult(stackId, response.getFlowIdentifier());
        } catch (Exception e) {
            LOGGER.error("Database 'upgrade validation cleanup' on cloud provider side has failed for stack: {}", stack.getName(), e);
            return upgradeValidationFailedEvent(stackId, e);
        }
    }

    private StackEvent upgradeValidationFailedEvent(Long stackId, Exception e) {
        return new ValidateRdsUpgradeFailedEvent(stackId, e, DetailedStackStatus.EXTERNAL_DATABASE_UPGRADE_VALIDATION_FAILED);
    }
}