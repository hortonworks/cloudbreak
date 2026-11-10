package com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.handler;

import static com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.event.ChangeInstanceTypeHandlerSelectors.CHANGE_DATABASE_SERVER_INSTANCE_TYPE_HANDLER;
import static com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.event.ChangeInstanceTypeStateSelectors.CHANGE_INSTANCE_TYPE_MODIFY_FINISHED_EVENT;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.ResourceConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.cloud.model.database.DatabaseServerInstanceTypeState;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.event.ChangeInstanceTypeEvent;
import com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.event.ChangeInstanceTypeHandlerRequest;

/**
 * Issues the instance type modification. The live state is read again right before, so a rerun after a restart or a retry does not issue
 * a second modification that the provider would reject as concurrent: if the target is already applied or a modification is in progress,
 * the call is skipped and the flow goes on to wait for it. The in-progress skip here is only rerun safety: the validation that ran right
 * before this handler already fails the request fast when a modification to a different instance type is in progress.
 */
@Component
public class ChangeDatabaseServerInstanceTypeHandler extends AbstractChangeInstanceTypeHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChangeDatabaseServerInstanceTypeHandler.class);

    @Override
    public String selector() {
        return CHANGE_DATABASE_SERVER_INSTANCE_TYPE_HANDLER.event();
    }

    @Override
    protected Selectable doAccept(ChangeInstanceTypeHandlerRequest request, ResourceConnector resourceConnector, AuthenticatedContext ac)
            throws Exception {
        DatabaseStack databaseStack = request.getDatabaseStack();
        String serverId = databaseStack.getDatabaseServer().getServerId();
        String targetInstanceType = request.getTargetInstanceType();
        DatabaseServerInstanceTypeState liveState = resourceConnector.getDatabaseServerInstanceTypeState(ac, databaseStack);
        if (StringUtils.equalsIgnoreCase(targetInstanceType, liveState.instanceType())) {
            LOGGER.info("Database server '{}' already runs on instance type '{}', skipping the modification", serverId, targetInstanceType);
        } else if (liveState.modificationInProgress()) {
            LOGGER.info("A modification is already in progress on database server '{}', skipping the modification to '{}' and waiting for it as rerun "
                    + "safety (validation passed just before). Live state: {}", serverId, targetInstanceType, liveState);
        } else {
            LOGGER.info("Changing the instance type of database server '{}' from '{}' to '{}'", serverId, liveState.instanceType(), targetInstanceType);
            resourceConnector.changeDatabaseServerInstanceType(ac, databaseStack, targetInstanceType);
        }
        return new ChangeInstanceTypeEvent(CHANGE_INSTANCE_TYPE_MODIFY_FINISHED_EVENT.event(), request.getResourceId(), targetInstanceType, liveState);
    }
}
