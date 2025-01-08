package com.sequenceiq.cloudbreak.reactor.handler.cluster.upgrade.rds.validation;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.dyngr.exception.PollerException;
import com.dyngr.exception.PollerStoppedException;
import com.dyngr.exception.UserBreakException;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.externaldatabase.ExternalDatabaseService;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.validation.ValidateRdsUpgradeFailedEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.validation.WaitForValidateRdsUpgradeCleanupRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.validation.WaitForValidateRdsUpgradeCleanupResult;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class WaitForValidateRdsUpgradeCleanupHandler extends ExceptionCatcherEventHandler<WaitForValidateRdsUpgradeCleanupRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(WaitForValidateRdsUpgradeCleanupHandler.class);

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private ExternalDatabaseService externalDatabaseService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(WaitForValidateRdsUpgradeCleanupRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<WaitForValidateRdsUpgradeCleanupRequest> event) {
        LOGGER.error("Waiting for validating RDS upgrade on cloud provider side has failed", e);
        return new ValidateRdsUpgradeFailedEvent(resourceId, e, DetailedStackStatus.EXTERNAL_DATABASE_UPGRADE_VALIDATION_FAILED);
    }

    @Override
    public Selectable doAccept(HandlerEvent<WaitForValidateRdsUpgradeCleanupRequest> event) {
        WaitForValidateRdsUpgradeCleanupRequest request = event.getData();
        Long stackId = request.getResourceId();
        StackDto stack = stackDtoService.getById(stackId);
        try {
            LOGGER.info("Waiting for validation cleanup for RDS upgrade on cloud provider side");
            ClusterView cluster = stack.getCluster();

            if (request.getFlowIdentifier() != null) {
                externalDatabaseService.waitForDatabaseFlowToBeFinished(cluster, request.getFlowIdentifier());
            } else {
                LOGGER.info("No need to wait for RDS upgrade validation cleanup, as the flow identifier is null");
            }
            return createResponse(stackId, request);
        } catch (UserBreakException e) {
            LOGGER.error("Database 'upgrade validation cleanup' polling exited before timeout. Cause: ", e);
            return upgradeValidationFailedEvent(stackId, e);
        } catch (PollerStoppedException e) {
            LOGGER.error("Database 'upgrade validation cleanup' poller stopped for stack: {}", stack.getName(), e);
            return upgradeValidationFailedEvent(stackId, e);
        } catch (PollerException e) {
            LOGGER.error("Database 'upgrade validation cleanup' polling failed for stack: {}", stack.getName(), e);
            return upgradeValidationFailedEvent(stackId, e);
        } catch (Exception e) {
            LOGGER.error("Database 'upgrade validation cleanup' on cloud provider side has failed for stack: {}", stack.getName(), e);
            return upgradeValidationFailedEvent(stackId, e);
        }
    }

    private StackEvent upgradeValidationFailedEvent(Long stackId, Exception e) {
        return new ValidateRdsUpgradeFailedEvent(stackId, e, DetailedStackStatus.EXTERNAL_DATABASE_UPGRADE_VALIDATION_FAILED);
    }

    private Selectable createResponse(Long stackId, WaitForValidateRdsUpgradeCleanupRequest request) {
        String connectionErrorMessage = request.getValidateConnectionErrorMessage();
        Selectable response;
        if (StringUtils.isNotBlank(connectionErrorMessage)) {
            LOGGER.warn("Validation cleanup for RDS upgrade on cloud provider side has failed: {}", connectionErrorMessage);
            response = new ValidateRdsUpgradeFailedEvent(stackId,
                        new Exception(connectionErrorMessage), DetailedStackStatus.EXTERNAL_DATABASE_UPGRADE_VALIDATION_FAILED);
        } else {
            response = new WaitForValidateRdsUpgradeCleanupResult(stackId, null);
        }
        return response;
    }
}