package com.sequenceiq.cloudbreak.core.flow2.stack.repair;

import com.sequenceiq.cloudbreak.core.flow2.config.AbstractFlowConfiguration;
import org.springframework.stereotype.Component;

import static com.sequenceiq.cloudbreak.core.flow2.stack.repair.ManualStackRepairTriggerEvent.MANUAL_STACK_REPAIR_TRIGGER_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.repair.ManualStackRepairTriggerEvent.MANUAL_STACK_REPAIR_TRIGGER_FAILURE_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.repair.ManualStackRepairTriggerEvent.MANUAL_STACK_REPAIR_TRIGGER_FAILURE_HANDLED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.repair.ManualStackRepairTriggerEvent.NOTIFY_REPAIR_SERVICE_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.repair.ManualStackRepairTriggerEvent.REPAIR_SERVICE_NOTIFIED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.repair.ManualStackRepairTriggerState.INIT_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.repair.ManualStackRepairTriggerState.UNHEALTHY_INSTANCES_DETECTION_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.repair.ManualStackRepairTriggerState.NOTIFY_STACK_REPAIR_SERVICE_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.repair.ManualStackRepairTriggerState.MANUAL_STACK_REPAIR_TRIGGER_FAILED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.repair.ManualStackRepairTriggerState.FINAL_STATE;

import java.util.List;

@Component
public class ManualStackRepairTriggerFlowConfig extends AbstractFlowConfiguration<ManualStackRepairTriggerState, ManualStackRepairTriggerEvent> {

    private static final List<Transition<ManualStackRepairTriggerState, ManualStackRepairTriggerEvent>> TRANSITIONS =
            new Transition.Builder<ManualStackRepairTriggerState, ManualStackRepairTriggerEvent>()
                .defaultFailureEvent(MANUAL_STACK_REPAIR_TRIGGER_FAILURE_EVENT)
                .from(INIT_STATE).to(UNHEALTHY_INSTANCES_DETECTION_STATE).event(MANUAL_STACK_REPAIR_TRIGGER_EVENT).defaultFailureEvent()
                .from(UNHEALTHY_INSTANCES_DETECTION_STATE).to(NOTIFY_STACK_REPAIR_SERVICE_STATE).event(NOTIFY_REPAIR_SERVICE_EVENT).defaultFailureEvent()
                .from(NOTIFY_STACK_REPAIR_SERVICE_STATE).to(FINAL_STATE).event(REPAIR_SERVICE_NOTIFIED_EVENT).defaultFailureEvent()
                .build();

    private static final FlowEdgeConfig<ManualStackRepairTriggerState, ManualStackRepairTriggerEvent> EDGE_CONFIG =
            new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, MANUAL_STACK_REPAIR_TRIGGER_FAILED_STATE, MANUAL_STACK_REPAIR_TRIGGER_FAILURE_HANDLED_EVENT);

    public ManualStackRepairTriggerFlowConfig() {
        super(ManualStackRepairTriggerState.class, ManualStackRepairTriggerEvent.class);
    }

    @Override
    protected List<Transition<ManualStackRepairTriggerState, ManualStackRepairTriggerEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    protected FlowEdgeConfig<ManualStackRepairTriggerState, ManualStackRepairTriggerEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public ManualStackRepairTriggerEvent[] getEvents() {
        return ManualStackRepairTriggerEvent.values();
    }

    @Override
    public ManualStackRepairTriggerEvent[] getInitEvents() {
        return new ManualStackRepairTriggerEvent[] {
                ManualStackRepairTriggerEvent.MANUAL_STACK_REPAIR_TRIGGER_EVENT
        };
    }
}
