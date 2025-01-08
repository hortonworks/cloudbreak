package com.sequenceiq.cloudbreak.reactor.handler.cluster.upgrade.rds.validation;

import java.util.Optional;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.dyngr.exception.PollerException;
import com.dyngr.exception.PollerStoppedException;
import com.dyngr.exception.UserBreakException;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseConnectionProperties;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.externaldatabase.ExternalDatabaseService;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.validation.ValidateRdsUpgradeFailedEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.validation.WaitForValidateRdsUpgradeOnCloudProviderRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.validation.WaitForValidateRdsUpgradeOnCloudProviderResult;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.CanaryDatabasePropertiesV4Response;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerV4Response;

@Component
public class WaitForValidateRdsUpgradeOnProviderHandler extends ExceptionCatcherEventHandler<WaitForValidateRdsUpgradeOnCloudProviderRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(WaitForValidateRdsUpgradeOnProviderHandler.class);

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private ExternalDatabaseService externalDatabaseService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(WaitForValidateRdsUpgradeOnCloudProviderRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<WaitForValidateRdsUpgradeOnCloudProviderRequest> event) {
        LOGGER.error("Waiting for validating RDS upgrade on cloud provider side has failed", e);
        return new ValidateRdsUpgradeFailedEvent(resourceId, e, DetailedStackStatus.EXTERNAL_DATABASE_UPGRADE_VALIDATION_FAILED);
    }

    @Override
    public Selectable doAccept(HandlerEvent<WaitForValidateRdsUpgradeOnCloudProviderRequest> event) {
        WaitForValidateRdsUpgradeOnCloudProviderRequest request = event.getData();
        Long stackId = request.getResourceId();
        DatabaseConnectionProperties canaryProperties = request.getCanaryProperties();
        StackDto stack = stackDtoService.getById(stackId);
        try {
            LOGGER.info("Waiting for the RDS upgrade validation result");
            ClusterView cluster = stack.getCluster();

            DatabaseServerV4Response databaseServerV4Response = null;
            if (request.getFlowIdentifier() != null) {
                databaseServerV4Response = externalDatabaseService.waitForDatabaseFlowToBeFinished(
                        cluster, request.getFlowIdentifier());
                String canaryHost = Optional.ofNullable(databaseServerV4Response)
                        .map(DatabaseServerV4Response::getCanaryDatabasePropertiesV4Response)
                        .map(CanaryDatabasePropertiesV4Response::getHost)
                        .orElse(null);
                canaryProperties.setConnectionUrl(canaryHost);
            } else {
                LOGGER.info("No need to wait for RDS upgrade validation, as the flow identifier is null");
            }
            return createResponse(stackId, databaseServerV4Response, canaryProperties);
        } catch (UserBreakException e) {
            LOGGER.error("Database 'upgrade validation' polling exited before timeout. Cause: ", e);
            return upgradeValidationFailedEvent(stackId, e);
        } catch (PollerStoppedException e) {
            LOGGER.error("Database 'upgrade validation' poller stopped for stack: {}", stack.getName(), e);
            return upgradeValidationFailedEvent(stackId, e);
        } catch (PollerException e) {
            LOGGER.error("Database 'upgrade validation' polling failed for stack: {}", stack.getName(), e);
            return upgradeValidationFailedEvent(stackId, e);
        } catch (Exception e) {
            LOGGER.error("Database 'upgrade validation' on cloud provider side has failed for stack: {}", stack.getName(), e);
            return upgradeValidationFailedEvent(stackId, e);
        }
    }

    private StackEvent upgradeValidationFailedEvent(Long stackId, Exception e) {
        return new ValidateRdsUpgradeFailedEvent(stackId, e, DetailedStackStatus.EXTERNAL_DATABASE_UPGRADE_VALIDATION_FAILED);
    }

    private Selectable createResponse(Long stackId, DatabaseServerV4Response redbeamsResponse,
            DatabaseConnectionProperties canaryProperties) {
        String validationReason = Optional.ofNullable(redbeamsResponse)
                .map(DatabaseServerV4Response::getStatusReason)
                .orElse("");
        Selectable response;

        if (StringUtils.isNotBlank(validationReason)) {
            LOGGER.info("Validating RDS upgrade on cloud provider side has a warning: {}", validationReason);
            response = new WaitForValidateRdsUpgradeOnCloudProviderResult(stackId, validationReason, canaryProperties);

        } else {
            response = new WaitForValidateRdsUpgradeOnCloudProviderResult(stackId, null, canaryProperties);
        }
        return response;
    }
}