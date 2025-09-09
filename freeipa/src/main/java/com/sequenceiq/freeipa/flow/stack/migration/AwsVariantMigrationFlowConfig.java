package com.sequenceiq.freeipa.flow.stack.migration;

import static com.sequenceiq.freeipa.flow.stack.migration.AwsVariantMigrationEvent.AWS_VARIANT_MIGRATION_FAILED_EVENT;
import static com.sequenceiq.freeipa.flow.stack.migration.AwsVariantMigrationEvent.AWS_VARIANT_MIGRATION_FAIL_HANDLED_EVENT;
import static com.sequenceiq.freeipa.flow.stack.migration.AwsVariantMigrationEvent.AWS_VARIANT_MIGRATION_FINALIZED_EVENT;
import static com.sequenceiq.freeipa.flow.stack.migration.AwsVariantMigrationEvent.CHANGE_VARIANT_EVENT;
import static com.sequenceiq.freeipa.flow.stack.migration.AwsVariantMigrationEvent.CREATE_RESOURCES_EVENT;
import static com.sequenceiq.freeipa.flow.stack.migration.AwsVariantMigrationEvent.DELETE_CLOUD_FORMATION_EVENT;
import static com.sequenceiq.freeipa.flow.stack.migration.AwsVariantMigrationFlowState.AWS_VARIANT_MIGRATION_FAILED_STATE;
import static com.sequenceiq.freeipa.flow.stack.migration.AwsVariantMigrationFlowState.CHANGE_VARIANT_STATE;
import static com.sequenceiq.freeipa.flow.stack.migration.AwsVariantMigrationFlowState.CREATE_RESOURCES_STATE;
import static com.sequenceiq.freeipa.flow.stack.migration.AwsVariantMigrationFlowState.DELETE_CLOUD_FORMATION_STATE;
import static com.sequenceiq.freeipa.flow.stack.migration.AwsVariantMigrationFlowState.FINAL_STATE;
import static com.sequenceiq.freeipa.flow.stack.migration.AwsVariantMigrationFlowState.INIT_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration.Transition.Builder;
import com.sequenceiq.flow.core.config.RetryableFlowConfiguration;
import com.sequenceiq.freeipa.flow.StackStatusFinalizerAbstractFlowConfig;

@Component
public class AwsVariantMigrationFlowConfig extends StackStatusFinalizerAbstractFlowConfig<AwsVariantMigrationFlowState, AwsVariantMigrationEvent>
        implements RetryableFlowConfiguration<AwsVariantMigrationEvent> {

    private static final List<Transition<AwsVariantMigrationFlowState, AwsVariantMigrationEvent>> TRANSITIONS =
            new Builder<AwsVariantMigrationFlowState, AwsVariantMigrationEvent>()
                    .defaultFailureEvent(AWS_VARIANT_MIGRATION_FAILED_EVENT)
                    .from(INIT_STATE).to(CREATE_RESOURCES_STATE).event(CREATE_RESOURCES_EVENT).defaultFailureEvent()
                    .from(CREATE_RESOURCES_STATE).to(DELETE_CLOUD_FORMATION_STATE).event(DELETE_CLOUD_FORMATION_EVENT).defaultFailureEvent()
                    .from(DELETE_CLOUD_FORMATION_STATE).to(CHANGE_VARIANT_STATE).event(CHANGE_VARIANT_EVENT).defaultFailureEvent()
                    .from(CHANGE_VARIANT_STATE).to(FINAL_STATE).event(AWS_VARIANT_MIGRATION_FINALIZED_EVENT).defaultFailureEvent()
                    .build();

    private static final AbstractFlowConfiguration.FlowEdgeConfig<AwsVariantMigrationFlowState, AwsVariantMigrationEvent> EDGE_CONFIG =
            new AbstractFlowConfiguration.FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, AWS_VARIANT_MIGRATION_FAILED_STATE,
                    AWS_VARIANT_MIGRATION_FAIL_HANDLED_EVENT);

    public AwsVariantMigrationFlowConfig() {
        super(AwsVariantMigrationFlowState.class, AwsVariantMigrationEvent.class);
    }

    @Override
    protected List<Transition<AwsVariantMigrationFlowState, AwsVariantMigrationEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    public AbstractFlowConfiguration.FlowEdgeConfig<AwsVariantMigrationFlowState, AwsVariantMigrationEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public AwsVariantMigrationEvent[] getEvents() {
        return AwsVariantMigrationEvent.values();
    }

    @Override
    public AwsVariantMigrationEvent[] getInitEvents() {
        return new AwsVariantMigrationEvent[]{
                CREATE_RESOURCES_EVENT
        };
    }

    @Override
    public String getDisplayName() {
        return "Migrate AWS variant to AWS_NATIVE variant";
    }

    @Override
    public AwsVariantMigrationEvent getRetryableEvent() {
        return AWS_VARIANT_MIGRATION_FAIL_HANDLED_EVENT;
    }
}
