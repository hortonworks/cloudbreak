package com.sequenceiq.redbeams.flow.redbeams.sslmigration;

import static com.sequenceiq.redbeams.flow.redbeams.sslmigration.RedbeamsSslMigrationEventSelectors.REDBEAMS_SSL_MIGRATION_EVENT;
import static com.sequenceiq.redbeams.flow.redbeams.sslmigration.RedbeamsSslMigrationEventSelectors.REDBEAMS_SSL_MIGRATION_FAILED_EVENT;
import static com.sequenceiq.redbeams.flow.redbeams.sslmigration.RedbeamsSslMigrationEventSelectors.REDBEAMS_SSL_MIGRATION_FAILURE_HANDLED_EVENT;
import static com.sequenceiq.redbeams.flow.redbeams.sslmigration.RedbeamsSslMigrationEventSelectors.REDBEAMS_SSL_MIGRATION_FINALIZED_EVENT;
import static com.sequenceiq.redbeams.flow.redbeams.sslmigration.RedbeamsSslMigrationEventSelectors.REDBEAMS_SSL_MIGRATION_FINISHED_EVENT;
import static com.sequenceiq.redbeams.flow.redbeams.sslmigration.RedbeamsSslMigrationEventSelectors.values;
import static com.sequenceiq.redbeams.flow.redbeams.sslmigration.RedbeamsSslMigrationState.FINAL_STATE;
import static com.sequenceiq.redbeams.flow.redbeams.sslmigration.RedbeamsSslMigrationState.INIT_STATE;
import static com.sequenceiq.redbeams.flow.redbeams.sslmigration.RedbeamsSslMigrationState.REDBEAMS_SSL_MIGRATION_FAILED_STATE;
import static com.sequenceiq.redbeams.flow.redbeams.sslmigration.RedbeamsSslMigrationState.REDBEAMS_SSL_MIGRATION_FINISHED_STATE;
import static com.sequenceiq.redbeams.flow.redbeams.sslmigration.RedbeamsSslMigrationState.REDBEAMS_SSL_MIGRATION_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;
import com.sequenceiq.flow.core.config.RetryableFlowConfiguration;

@Component
public class RedbeamsSslMigrationFlowConfig extends AbstractFlowConfiguration<RedbeamsSslMigrationState, RedbeamsSslMigrationEventSelectors>
        implements RetryableFlowConfiguration<RedbeamsSslMigrationEventSelectors> {

    private static final RedbeamsSslMigrationEventSelectors[] REDBEAMS_INIT_EVENTS = {REDBEAMS_SSL_MIGRATION_EVENT};

    private static final List<Transition<RedbeamsSslMigrationState, RedbeamsSslMigrationEventSelectors>> TRANSITIONS =
            new Transition.Builder<RedbeamsSslMigrationState, RedbeamsSslMigrationEventSelectors>()
                    .defaultFailureEvent(REDBEAMS_SSL_MIGRATION_FAILED_EVENT)

                    .from(INIT_STATE)
                    .to(REDBEAMS_SSL_MIGRATION_STATE)
                    .event(REDBEAMS_SSL_MIGRATION_EVENT)
                    .defaultFailureEvent()

                    .from(REDBEAMS_SSL_MIGRATION_STATE)
                    .to(REDBEAMS_SSL_MIGRATION_FINISHED_STATE)
                    .event(REDBEAMS_SSL_MIGRATION_FINISHED_EVENT)
                    .defaultFailureEvent()

                    .from(REDBEAMS_SSL_MIGRATION_FINISHED_STATE)
                    .to(FINAL_STATE)
                    .event(REDBEAMS_SSL_MIGRATION_FINALIZED_EVENT)
                    .defaultFailureEvent()

                    .build();

    private static final FlowEdgeConfig<RedbeamsSslMigrationState, RedbeamsSslMigrationEventSelectors> EDGE_CONFIG =
            new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, REDBEAMS_SSL_MIGRATION_FAILED_STATE, REDBEAMS_SSL_MIGRATION_FAILURE_HANDLED_EVENT);

    public RedbeamsSslMigrationFlowConfig() {
        super(RedbeamsSslMigrationState.class, RedbeamsSslMigrationEventSelectors.class);
    }

    @Override
    protected List<Transition<RedbeamsSslMigrationState, RedbeamsSslMigrationEventSelectors>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    public FlowEdgeConfig<RedbeamsSslMigrationState, RedbeamsSslMigrationEventSelectors> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public RedbeamsSslMigrationEventSelectors[] getEvents() {
        return values();
    }

    @Override
    public RedbeamsSslMigrationEventSelectors[] getInitEvents() {
        return REDBEAMS_INIT_EVENTS;
    }

    @Override
    public String getDisplayName() {
        return "RDS SSL Migration";
    }

    @Override
    public RedbeamsSslMigrationEventSelectors getRetryableEvent() {
        return REDBEAMS_SSL_MIGRATION_FAILURE_HANDLED_EVENT;
    }
}
