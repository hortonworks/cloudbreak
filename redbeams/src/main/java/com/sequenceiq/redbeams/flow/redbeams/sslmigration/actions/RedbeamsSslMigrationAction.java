package com.sequenceiq.redbeams.flow.redbeams.sslmigration.actions;

import static com.sequenceiq.redbeams.api.model.common.DetailedDBStackStatus.DB_SSL_MIGRATION_IN_PROGRESS;

import java.util.Map;
import java.util.Optional;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.redbeams.flow.redbeams.sslmigration.RedbeamsSslMigrationContext;
import com.sequenceiq.redbeams.flow.redbeams.sslmigration.event.RedbeamsSslMigrationEvent;
import com.sequenceiq.redbeams.flow.redbeams.sslmigration.event.RedbeamsSslMigrationFailed;
import com.sequenceiq.redbeams.flow.redbeams.sslmigration.event.RedbeamsSslMigrationHandlerRequest;
import com.sequenceiq.redbeams.service.stack.DBStackStatusUpdater;

@Component("REDBEAMS_SSL_MIGRATION_STATE")
public class RedbeamsSslMigrationAction extends AbstractRedbeamsSslMigrationAction<RedbeamsSslMigrationEvent> {

    @Inject
    private DBStackStatusUpdater dbStackStatusUpdater;

    public RedbeamsSslMigrationAction() {
        super(RedbeamsSslMigrationEvent.class);
    }

    @Override
    protected void doExecute(RedbeamsSslMigrationContext context, RedbeamsSslMigrationEvent payload, Map<Object, Object> variables) throws Exception {
        dbStackStatusUpdater.updateStatus(payload.getResourceId(), DB_SSL_MIGRATION_IN_PROGRESS);
        sendEvent(context);
    }

    @Override
    protected Selectable createRequest(RedbeamsSslMigrationContext context) {
        return new RedbeamsSslMigrationHandlerRequest(context.getCloudContext().getId(),
                context.getCloudContext(),
                context.getCloudCredential(),
                context.getDatabaseStack());
    }

    @Override
    protected Object getFailurePayload(RedbeamsSslMigrationEvent payload, Optional<RedbeamsSslMigrationContext> flowContext, Exception ex) {
        return new RedbeamsSslMigrationFailed(payload.getResourceId(), ex);
    }
}
