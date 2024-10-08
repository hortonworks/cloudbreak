package com.sequenceiq.cloudbreak.reactor.handler.cluster.upgrade.rds;

import jakarta.inject.Inject;

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
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.UpgradeRdsFailedEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.WaitForDatabaseServerUpgradeRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.WaitForDatabaseServerUpgradeResult;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class WaitForRdsUpgradeHandler extends ExceptionCatcherEventHandler<WaitForDatabaseServerUpgradeRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(WaitForRdsUpgradeHandler.class);

    @Inject
    private ExternalDatabaseService databaseService;

    @Inject
    private StackDtoService stackDtoService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(WaitForDatabaseServerUpgradeRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<WaitForDatabaseServerUpgradeRequest> event) {
        LOGGER.error("RDS database server upgrade has failed", e);
        return new UpgradeRdsFailedEvent(resourceId, e, DetailedStackStatus.DATABASE_UPGRADE_FAILED);
    }

    @Override
    public Selectable doAccept(HandlerEvent<WaitForDatabaseServerUpgradeRequest> event) {
        WaitForDatabaseServerUpgradeRequest request = event.getData();
        Long stackId = request.getResourceId();
        LOGGER.info("Waiting for RDS database upgrade to be finished...");
        StackDto stack = stackDtoService.getById(stackId);
        ClusterView cluster = stack.getCluster();
        try {
            if (request.getFlowIdentifier() != null) {
                databaseService.waitForDatabaseToBeUpgraded(cluster, request.getFlowIdentifier());
            } else {
                LOGGER.info("No need to wait for RDS upgrade, as the flow identifier is null");
            }
            return new WaitForDatabaseServerUpgradeResult(stackId, request.getVersion(), request.getFlowIdentifier());
        } catch (UserBreakException e) {
            LOGGER.error("Database 'upgrade' polling exited before timeout. Cause: ", e);
            return upgradeFailedEvent(stackId, e);
        } catch (PollerStoppedException e) {
            LOGGER.error(String.format("Database 'upgrade' poller stopped for stack: %s", stack.getName()), e);
            return upgradeFailedEvent(stackId, e);
        } catch (PollerException e) {
            LOGGER.error(String.format("Database 'upgrade' polling failed for stack: %s", stack.getName()), e);
            return upgradeFailedEvent(stackId, e);
        } catch (Exception e) {
            LOGGER.error(String.format("Exception during DB upgrade for stack: %s", stack.getName()), e);
            return upgradeFailedEvent(stackId, e);
        }
    }

    private StackEvent upgradeFailedEvent(Long stackId, Exception e) {
        return new UpgradeRdsFailedEvent(stackId, e, DetailedStackStatus.DATABASE_UPGRADE_FAILED);
    }
}
