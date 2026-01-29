package com.sequenceiq.datalake.flow.upgrade.database;


import static com.sequenceiq.datalake.flow.upgrade.database.SdxUpgradeDatabaseServerState.FINAL_STATE;
import static com.sequenceiq.datalake.flow.upgrade.database.SdxUpgradeDatabaseServerState.INIT_STATE;
import static com.sequenceiq.datalake.flow.upgrade.database.SdxUpgradeDatabaseServerState.SDX_UPGRADE_DATABASE_SERVER_FAILED_STATE;
import static com.sequenceiq.datalake.flow.upgrade.database.SdxUpgradeDatabaseServerState.SDX_UPGRADE_DATABASE_SERVER_FINISHED_STATE;
import static com.sequenceiq.datalake.flow.upgrade.database.SdxUpgradeDatabaseServerState.SDX_UPGRADE_DATABASE_SERVER_UPGRADE_STATE;
import static com.sequenceiq.datalake.flow.upgrade.database.SdxUpgradeDatabaseServerState.SDX_UPGRADE_DATABASE_SERVER_WAIT_UPGRADE_STATE;
import static com.sequenceiq.datalake.flow.upgrade.database.SdxUpgradeDatabaseServerStateSelectors.SDX_UPGRADE_DATABASE_SERVER_FAILED_EVENT;
import static com.sequenceiq.datalake.flow.upgrade.database.SdxUpgradeDatabaseServerStateSelectors.SDX_UPGRADE_DATABASE_SERVER_FAILED_HANDLED_EVENT;
import static com.sequenceiq.datalake.flow.upgrade.database.SdxUpgradeDatabaseServerStateSelectors.SDX_UPGRADE_DATABASE_SERVER_FINALIZED_EVENT;
import static com.sequenceiq.datalake.flow.upgrade.database.SdxUpgradeDatabaseServerStateSelectors.SDX_UPGRADE_DATABASE_SERVER_SUCCESS_EVENT;
import static com.sequenceiq.datalake.flow.upgrade.database.SdxUpgradeDatabaseServerStateSelectors.SDX_UPGRADE_DATABASE_SERVER_UPGRADE_EVENT;
import static com.sequenceiq.datalake.flow.upgrade.database.SdxUpgradeDatabaseServerStateSelectors.SDX_UPGRADE_DATABASE_SERVER_WAIT_SUCCESS_EVENT;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.datalake.flow.RetryableDatalakeFlowConfiguration;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;

@Component
public class SdxUpgradeDatabaseServerFlowConfig extends AbstractFlowConfiguration<SdxUpgradeDatabaseServerState, SdxUpgradeDatabaseServerStateSelectors>
        implements RetryableDatalakeFlowConfiguration<SdxUpgradeDatabaseServerStateSelectors> {

    private static final List<Transition<SdxUpgradeDatabaseServerState, SdxUpgradeDatabaseServerStateSelectors>> TRANSITIONS =
            new Transition.Builder<SdxUpgradeDatabaseServerState, SdxUpgradeDatabaseServerStateSelectors>()
                    .defaultFailureEvent(SDX_UPGRADE_DATABASE_SERVER_FAILED_EVENT)

                    .from(INIT_STATE).to(SDX_UPGRADE_DATABASE_SERVER_UPGRADE_STATE)
                    .event(SDX_UPGRADE_DATABASE_SERVER_UPGRADE_EVENT).defaultFailureEvent()

                    .from(SDX_UPGRADE_DATABASE_SERVER_UPGRADE_STATE).to(SDX_UPGRADE_DATABASE_SERVER_WAIT_UPGRADE_STATE)
                    .event(SDX_UPGRADE_DATABASE_SERVER_SUCCESS_EVENT).defaultFailureEvent()

                    .from(SDX_UPGRADE_DATABASE_SERVER_WAIT_UPGRADE_STATE).to(SDX_UPGRADE_DATABASE_SERVER_FINISHED_STATE)
                    .event(SDX_UPGRADE_DATABASE_SERVER_WAIT_SUCCESS_EVENT).defaultFailureEvent()

                    .from(SDX_UPGRADE_DATABASE_SERVER_FINISHED_STATE).to(FINAL_STATE)
                    .event(SDX_UPGRADE_DATABASE_SERVER_FINALIZED_EVENT).defaultFailureEvent()
                    .build();

    private static final FlowEdgeConfig<SdxUpgradeDatabaseServerState, SdxUpgradeDatabaseServerStateSelectors> EDGE_CONFIG =
            new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, SDX_UPGRADE_DATABASE_SERVER_FAILED_STATE, SDX_UPGRADE_DATABASE_SERVER_FAILED_HANDLED_EVENT);

    public SdxUpgradeDatabaseServerFlowConfig() {
        super(SdxUpgradeDatabaseServerState.class, SdxUpgradeDatabaseServerStateSelectors.class);
    }

    @Override
    public SdxUpgradeDatabaseServerStateSelectors[] getEvents() {
        return SdxUpgradeDatabaseServerStateSelectors.values();
    }

    @Override
    public SdxUpgradeDatabaseServerStateSelectors[] getInitEvents() {
        return new SdxUpgradeDatabaseServerStateSelectors[]{
                SDX_UPGRADE_DATABASE_SERVER_UPGRADE_EVENT
        };
    }

    @Override
    public String getDisplayName() {
        return "Upgrade Database Server";
    }

    @Override
    protected List<Transition<SdxUpgradeDatabaseServerState, SdxUpgradeDatabaseServerStateSelectors>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    public FlowEdgeConfig<SdxUpgradeDatabaseServerState, SdxUpgradeDatabaseServerStateSelectors> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public SdxUpgradeDatabaseServerStateSelectors getRetryableEvent() {
        return SDX_UPGRADE_DATABASE_SERVER_FAILED_HANDLED_EVENT;
    }

    @Override
    public List<SdxUpgradeDatabaseServerStateSelectors> getStackRetryEvents() {
        return List.of(SDX_UPGRADE_DATABASE_SERVER_SUCCESS_EVENT);
    }
}
