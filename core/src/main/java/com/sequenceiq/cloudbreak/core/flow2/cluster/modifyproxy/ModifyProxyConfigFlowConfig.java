package com.sequenceiq.cloudbreak.core.flow2.cluster.modifyproxy;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.modifyproxy.ModifyProxyConfigState.FINAL_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.modifyproxy.ModifyProxyConfigState.INIT_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.modifyproxy.ModifyProxyConfigState.MODIFY_PROXY_FAILED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.modifyproxy.ModifyProxyConfigState.MODIFY_PROXY_FINISHED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.modifyproxy.ModifyProxyConfigState.MODIFY_PROXY_ON_CM_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.modifyproxy.ModifyProxyConfigState.MODIFY_PROXY_SALT_STATE_APPLY_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.flow2.StackStatusFinalizerAbstractFlowConfig;

@Component
public class ModifyProxyConfigFlowConfig extends StackStatusFinalizerAbstractFlowConfig<ModifyProxyConfigState, ModifyProxyConfigEvent> {

    private static final List<Transition<ModifyProxyConfigState, ModifyProxyConfigEvent>> TRANSITIONS =
            new Transition.Builder<ModifyProxyConfigState, ModifyProxyConfigEvent>()
                    .defaultFailureEvent(ModifyProxyConfigEvent.MODIFY_PROXY_CONFIG_FAILED_EVENT)

                    .from(INIT_STATE).to(MODIFY_PROXY_SALT_STATE_APPLY_STATE).event(ModifyProxyConfigEvent.MODIFY_PROXY_CONFIG_EVENT)
                    .noFailureEvent()
                    .from(MODIFY_PROXY_SALT_STATE_APPLY_STATE).to(MODIFY_PROXY_ON_CM_STATE).event(ModifyProxyConfigEvent.MODIFY_PROXY_CONFIG_ON_CM)
                    .defaultFailureEvent()
                    .from(MODIFY_PROXY_ON_CM_STATE).to(MODIFY_PROXY_FINISHED_STATE).event(ModifyProxyConfigEvent.MODIFY_PROXY_CONFIG_SUCCESS_EVENT)
                    .defaultFailureEvent()
                    .from(MODIFY_PROXY_FINISHED_STATE).to(FINAL_STATE).event(ModifyProxyConfigEvent.MODIFY_PROXY_CONFIG_FINISHED_EVENT)
                    .defaultFailureEvent()

                    .build();

    private static final FlowEdgeConfig<ModifyProxyConfigState, ModifyProxyConfigEvent> EDGE_CONFIG =
            new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, MODIFY_PROXY_FAILED_STATE, ModifyProxyConfigEvent.MODIFY_PROXY_CONFIG_FAIL_HANDLED_EVENT);

    public ModifyProxyConfigFlowConfig() {
        super(ModifyProxyConfigState.class, ModifyProxyConfigEvent.class);
    }

    @Override
    protected List<Transition<ModifyProxyConfigState, ModifyProxyConfigEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    public FlowEdgeConfig<ModifyProxyConfigState, ModifyProxyConfigEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public ModifyProxyConfigEvent[] getEvents() {
        return ModifyProxyConfigEvent.values();
    }

    @Override
    public ModifyProxyConfigEvent[] getInitEvents() {
        return new ModifyProxyConfigEvent[]{
                ModifyProxyConfigEvent.MODIFY_PROXY_CONFIG_EVENT
        };
    }

    @Override
    public String getDisplayName() {
        return "Modify proxy config";
    }
}
