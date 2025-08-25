package com.sequenceiq.redbeams.flow.redbeams.sslmigration.actions;

import static com.sequenceiq.redbeams.api.model.common.DetailedDBStackStatus.DB_SSL_MIGRATION_FAILED;
import static com.sequenceiq.redbeams.flow.redbeams.sslmigration.RedbeamsSslMigrationEventSelectors.REDBEAMS_SSL_MIGRATION_FAILURE_HANDLED_EVENT;

import java.util.Map;
import java.util.Optional;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.statemachine.StateContext;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.flow.core.Flow;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.flow.redbeams.sslmigration.RedbeamsSslMigrationContext;
import com.sequenceiq.redbeams.flow.redbeams.sslmigration.RedbeamsSslMigrationEventSelectors;
import com.sequenceiq.redbeams.flow.redbeams.sslmigration.RedbeamsSslMigrationState;
import com.sequenceiq.redbeams.flow.redbeams.sslmigration.event.RedbeamsSslMigrationEvent;
import com.sequenceiq.redbeams.flow.redbeams.sslmigration.event.RedbeamsSslMigrationFailed;
import com.sequenceiq.redbeams.metrics.MetricType;
import com.sequenceiq.redbeams.metrics.RedbeamsMetricService;
import com.sequenceiq.redbeams.service.stack.DBStackStatusUpdater;

@Component("REDBEAMS_SSL_MIGRATION_FAILED_STATE")
public class RedbeamsSslMigrationFailedAction extends AbstractRedbeamsSslMigrationAction<RedbeamsSslMigrationFailed> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedbeamsSslMigrationFailedAction.class);

    @Inject
    private DBStackStatusUpdater dbStackStatusUpdater;

    @Inject
    private RedbeamsMetricService metricService;

    public RedbeamsSslMigrationFailedAction() {
        super(RedbeamsSslMigrationFailed.class);
    }

    @Override
    protected void doExecute(RedbeamsSslMigrationContext context, RedbeamsSslMigrationFailed payload, Map<Object, Object> variables) throws Exception {
        Exception failureException = payload.getException();
        LOGGER.warn("Error during database server rotate cert flow:", failureException);

        String errorReason = failureException == null ? "Unknown error" : failureException.getMessage();
        Optional<DBStack> dbStack = dbStackStatusUpdater.updateStatus(payload.getResourceId(), DB_SSL_MIGRATION_FAILED, errorReason);
        metricService.incrementMetricCounter(MetricType.DB_SSL_MIGRATION_FAILED, dbStack);
        sendEvent(context);
    }

    @Override
    protected RedbeamsSslMigrationContext createFlowContext(
            FlowParameters flowParameters,
            StateContext<RedbeamsSslMigrationState, RedbeamsSslMigrationEventSelectors> stateContext,
            RedbeamsSslMigrationFailed payload) {
        Flow flow = getFlow(flowParameters.getFlowId());
        flow.setFlowFailed(payload.getException());
        return super.createFlowContext(flowParameters, stateContext, payload);
    }

    @Override
    protected Object getFailurePayload(RedbeamsSslMigrationFailed payload, Optional<RedbeamsSslMigrationContext> flowContext,
        Exception ex) {
        return new RedbeamsSslMigrationFailed(payload.getResourceId(), ex);
    }

    @Override
    protected Selectable createRequest(RedbeamsSslMigrationContext context) {
        return new RedbeamsSslMigrationEvent(REDBEAMS_SSL_MIGRATION_FAILURE_HANDLED_EVENT.event(), context.getDBStack().getId());
    }
}
