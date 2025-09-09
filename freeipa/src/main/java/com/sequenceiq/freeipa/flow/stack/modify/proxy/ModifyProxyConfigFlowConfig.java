package com.sequenceiq.freeipa.flow.stack.modify.proxy;


import static com.sequenceiq.freeipa.flow.stack.modify.proxy.ModifyProxyConfigState.FINAL_STATE;
import static com.sequenceiq.freeipa.flow.stack.modify.proxy.ModifyProxyConfigState.INIT_STATE;
import static com.sequenceiq.freeipa.flow.stack.modify.proxy.ModifyProxyConfigState.MODIFY_PROXY_FAILED_STATE;
import static com.sequenceiq.freeipa.flow.stack.modify.proxy.ModifyProxyConfigState.MODIFY_PROXY_FINISHED_STATE;
import static com.sequenceiq.freeipa.flow.stack.modify.proxy.ModifyProxyConfigState.MODIFY_PROXY_SALT_STATE_APPLY_STATE;
import static com.sequenceiq.freeipa.flow.stack.modify.proxy.selector.ModifyProxyConfigEvent.MODIFY_PROXY_FAILED_EVENT;
import static com.sequenceiq.freeipa.flow.stack.modify.proxy.selector.ModifyProxyConfigEvent.MODIFY_PROXY_FAILURE_HANDLED_EVENT;
import static com.sequenceiq.freeipa.flow.stack.modify.proxy.selector.ModifyProxyConfigEvent.MODIFY_PROXY_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.stack.modify.proxy.selector.ModifyProxyConfigEvent.MODIFY_PROXY_SUCCESS_EVENT;
import static com.sequenceiq.freeipa.flow.stack.modify.proxy.selector.ModifyProxyConfigEvent.MODIFY_PROXY_TRIGGER_EVENT;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.flow.core.config.RetryableFlowConfiguration;
import com.sequenceiq.freeipa.flow.StackStatusFinalizerAbstractFlowConfig;
import com.sequenceiq.freeipa.flow.stack.modify.proxy.selector.ModifyProxyConfigEvent;

@Component
public class ModifyProxyConfigFlowConfig extends StackStatusFinalizerAbstractFlowConfig<ModifyProxyConfigState, ModifyProxyConfigEvent>
        implements RetryableFlowConfiguration<ModifyProxyConfigEvent> {

    private static final FlowEdgeConfig<ModifyProxyConfigState, ModifyProxyConfigEvent> EDGE_CONFIG =
            new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, MODIFY_PROXY_FAILED_STATE, MODIFY_PROXY_FAILURE_HANDLED_EVENT);

    private static final List<Transition<ModifyProxyConfigState, ModifyProxyConfigEvent>> TRANSITIONS =
            new Transition.Builder<ModifyProxyConfigState, ModifyProxyConfigEvent>().defaultFailureEvent(MODIFY_PROXY_FAILED_EVENT)

                    .from(INIT_STATE)
                    .to(MODIFY_PROXY_SALT_STATE_APPLY_STATE)
                    .event(MODIFY_PROXY_TRIGGER_EVENT)
                    .defaultFailureEvent()

                    .from(MODIFY_PROXY_SALT_STATE_APPLY_STATE)
                    .to(MODIFY_PROXY_FINISHED_STATE)
                    .event(MODIFY_PROXY_SUCCESS_EVENT)
                    .defaultFailureEvent()

                    .from(MODIFY_PROXY_FINISHED_STATE)
                    .to(FINAL_STATE)
                    .event(MODIFY_PROXY_FINISHED_EVENT)
                    .defaultFailureEvent()

                    .build();

    protected ModifyProxyConfigFlowConfig() {
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
        return new ModifyProxyConfigEvent[]{ModifyProxyConfigEvent.MODIFY_PROXY_TRIGGER_EVENT};
    }

    @Override
    public String getDisplayName() {
        return "Modify proxy config";
    }

    @Override
    public ModifyProxyConfigEvent getRetryableEvent() {
        return EDGE_CONFIG.getFailureHandled();
    }
}
