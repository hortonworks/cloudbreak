package com.sequenceiq.freeipa.flow.stack.upgrade.ccm;

import static com.sequenceiq.freeipa.flow.stack.upgrade.ccm.UpgradeCcmState.FINAL_STATE;
import static com.sequenceiq.freeipa.flow.stack.upgrade.ccm.UpgradeCcmState.INIT_STATE;
import static com.sequenceiq.freeipa.flow.stack.upgrade.ccm.UpgradeCcmState.UPGRADE_CCM_CHANGE_TUNNEL_STATE;
import static com.sequenceiq.freeipa.flow.stack.upgrade.ccm.UpgradeCcmState.UPGRADE_CCM_CHECK_PREREQUISITES_STATE;
import static com.sequenceiq.freeipa.flow.stack.upgrade.ccm.UpgradeCcmState.UPGRADE_CCM_DEREGISTER_MINA_STATE;
import static com.sequenceiq.freeipa.flow.stack.upgrade.ccm.UpgradeCcmState.UPGRADE_CCM_FAILED_STATE;
import static com.sequenceiq.freeipa.flow.stack.upgrade.ccm.UpgradeCcmState.UPGRADE_CCM_FINALIZE_FAILED_STATE;
import static com.sequenceiq.freeipa.flow.stack.upgrade.ccm.UpgradeCcmState.UPGRADE_CCM_FINALIZING_STATE;
import static com.sequenceiq.freeipa.flow.stack.upgrade.ccm.UpgradeCcmState.UPGRADE_CCM_FINISHED_STATE;
import static com.sequenceiq.freeipa.flow.stack.upgrade.ccm.UpgradeCcmState.UPGRADE_CCM_OBTAIN_AGENT_DATA_STATE;
import static com.sequenceiq.freeipa.flow.stack.upgrade.ccm.UpgradeCcmState.UPGRADE_CCM_PUSH_SALT_STATES_STATE;
import static com.sequenceiq.freeipa.flow.stack.upgrade.ccm.UpgradeCcmState.UPGRADE_CCM_RECONFIGURE_NGINX_STATE;
import static com.sequenceiq.freeipa.flow.stack.upgrade.ccm.UpgradeCcmState.UPGRADE_CCM_REGISTER_CLUSTER_PROXY_STATE;
import static com.sequenceiq.freeipa.flow.stack.upgrade.ccm.UpgradeCcmState.UPGRADE_CCM_REMOVE_MINA_STATE;
import static com.sequenceiq.freeipa.flow.stack.upgrade.ccm.UpgradeCcmState.UPGRADE_CCM_REVERT_ALL_FAILURE_STATE;
import static com.sequenceiq.freeipa.flow.stack.upgrade.ccm.UpgradeCcmState.UPGRADE_CCM_REVERT_FAILURE_STATE;
import static com.sequenceiq.freeipa.flow.stack.upgrade.ccm.UpgradeCcmState.UPGRADE_CCM_UPGRADE_STATE;
import static com.sequenceiq.freeipa.flow.stack.upgrade.ccm.selector.UpgradeCcmStateSelector.UPGRADE_CCM_CHECK_PREREQUISITES_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.stack.upgrade.ccm.selector.UpgradeCcmStateSelector.UPGRADE_CCM_CLEANING_FAILED_EVENT;
import static com.sequenceiq.freeipa.flow.stack.upgrade.ccm.selector.UpgradeCcmStateSelector.UPGRADE_CCM_DEREGISTER_MINA_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.stack.upgrade.ccm.selector.UpgradeCcmStateSelector.UPGRADE_CCM_FAILED_EVENT;
import static com.sequenceiq.freeipa.flow.stack.upgrade.ccm.selector.UpgradeCcmStateSelector.UPGRADE_CCM_FAILED_REVERT_ALL_EVENT;
import static com.sequenceiq.freeipa.flow.stack.upgrade.ccm.selector.UpgradeCcmStateSelector.UPGRADE_CCM_FAILED_REVERT_EVENT;
import static com.sequenceiq.freeipa.flow.stack.upgrade.ccm.selector.UpgradeCcmStateSelector.UPGRADE_CCM_FAILURE_HANDLED_EVENT;
import static com.sequenceiq.freeipa.flow.stack.upgrade.ccm.selector.UpgradeCcmStateSelector.UPGRADE_CCM_FINALIZE_FAILED_EVENT;
import static com.sequenceiq.freeipa.flow.stack.upgrade.ccm.selector.UpgradeCcmStateSelector.UPGRADE_CCM_FINALIZING_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.stack.upgrade.ccm.selector.UpgradeCcmStateSelector.UPGRADE_CCM_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.stack.upgrade.ccm.selector.UpgradeCcmStateSelector.UPGRADE_CCM_OBTAIN_AGENT_DATA_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.stack.upgrade.ccm.selector.UpgradeCcmStateSelector.UPGRADE_CCM_PUSH_SALT_STATES_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.stack.upgrade.ccm.selector.UpgradeCcmStateSelector.UPGRADE_CCM_RECONFIGURE_NGINX_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.stack.upgrade.ccm.selector.UpgradeCcmStateSelector.UPGRADE_CCM_REGISTER_CLUSTER_PROXY_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.stack.upgrade.ccm.selector.UpgradeCcmStateSelector.UPGRADE_CCM_REMOVE_MINA_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.stack.upgrade.ccm.selector.UpgradeCcmStateSelector.UPGRADE_CCM_TRIGGER_EVENT;
import static com.sequenceiq.freeipa.flow.stack.upgrade.ccm.selector.UpgradeCcmStateSelector.UPGRADE_CCM_TUNNEL_CHANGE_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.stack.upgrade.ccm.selector.UpgradeCcmStateSelector.UPGRADE_CCM_UPGRADE_FINISHED_EVENT;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.flow.core.config.RetryableFlowConfiguration;
import com.sequenceiq.freeipa.flow.StackStatusFinalizerAbstractFlowConfig;
import com.sequenceiq.freeipa.flow.stack.upgrade.ccm.selector.UpgradeCcmStateSelector;

