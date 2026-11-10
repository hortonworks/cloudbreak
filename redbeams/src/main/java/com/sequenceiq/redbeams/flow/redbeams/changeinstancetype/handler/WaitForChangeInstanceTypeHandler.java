package com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.handler;

import static com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.event.ChangeInstanceTypeHandlerSelectors.WAIT_FOR_CHANGE_INSTANCE_TYPE_HANDLER;
import static com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.event.ChangeInstanceTypeStateSelectors.CHANGE_INSTANCE_TYPE_WAIT_FINISHED_EVENT;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.ResourceConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.cloud.model.database.DatabaseServerInstanceTypeState;
import com.sequenceiq.cloudbreak.cloud.scheduler.SyncPollingScheduler;
import com.sequenceiq.cloudbreak.cloud.task.PollTask;
import com.sequenceiq.cloudbreak.cloud.task.PollTaskFactory;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.redbeams.configuration.ChangeInstanceTypeConfig;
import com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.event.ChangeInstanceTypeEvent;
import com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.event.ChangeInstanceTypeHandlerRequest;

/**
 * Polls the provider until the target instance type is applied and no modification is in progress. The timeout depends on the provider and
 * on the live HA state. A timed out modification is never cancelled or reverted, none of the providers offer a cancel API for it.
 */
@Component
public class WaitForChangeInstanceTypeHandler extends AbstractChangeInstanceTypeHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(WaitForChangeInstanceTypeHandler.class);

    @Inject
    private PollTaskFactory pollTaskFactory;

    @Inject
    private SyncPollingScheduler<DatabaseServerInstanceTypeState> syncPollingScheduler;

    @Inject
    private ChangeInstanceTypeConfig changeInstanceTypeConfig;

    @Override
    public String selector() {
        return WAIT_FOR_CHANGE_INSTANCE_TYPE_HANDLER.event();
    }

    @Override
    protected Selectable doAccept(ChangeInstanceTypeHandlerRequest request, ResourceConnector resourceConnector, AuthenticatedContext ac)
            throws Exception {
        DatabaseStack databaseStack = request.getDatabaseStack();
        String serverId = databaseStack.getDatabaseServer().getServerId();
        String targetInstanceType = request.getTargetInstanceType();
        Duration interval = changeInstanceTypeConfig.getPollingInterval();
        Duration timeout = changeInstanceTypeConfig.getTimeout(request.getCloudContext().getPlatform().value(),
                request.getLiveState().highAvailability());
        int maxAttempts = (int) Math.max(1, timeout.toMillis() / interval.toMillis());
        LOGGER.info("Waiting for instance type '{}' to be applied on database server '{}', polling every {} seconds for at most {} minutes",
                targetInstanceType, serverId, interval.toSeconds(), timeout.toMinutes());
        PollTask<DatabaseServerInstanceTypeState> task = pollTaskFactory.newPollChangeDatabaseServerInstanceTypeTask(ac, databaseStack, targetInstanceType);
        DatabaseServerInstanceTypeState liveState;
        try {
            liveState = syncPollingScheduler.schedule(task, (int) interval.toMillis(), maxAttempts, SyncPollingScheduler.FAILURE_TOLERANT_ATTEMPT);
        } catch (TimeoutException e) {
            throw new CloudbreakServiceException(String.format("Changing the instance type of database server '%s' to '%s' did not finish within %d minutes. "
                    + "The modification can not be cancelled on the cloud provider, manual investigation is needed.",
                    serverId, targetInstanceType, timeout.toMinutes()), e);
        }
        LOGGER.info("Instance type '{}' is applied on database server '{}', live state: {}", targetInstanceType, serverId, liveState);
        return new ChangeInstanceTypeEvent(CHANGE_INSTANCE_TYPE_WAIT_FINISHED_EVENT.event(), request.getResourceId(), targetInstanceType, liveState);
    }
}
