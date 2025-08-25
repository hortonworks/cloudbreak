package com.sequenceiq.redbeams.flow.redbeams.sslmigration.actions;

import static com.sequenceiq.redbeams.flow.redbeams.sslmigration.RedbeamsSslMigrationEventSelectors.REDBEAMS_SSL_MIGRATION_FINALIZED_EVENT;
import static com.sequenceiq.redbeams.metrics.MetricType.DB_SSL_MIGRATION_FINISHED;

import java.util.Map;
import java.util.Optional;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.redbeams.api.model.common.DetailedDBStackStatus;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.flow.redbeams.sslmigration.RedbeamsSslMigrationContext;
import com.sequenceiq.redbeams.flow.redbeams.sslmigration.event.RedbeamsSslMigrationEvent;
import com.sequenceiq.redbeams.flow.redbeams.sslmigration.event.RedbeamsSslMigrationFailed;
import com.sequenceiq.redbeams.flow.redbeams.sslmigration.event.RedbeamsSslMigrationHandlerSuccessResult;
import com.sequenceiq.redbeams.metrics.RedbeamsMetricService;
import com.sequenceiq.redbeams.service.stack.DBStackStatusUpdater;

@Component("REDBEAMS_SSL_MIGRATION_FINISHED_STATE")
public class RedbeamsSslMigrationFinishedAction extends AbstractRedbeamsSslMigrationAction<RedbeamsSslMigrationHandlerSuccessResult> {

    @Inject
    private DBStackStatusUpdater dbStackStatusUpdater;

    @Inject
    private RedbeamsMetricService metricService;

    public RedbeamsSslMigrationFinishedAction() {
        super(RedbeamsSslMigrationHandlerSuccessResult.class);
    }

    @Override
    protected void doExecute(RedbeamsSslMigrationContext context, RedbeamsSslMigrationHandlerSuccessResult payload,
        Map<Object, Object> variables) throws Exception {
        Optional<DBStack> dbStack = dbStackStatusUpdater.updateStatus(payload.getResourceId(), DetailedDBStackStatus.DB_SSL_MIGRATION_COMPLETED);
        metricService.incrementMetricCounter(DB_SSL_MIGRATION_FINISHED, dbStack);
        sendEvent(context);
    }

    @Override
    protected Selectable createRequest(RedbeamsSslMigrationContext context) {
        return new RedbeamsSslMigrationEvent(
                REDBEAMS_SSL_MIGRATION_FINALIZED_EVENT.event(),
                context.getDBStack().getId());
    }

    @Override
    protected Object getFailurePayload(RedbeamsSslMigrationHandlerSuccessResult payload,
        Optional<RedbeamsSslMigrationContext> flowContext, Exception ex) {
        return new RedbeamsSslMigrationFailed(payload.getResourceId(), ex);
    }
}
