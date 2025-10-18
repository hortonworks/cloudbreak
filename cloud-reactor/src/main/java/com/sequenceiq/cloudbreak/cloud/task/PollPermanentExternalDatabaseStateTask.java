package com.sequenceiq.cloudbreak.cloud.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.ResourceConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.cloud.model.ExternalDatabaseStatus;

@Component(PollPermanentExternalDatabaseStateTask.NAME)
@Scope("prototype")
public class PollPermanentExternalDatabaseStateTask extends AbstractPollTask<ExternalDatabaseStatus> {

    public static final String NAME = "pollPermanentExternalDatabaseStateTask";

    private static final Logger LOGGER = LoggerFactory.getLogger(PollPermanentExternalDatabaseStateTask.class);

    private DatabaseStack dbStack;

    private ResourceConnector resourceConnector;

    public PollPermanentExternalDatabaseStateTask(AuthenticatedContext authenticatedContext,
            DatabaseStack dbStack, ResourceConnector resourceConnector) {
        super(authenticatedContext);
        this.dbStack = dbStack;
        this.resourceConnector = resourceConnector;
    }

    @Override
    protected ExternalDatabaseStatus doCall() {
        LOGGER.debug("Checking '{}' RDB instance status is in permanent status group.", dbStack.getDatabaseServer().getServerId());
        try {
            return resourceConnector.getDatabaseServerStatus(getAuthenticatedContext(), dbStack);
        } catch (Exception ex) {
            throw new CloudConnectorException(ex.getMessage(), ex);
        }

    }

    @Override
    public boolean completed(ExternalDatabaseStatus externalDatabaseStatus) {
        return externalDatabaseStatus.isPermanent();
    }
}
