package com.sequenceiq.cloudbreak.core.flow2.cluster.ccm.upgrade;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.ccm.upgrade.UpgradeCcmEvent.FAIL_HANDLED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.ccm.upgrade.UpgradeCcmEvent.FINALIZED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.ccm.upgrade.UpgradeCcmEvent.UPGRADE_CCM_DEREGISTER_AGENTS_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.ccm.upgrade.UpgradeCcmEvent.UPGRADE_CCM_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.ccm.upgrade.UpgradeCcmEvent.UPGRADE_CCM_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.ccm.upgrade.UpgradeCcmEvent.UPGRADE_CCM_FINALIZING_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.ccm.upgrade.UpgradeCcmEvent.UPGRADE_CCM_PUSH_SALT_STATES_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.ccm.upgrade.UpgradeCcmEvent.UPGRADE_CCM_RECONFIGURE_NGINX_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.ccm.upgrade.UpgradeCcmEvent.UPGRADE_CCM_REGISTER_CLUSTER_PROXY_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.ccm.upgrade.UpgradeCcmEvent.UPGRADE_CCM_REMOVE_AGENTS_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.ccm.upgrade.UpgradeCcmEvent.UPGRADE_CCM_REVERT_ALL_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.ccm.upgrade.UpgradeCcmEvent.UPGRADE_CCM_REVERT_SALTSTATE_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.ccm.upgrade.UpgradeCcmEvent.UPGRADE_CCM_REVERT_TUNNEL_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.ccm.upgrade.UpgradeCcmEvent.UPGRADE_CCM_TUNNEL_UPDATE_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.ccm.upgrade.UpgradeCcmEvent.UPGRADE_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.ccm.upgrade.UpgradeCcmState.FINAL_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.ccm.upgrade.UpgradeCcmState.INIT_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.ccm.upgrade.UpgradeCcmState.UPGRADE_CCM_DEREGISTER_AGENT_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.ccm.upgrade.UpgradeCcmState.UPGRADE_CCM_FAILED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.ccm.upgrade.UpgradeCcmState.UPGRADE_CCM_FINALIZE_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.ccm.upgrade.UpgradeCcmState.UPGRADE_CCM_FINISHED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.ccm.upgrade.UpgradeCcmState.UPGRADE_CCM_PUSH_SALT_STATES_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.ccm.upgrade.UpgradeCcmState.UPGRADE_CCM_RECONFIGURE_NGINX_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.ccm.upgrade.UpgradeCcmState.UPGRADE_CCM_REGISTER_CLUSTER_PROXY_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.ccm.upgrade.UpgradeCcmState.UPGRADE_CCM_REMOVE_AGENT_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.ccm.upgrade.UpgradeCcmState.UPGRADE_CCM_REVERT_ALL_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.ccm.upgrade.UpgradeCcmState.UPGRADE_CCM_REVERT_SALTSTATE_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.ccm.upgrade.UpgradeCcmState.UPGRADE_CCM_REVERT_TUNNEL_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.ccm.upgrade.UpgradeCcmState.UPGRADE_CCM_TUNNEL_UPDATE_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.flow2.StackStatusFinalizerAbstractFlowConfig;
import com.sequenceiq.flow.core.FlowTriggerCondition;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration.Transition.Builder;
import com.sequenceiq.flow.core.config.RetryableFlowConfiguration;

