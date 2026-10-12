package com.sequenceiq.cloudbreak.reactor.handler.cluster;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.dyngr.exception.PollerException;
import com.dyngr.exception.PollerStoppedException;
import com.dyngr.exception.UserBreakException;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.externaldatabase.ExternalDatabaseService;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.install.WaitForExternalDatabaseFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.install.WaitForExternalDatabaseRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.install.WaitForExternalDatabaseSuccess;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class WaitForExternalDatabaseHandler extends ExceptionCatcherEventHandler<WaitForExternalDatabaseRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(WaitForExternalDatabaseHandler.class);

    @Inject
    private ExternalDatabaseService externalDatabaseService;

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private CloudbreakEventService cloudbreakEventService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(WaitForExternalDatabaseRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<WaitForExternalDatabaseRequest> event) {
        return new WaitForExternalDatabaseFailed(resourceId, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<WaitForExternalDatabaseRequest> event) {
        Long stackId = event.getData().getResourceId();
        ClusterView cluster = stackDtoService.getClusterViewByStackId(stackId);
        String crn = cluster == null ? null : cluster.getDatabaseServerCrn();
        if (StringUtils.isBlank(crn)) {
            LOGGER.info("No external DB configured for stack {}, skipping external database availability barrier", stackId);
            return new WaitForExternalDatabaseSuccess(stackId);
        }
        LOGGER.info("Waiting for external database {} to reach AVAILABLE for stack {}", crn, stackId);
        try {
            externalDatabaseService.waitForDatabaseAvailable(cluster, crn, true);
            LOGGER.info("External database {} is AVAILABLE for stack {}", crn, stackId);
            fireExternalDatabaseCreationFinishedEventForDatalake(stackId);
            return new WaitForExternalDatabaseSuccess(stackId);
        } catch (UserBreakException e) {
            LOGGER.error("External database polling exited before timeout for stack {}, crn {}", stackId, crn, e);
            return new WaitForExternalDatabaseFailed(stackId, e);
        } catch (PollerStoppedException e) {
            LOGGER.error("External database did not reach AVAILABLE in time for stack {}, crn {}", stackId, crn, e);
            return new WaitForExternalDatabaseFailed(stackId, e);
        } catch (PollerException e) {
            LOGGER.error("External database polling failed for stack {}, crn {}", stackId, crn, e);
            return new WaitForExternalDatabaseFailed(stackId, e);
        } catch (Exception e) {
            LOGGER.error("Unexpected error while waiting for external database for stack {}, crn {}", stackId, crn, e);
            return new WaitForExternalDatabaseFailed(stackId, e);
        }
    }

    private void fireExternalDatabaseCreationFinishedEventForDatalake(Long stackId) {
        try {
            StackView stack = stackDtoService.getStackViewById(stackId);
            if (stack.isDatalake()) {
                cloudbreakEventService.fireCloudbreakEvent(stackId, stack.getStatus().name(), ResourceEvent.CLUSTER_EXTERNAL_DATABASE_CREATION_FINISHED);
            }
        } catch (Exception e) {
            LOGGER.warn("Could not send the external database creation finished event for stack {}", stackId, e);
        }
    }
}
