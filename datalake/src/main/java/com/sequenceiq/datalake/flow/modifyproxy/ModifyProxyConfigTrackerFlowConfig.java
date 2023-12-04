package com.sequenceiq.datalake.flow.modifyproxy;

import static com.sequenceiq.datalake.flow.modifyproxy.ModifyProxyConfigTrackerEvent.MODIFY_PROXY_CONFIG_EVENT;
import static com.sequenceiq.datalake.flow.modifyproxy.ModifyProxyConfigTrackerEvent.MODIFY_PROXY_CONFIG_FAIL_HANDLED_EVENT;
import static com.sequenceiq.datalake.flow.modifyproxy.ModifyProxyConfigTrackerEvent.MODIFY_PROXY_CONFIG_FINISHED_EVENT;
import static com.sequenceiq.datalake.flow.modifyproxy.ModifyProxyConfigTrackerEvent.MODIFY_PROXY_CONFIG_SUCCESS_EVENT;
import static com.sequenceiq.datalake.flow.modifyproxy.ModifyProxyConfigTrackerState.FINAL_STATE;
import static com.sequenceiq.datalake.flow.modifyproxy.ModifyProxyConfigTrackerState.INIT_STATE;
import static com.sequenceiq.datalake.flow.modifyproxy.ModifyProxyConfigTrackerState.MODIFY_PROXY_CONFIG_FAILED_STATE;
import static com.sequenceiq.datalake.flow.modifyproxy.ModifyProxyConfigTrackerState.MODIFY_PROXY_CONFIG_SUCCESS_STATE;
import static com.sequenceiq.datalake.flow.modifyproxy.ModifyProxyConfigTrackerState.MODIFY_PROXY_CONFIG_WAITING_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;

@Component
public class ModifyProxyConfigTrackerFlowConfig extends AbstractFlowConfiguration<ModifyProxyConfigTrackerState, ModifyProxyConfigTrackerEvent> {

    private static final List<Transition<ModifyProxyConfigTrackerState, ModifyProxyConfigTrackerEvent>> TRANSITIONS =
            new Transition.Builder<ModifyProxyConfigTrackerState, ModifyProxyConfigTrackerEvent>()
                    .defaultFailureEvent(ModifyProxyConfigTrackerEvent.MODIFY_PROXY_CONFIG_FAILED_EVENT)

                    .from(INIT_STATE).to(MODIFY_PROXY_CONFIG_WAITING_STATE).event(MODIFY_PROXY_CONFIG_EVENT).noFailureEvent()
                    .from(MODIFY_PROXY_CONFIG_WAITING_STATE).to(MODIFY_PROXY_CONFIG_SUCCESS_STATE).event(MODIFY_PROXY_CONFIG_SUCCESS_EVENT)
                    .defaultFailureEvent()
                    .from(MODIFY_PROXY_CONFIG_SUCCESS_STATE).to(FINAL_STATE).event(MODIFY_PROXY_CONFIG_FINISHED_EVENT)
                    .defaultFailureEvent()

                    .build();

    private static final FlowEdgeConfig<ModifyProxyConfigTrackerState, ModifyProxyConfigTrackerEvent> EDGE_CONFIG =
            new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, MODIFY_PROXY_CONFIG_FAILED_STATE, MODIFY_PROXY_CONFIG_FAIL_HANDLED_EVENT);

    public ModifyProxyConfigTrackerFlowConfig() {
        super(ModifyProxyConfigTrackerState.class, ModifyProxyConfigTrackerEvent.class);
    }

    @Override
    protected List<Transition<ModifyProxyConfigTrackerState, ModifyProxyConfigTrackerEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    public FlowEdgeConfig<ModifyProxyConfigTrackerState, ModifyProxyConfigTrackerEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public ModifyProxyConfigTrackerEvent[] getEvents() {
        return ModifyProxyConfigTrackerEvent.values();
    }

    @Override
    public ModifyProxyConfigTrackerEvent[] getInitEvents() {
        return new ModifyProxyConfigTrackerEvent[]{
                MODIFY_PROXY_CONFIG_EVENT
        };
    }

    @Override
    public String getDisplayName() {
        return "Modify proxy config";
    }
}