@Component
public class UpgradeCcmFlowConfig extends StackStatusFinalizerAbstractFlowConfig<UpgradeCcmState, UpgradeCcmEvent>
        implements RetryableFlowConfiguration<UpgradeCcmEvent> {

    private static final List<Transition<UpgradeCcmState, UpgradeCcmEvent>> TRANSITIONS =
            new Builder<UpgradeCcmState, UpgradeCcmEvent>()
                    .defaultFailureEvent(UPGRADE_CCM_FAILED_EVENT)
                    .from(INIT_STATE).to(UPGRADE_CCM_TUNNEL_UPDATE_STATE)
                    .event(UPGRADE_CCM_EVENT)
                    .defaultFailureEvent()

                    .from(UPGRADE_CCM_TUNNEL_UPDATE_STATE).to(UPGRADE_CCM_PUSH_SALT_STATES_STATE)
                    .event(UPGRADE_CCM_TUNNEL_UPDATE_FINISHED_EVENT)
                    .defaultFailureEvent()

                    .from(UPGRADE_CCM_PUSH_SALT_STATES_STATE).to(UPGRADE_CCM_RECONFIGURE_NGINX_STATE)
                    .event(UPGRADE_CCM_PUSH_SALT_STATES_FINISHED_EVENT)
                    .defaultFailureEvent()

                    .from(UPGRADE_CCM_PUSH_SALT_STATES_STATE).to(UPGRADE_CCM_REVERT_TUNNEL_STATE)
                    .event(UPGRADE_CCM_REVERT_TUNNEL_EVENT)
                    .defaultFailureEvent()

                    .from(UPGRADE_CCM_RECONFIGURE_NGINX_STATE).to(UPGRADE_CCM_REGISTER_CLUSTER_PROXY_STATE)
                    .event(UPGRADE_CCM_RECONFIGURE_NGINX_FINISHED_EVENT)
                    .defaultFailureEvent()

                    .from(UPGRADE_CCM_RECONFIGURE_NGINX_STATE).to(UPGRADE_CCM_REVERT_SALTSTATE_STATE)
                    .event(UPGRADE_CCM_REVERT_SALTSTATE_EVENT)
                    .defaultFailureEvent()

                    .from(UPGRADE_CCM_REGISTER_CLUSTER_PROXY_STATE).to(UPGRADE_CCM_REMOVE_AGENT_STATE)
                    .event(UPGRADE_CCM_REGISTER_CLUSTER_PROXY_FINISHED_EVENT)
                    .defaultFailureEvent()

                    .from(UPGRADE_CCM_REGISTER_CLUSTER_PROXY_STATE).to(UPGRADE_CCM_REVERT_ALL_STATE)
                    .event(UPGRADE_CCM_REVERT_ALL_EVENT)
                    .defaultFailureEvent()

                    .from(UPGRADE_CCM_REMOVE_AGENT_STATE).to(UPGRADE_CCM_DEREGISTER_AGENT_STATE)
                    .event(UPGRADE_CCM_REMOVE_AGENTS_FINISHED_EVENT)
                    .defaultFailureEvent()

                    .from(UPGRADE_CCM_DEREGISTER_AGENT_STATE).to(UPGRADE_CCM_FINALIZE_STATE)
                    .event(UPGRADE_CCM_DEREGISTER_AGENTS_FINISHED_EVENT)
                    .defaultFailureEvent()

                    .from(UPGRADE_CCM_FINALIZE_STATE).to(UPGRADE_CCM_FINISHED_STATE)
                    .event(UPGRADE_CCM_FINALIZING_FINISHED_EVENT)
                    .defaultFailureEvent()

                    .from(UPGRADE_CCM_REVERT_TUNNEL_STATE).to(UPGRADE_CCM_FINISHED_STATE)
                    .event(UPGRADE_FINISHED_EVENT)
                    .defaultFailureEvent()

                    .from(UPGRADE_CCM_REVERT_SALTSTATE_STATE).to(UPGRADE_CCM_FINISHED_STATE)
                    .event(UPGRADE_FINISHED_EVENT)
                    .defaultFailureEvent()

                    .from(UPGRADE_CCM_REVERT_ALL_STATE).to(UPGRADE_CCM_FINISHED_STATE)
                    .event(UPGRADE_FINISHED_EVENT)
                    .defaultFailureEvent()

                    .from(UPGRADE_CCM_FINISHED_STATE).to(FINAL_STATE)
                    .event(FINALIZED_EVENT)
                    .defaultFailureEvent()

                    .build();

    private static final FlowEdgeConfig<UpgradeCcmState, UpgradeCcmEvent> EDGE_CONFIG = new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE,
            UPGRADE_CCM_FAILED_STATE, FAIL_HANDLED_EVENT);

    public UpgradeCcmFlowConfig() {
        super(UpgradeCcmState.class, UpgradeCcmEvent.class);
    }

    @Override
    public FlowTriggerCondition getFlowTriggerCondition() {
        return getApplicationContext().getBean(CcmUpgradeFlowTriggerCondition.class);
    }

    @Override
    protected List<Transition<UpgradeCcmState, UpgradeCcmEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    public FlowEdgeConfig<UpgradeCcmState, UpgradeCcmEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public UpgradeCcmEvent[] getEvents() {
        return UpgradeCcmEvent.values();
    }

    @Override
    public UpgradeCcmEvent[] getInitEvents() {
        return new UpgradeCcmEvent[]{
                UPGRADE_CCM_EVENT
        };
    }

    @Override
    public String getDisplayName() {
        return "CCM upgrade";
    }

    @Override
    public UpgradeCcmEvent getRetryableEvent() {
        return FAIL_HANDLED_EVENT;
    }
}
