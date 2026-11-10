package com.sequenceiq.cloudbreak.cloud.task;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.ResourceConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.cloud.model.database.DatabaseServerInstanceTypeState;

@Component(PollChangeDatabaseServerInstanceTypeTask.NAME)
@Scope("prototype")
public class PollChangeDatabaseServerInstanceTypeTask extends AbstractPollTask<DatabaseServerInstanceTypeState> {

    public static final String NAME = "pollChangeDatabaseServerInstanceTypeTask";

    private static final Logger LOGGER = LoggerFactory.getLogger(PollChangeDatabaseServerInstanceTypeTask.class);

    private final DatabaseStack dbStack;

    private final String targetInstanceType;

    private final ResourceConnector resourceConnector;

    public PollChangeDatabaseServerInstanceTypeTask(AuthenticatedContext authenticatedContext, DatabaseStack dbStack, String targetInstanceType,
            ResourceConnector resourceConnector) {
        super(authenticatedContext);
        this.dbStack = dbStack;
        this.targetInstanceType = targetInstanceType;
        this.resourceConnector = resourceConnector;
    }

    @Override
    protected DatabaseServerInstanceTypeState doCall() {
        LOGGER.debug("Checking whether instance type '{}' is applied on database server '{}'.", targetInstanceType,
                dbStack.getDatabaseServer().getServerId());
        try {
            return resourceConnector.getDatabaseServerInstanceTypeState(getAuthenticatedContext(), dbStack);
        } catch (Exception ex) {
            throw new CloudConnectorException(ex.getMessage(), ex);
        }
    }

    @Override
    public boolean completed(DatabaseServerInstanceTypeState state) {
        boolean completed = !state.modificationInProgress() && StringUtils.equalsIgnoreCase(targetInstanceType, state.instanceType());
        LOGGER.debug("Instance type state of database server '{}': {}, target instance type '{}' applied: {}",
                dbStack.getDatabaseServer().getServerId(), state, targetInstanceType, completed);
        return completed;
    }
}
