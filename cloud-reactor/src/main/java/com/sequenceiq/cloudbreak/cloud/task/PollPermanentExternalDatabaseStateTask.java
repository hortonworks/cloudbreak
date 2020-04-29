package com.sequenceiq.cloudbreak.cloud.task;

import com.sequenceiq.cloudbreak.cloud.ResourceConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.ExternalDatabaseStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component(PollPermanentExternalDatabaseStateTask.NAME)
@Scope("prototype")
public class PollPermanentExternalDatabaseStateTask extends AbstractPollTask<ExternalDatabaseStatus> {

    public static final String NAME = "pollPermanentExternalDatabaseStateTask";

    private static final Logger LOGGER = LoggerFactory.getLogger(PollPermanentExternalDatabaseStateTask.class);

    private String dbInstanceIdentifier;

    private ResourceConnector resourceConnector;

    public PollPermanentExternalDatabaseStateTask(AuthenticatedContext authenticatedContext,
            String dbInstanceIdentifier, ResourceConnector resourceConnector) {
        super(authenticatedContext);
        this.dbInstanceIdentifier = dbInstanceIdentifier;
        this.resourceConnector = resourceConnector;
    }

    @Override
    protected ExternalDatabaseStatus doCall() {
        LOGGER.debug("Checking '{}' RDB instance status is in permanent status group.", dbInstanceIdentifier);
        try {
            return resourceConnector.getDatabaseServerStatus(getAuthenticatedContext(), dbInstanceIdentifier);
        } catch (Exception ex) {
            throw new CloudConnectorException(ex.getMessage(), ex);
        }

    }

    @Override
    public boolean completed(ExternalDatabaseStatus externalDatabaseStatus) {
        return externalDatabaseStatus.isPermanent();
    }
}
