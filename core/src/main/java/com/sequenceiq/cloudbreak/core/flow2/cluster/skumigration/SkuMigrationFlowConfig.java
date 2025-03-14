package com.sequenceiq.cloudbreak.core.flow2.cluster.skumigration;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.skumigration.SkuMigrationFlowEvent.SKU_MIGRATION_ATTACH_PUBLIC_IPS_ADD_LB_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.skumigration.SkuMigrationFlowEvent.SKU_MIGRATION_CHECK_SKU_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.skumigration.SkuMigrationFlowEvent.SKU_MIGRATION_DETACH_PUBLIC_IPS_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.skumigration.SkuMigrationFlowEvent.SKU_MIGRATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.skumigration.SkuMigrationFlowEvent.SKU_MIGRATION_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.skumigration.SkuMigrationFlowEvent.SKU_MIGRATION_FAIL_HANDLED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.skumigration.SkuMigrationFlowEvent.SKU_MIGRATION_FINALIZED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.skumigration.SkuMigrationFlowEvent.SKU_MIGRATION_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.skumigration.SkuMigrationFlowEvent.SKU_MIGRATION_REMOVE_LOAD_BALANCER_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.skumigration.SkuMigrationFlowState.FINAL_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.skumigration.SkuMigrationFlowState.INIT_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.skumigration.SkuMigrationFlowState.SKU_MIGRATION_ATTACH_PUBLIC_IPS_ADD_LB_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.skumigration.SkuMigrationFlowState.SKU_MIGRATION_CHECK_SKU_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.skumigration.SkuMigrationFlowState.SKU_MIGRATION_DETACH_PUBLIC_IPS_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.skumigration.SkuMigrationFlowState.SKU_MIGRATION_FAILED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.skumigration.SkuMigrationFlowState.SKU_MIGRATION_FINISHED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.skumigration.SkuMigrationFlowState.SKU_MIGRATION_REMOVE_LOAD_BALANCER_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.skumigration.SkuMigrationFlowState.SKU_MIGRATION_UPDATE_DNS_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.flow2.StackStatusFinalizerAbstractFlowConfig;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration.Transition.Builder;

@Component
public class SkuMigrationFlowConfig
        extends StackStatusFinalizerAbstractFlowConfig<SkuMigrationFlowState, SkuMigrationFlowEvent> {

    public static final FlowEdgeConfig<SkuMigrationFlowState, SkuMigrationFlowEvent> EDGE_CONFIG =
            new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, SKU_MIGRATION_FAILED_STATE, SKU_MIGRATION_FAIL_HANDLED_EVENT);

    private static final List<Transition<SkuMigrationFlowState, SkuMigrationFlowEvent>> TRANSITIONS =
            new Builder<SkuMigrationFlowState, SkuMigrationFlowEvent>()
                    .defaultFailureEvent(SKU_MIGRATION_FAILED_EVENT)

                    .from(INIT_STATE)
                    .to(SKU_MIGRATION_CHECK_SKU_STATE)
                    .event(SKU_MIGRATION_EVENT)
                    .defaultFailureEvent()

                    .from(SKU_MIGRATION_CHECK_SKU_STATE)
                    .to(SKU_MIGRATION_DETACH_PUBLIC_IPS_STATE)
                    .event(SKU_MIGRATION_CHECK_SKU_FINISHED_EVENT)
                    .defaultFailureEvent()

                    .from(SKU_MIGRATION_CHECK_SKU_STATE)
                    .to(SKU_MIGRATION_FINISHED_STATE)
                    .event(SKU_MIGRATION_FINISHED_EVENT)
                    .defaultFailureEvent()

                    .from(SKU_MIGRATION_DETACH_PUBLIC_IPS_STATE)
                    .to(SKU_MIGRATION_REMOVE_LOAD_BALANCER_STATE)
                    .event(SKU_MIGRATION_DETACH_PUBLIC_IPS_FINISHED_EVENT)
                    .defaultFailureEvent()

                    .from(SKU_MIGRATION_REMOVE_LOAD_BALANCER_STATE)
                    .to(SKU_MIGRATION_ATTACH_PUBLIC_IPS_ADD_LB_STATE)
                    .event(SKU_MIGRATION_REMOVE_LOAD_BALANCER_FINISHED_EVENT)
                    .defaultFailureEvent()

                    .from(SKU_MIGRATION_ATTACH_PUBLIC_IPS_ADD_LB_STATE)
                    .to(SKU_MIGRATION_UPDATE_DNS_STATE)
                    .event(SKU_MIGRATION_ATTACH_PUBLIC_IPS_ADD_LB_FINISHED_EVENT)
                    .defaultFailureEvent()

                    .from(SKU_MIGRATION_UPDATE_DNS_STATE)
                    .to(SKU_MIGRATION_FINISHED_STATE)
                    .event(SKU_MIGRATION_FINISHED_EVENT)
                    .defaultFailureEvent()

                    .from(SKU_MIGRATION_FINISHED_STATE)
                    .to(FINAL_STATE)
                    .event(SKU_MIGRATION_FINALIZED_EVENT)
                    .defaultFailureEvent()
                    .build();

    protected SkuMigrationFlowConfig() {
        super(SkuMigrationFlowState.class, SkuMigrationFlowEvent.class);
    }

    @Override
    protected List<Transition<SkuMigrationFlowState, SkuMigrationFlowEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    public FlowEdgeConfig<SkuMigrationFlowState, SkuMigrationFlowEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public SkuMigrationFlowEvent[] getEvents() {
        return SkuMigrationFlowEvent.values();
    }

    @Override
    public SkuMigrationFlowEvent[] getInitEvents() {
        return new SkuMigrationFlowEvent[] {
                SKU_MIGRATION_EVENT
        };
    }

    @Override
    public String getDisplayName() {
        return "Load balancer migration";
    }
}
