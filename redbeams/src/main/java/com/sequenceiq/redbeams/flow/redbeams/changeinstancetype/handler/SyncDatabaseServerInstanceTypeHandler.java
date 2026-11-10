package com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.handler;

import static com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.event.ChangeInstanceTypeHandlerSelectors.SYNC_DATABASE_SERVER_INSTANCE_TYPE_HANDLER;
import static com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.event.ChangeInstanceTypeStateSelectors.CHANGE_INSTANCE_TYPE_SYNC_FINISHED_EVENT;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.ResourceConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.database.DatabaseServerInstanceTypeState;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.event.ChangeInstanceTypeEvent;
import com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.event.ChangeInstanceTypeHandlerRequest;

@Component
public class SyncDatabaseServerInstanceTypeHandler extends AbstractChangeInstanceTypeHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(SyncDatabaseServerInstanceTypeHandler.class);

    @Override
    public String selector() {
        return SYNC_DATABASE_SERVER_INSTANCE_TYPE_HANDLER.event();
    }

    @Override
    protected Selectable doAccept(ChangeInstanceTypeHandlerRequest request, ResourceConnector resourceConnector, AuthenticatedContext ac)
            throws Exception {
        DatabaseServerInstanceTypeState liveState = resourceConnector.getDatabaseServerInstanceTypeState(ac, request.getDatabaseStack());
        LOGGER.info("Live instance type state of database server '{}' before changing it to '{}': {}",
                request.getDatabaseStack().getDatabaseServer().getServerId(), request.getTargetInstanceType(), liveState);
        return new ChangeInstanceTypeEvent(CHANGE_INSTANCE_TYPE_SYNC_FINISHED_EVENT.event(), request.getResourceId(), request.getTargetInstanceType(),
                liveState);
    }
}