@Component
public class UpgradeCcmFlowConfig extends StackStatusFinalizerAbstractFlowConfig<UpgradeCcmState, UpgradeCcmStateSelector>
        implements RetryableFlowConfiguration<UpgradeCcmStateSelector> {

    private static final UpgradeCcmStateSelector[] INIT_EVENTS = { UPGRADE_CCM_TRIGGER_EVENT };

    private static final FlowEdgeConfig<UpgradeCcmState, UpgradeCcmStateSelector> EDGE_CONFIG =
            new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, UPGRADE_CCM_FAILED_STATE, UPGRADE_CCM_FAILURE_HANDLED_EVENT);

    private static final List<Transition<UpgradeCcmState, UpgradeCcmStateSelector>> TRANSITIONS =
            new Transition.Builder<UpgradeCcmState, UpgradeCcmStateSelector>().defaultFailureEvent(UPGRADE_CCM_FAILED_EVENT)
                    .from(INIT_STATE)
                    .to(UPGRADE_CCM_CHECK_PREREQUISITES_STATE)
                    .event(UPGRADE_CCM_TRIGGER_EVENT)
                    .defaultFailureEvent()

                    .from(UPGRADE_CCM_CHECK_PREREQUISITES_STATE)
                    .to(UPGRADE_CCM_CHANGE_TUNNEL_STATE)
                    .event(UPGRADE_CCM_CHECK_PREREQUISITES_FINISHED_EVENT)
                    .defaultFailureEvent()

                    .from(UPGRADE_CCM_CHANGE_TUNNEL_STATE)
                    .to(UPGRADE_CCM_OBTAIN_AGENT_DATA_STATE)
                    .event(UPGRADE_CCM_TUNNEL_CHANGE_FINISHED_EVENT)
                    .defaultFailureEvent()

                    .from(UPGRADE_CCM_OBTAIN_AGENT_DATA_STATE)
                    .to(UPGRADE_CCM_PUSH_SALT_STATES_STATE)
                    .event(UPGRADE_CCM_OBTAIN_AGENT_DATA_FINISHED_EVENT)
                    .defaultFailureEvent()

                    .from(UPGRADE_CCM_PUSH_SALT_STATES_STATE)
                    .to(UPGRADE_CCM_UPGRADE_STATE)
                    .event(UPGRADE_CCM_PUSH_SALT_STATES_FINISHED_EVENT)
                    .defaultFailureEvent()

                    .from(UPGRADE_CCM_UPGRADE_STATE)
                    .to(UPGRADE_CCM_RECONFIGURE_NGINX_STATE)
                    .event(UPGRADE_CCM_UPGRADE_FINISHED_EVENT)
                    .defaultFailureEvent()

                    .from(UPGRADE_CCM_UPGRADE_STATE)
                    .to(UPGRADE_CCM_REVERT_FAILURE_STATE)
                    .event(UPGRADE_CCM_FAILED_REVERT_EVENT)
                    .defaultFailureEvent()

                    .from(UPGRADE_CCM_RECONFIGURE_NGINX_STATE)
                    .to(UPGRADE_CCM_REGISTER_CLUSTER_PROXY_STATE)
                    .event(UPGRADE_CCM_RECONFIGURE_NGINX_FINISHED_EVENT)
                    .defaultFailureEvent()

                    .from(UPGRADE_CCM_RECONFIGURE_NGINX_STATE)
                    .to(UPGRADE_CCM_REVERT_FAILURE_STATE)
                    .event(UPGRADE_CCM_FAILED_REVERT_EVENT)
                    .defaultFailureEvent()

                    .from(UPGRADE_CCM_REGISTER_CLUSTER_PROXY_STATE)
                    .to(UPGRADE_CCM_REMOVE_MINA_STATE)
                    .event(UPGRADE_CCM_REGISTER_CLUSTER_PROXY_FINISHED_EVENT)
                    .defaultFailureEvent()

                    .from(UPGRADE_CCM_REGISTER_CLUSTER_PROXY_STATE)
                    .to(UPGRADE_CCM_REVERT_ALL_FAILURE_STATE)
                    .event(UPGRADE_CCM_FAILED_REVERT_ALL_EVENT)
                    .defaultFailureEvent()

                    .from(UPGRADE_CCM_REVERT_FAILURE_STATE)
                    .to(FINAL_STATE)
                    .event(UPGRADE_CCM_FINISHED_EVENT)
                    .defaultFailureEvent()

                    .from(UPGRADE_CCM_REVERT_ALL_FAILURE_STATE)
                    .to(FINAL_STATE)
                    .event(UPGRADE_CCM_FINISHED_EVENT)
                    .defaultFailureEvent()

                    .from(UPGRADE_CCM_REMOVE_MINA_STATE)
                    .to(UPGRADE_CCM_DEREGISTER_MINA_STATE)
                    .event(UPGRADE_CCM_REMOVE_MINA_FINISHED_EVENT)
                    .defaultFailureEvent()

                    .from(UPGRADE_CCM_REMOVE_MINA_STATE)
                    .to(UPGRADE_CCM_FINALIZING_STATE)
                    .event(UPGRADE_CCM_CLEANING_FAILED_EVENT)
                    .defaultFailureEvent()

                    .from(UPGRADE_CCM_DEREGISTER_MINA_STATE)
                    .to(UPGRADE_CCM_FINALIZING_STATE)
                    .event(UPGRADE_CCM_DEREGISTER_MINA_FINISHED_EVENT)
                    .defaultFailureEvent()

                    .from(UPGRADE_CCM_FINALIZING_STATE)
                    .to(UPGRADE_CCM_FINISHED_STATE)
                    .event(UPGRADE_CCM_FINALIZING_FINISHED_EVENT)
                    .failureState(UPGRADE_CCM_FINALIZE_FAILED_STATE).failureEvent(UPGRADE_CCM_FINALIZE_FAILED_EVENT)

                    .from(UPGRADE_CCM_FINISHED_STATE)
                    .to(FINAL_STATE)
                    .event(UPGRADE_CCM_FINISHED_EVENT)
                    .defaultFailureEvent()
                    .build();

    public UpgradeCcmFlowConfig() {
        super(UpgradeCcmState.class, UpgradeCcmStateSelector.class);
    }

    @Override
    public UpgradeCcmStateSelector[] getInitEvents() {
        return INIT_EVENTS;
    }

    @Override
    public UpgradeCcmStateSelector[] getEvents() {
        return UpgradeCcmStateSelector.values();
    }

    @Override
    public String getDisplayName() {
        return "Upgrade CCM";
    }

    @Override
    public FlowEdgeConfig<UpgradeCcmState, UpgradeCcmStateSelector> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    protected List<Transition<UpgradeCcmState, UpgradeCcmStateSelector>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    public UpgradeCcmStateSelector getRetryableEvent() {
        return EDGE_CONFIG.getFailureHandled();
    }

}
