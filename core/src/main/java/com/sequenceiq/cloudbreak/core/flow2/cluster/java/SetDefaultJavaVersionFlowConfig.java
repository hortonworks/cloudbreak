package com.sequenceiq.cloudbreak.core.flow2.cluster.java;

import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value.SET_DEFAULT_JAVA_VERSION_FAILED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value.SET_DEFAULT_JAVA_VERSION_FINISHED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value.SET_DEFAULT_JAVA_VERSION_STARTED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value.UNSET;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.java.SetDefaultJavaVersionFlowEvent.SET_DEFAULT_JAVA_VERSION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.java.SetDefaultJavaVersionFlowEvent.SET_DEFAULT_JAVA_VERSION_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.java.SetDefaultJavaVersionFlowEvent.SET_DEFAULT_JAVA_VERSION_FAIL_HANDLED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.java.SetDefaultJavaVersionFlowEvent.SET_DEFAULT_JAVA_VERSION_FINALIZED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.java.SetDefaultJavaVersionFlowEvent.SET_DEFAULT_JAVA_VERSION_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.java.SetDefaultJavaVersionFlowState.FINAL_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.java.SetDefaultJavaVersionFlowState.INIT_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.java.SetDefaultJavaVersionFlowState.SET_DEFAULT_JAVA_VERSION_FAILED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.java.SetDefaultJavaVersionFlowState.SET_DEFAULT_JAVA_VERSION_FINISED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.java.SetDefaultJavaVersionFlowState.SET_DEFAULT_JAVA_VERSION_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.core.flow2.StackStatusFinalizerAbstractFlowConfig;
import com.sequenceiq.cloudbreak.structuredevent.service.telemetry.mapper.ClusterUseCaseAware;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration.Transition.Builder;

@Component
public class SetDefaultJavaVersionFlowConfig extends StackStatusFinalizerAbstractFlowConfig<SetDefaultJavaVersionFlowState, SetDefaultJavaVersionFlowEvent>
        implements ClusterUseCaseAware {

    public static final FlowEdgeConfig<SetDefaultJavaVersionFlowState, SetDefaultJavaVersionFlowEvent> EDGE_CONFIG =
            new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, SET_DEFAULT_JAVA_VERSION_FAILED_STATE, SET_DEFAULT_JAVA_VERSION_FAIL_HANDLED_EVENT);

    private static final List<Transition<SetDefaultJavaVersionFlowState, SetDefaultJavaVersionFlowEvent>> TRANSITIONS =
            new Builder<SetDefaultJavaVersionFlowState, SetDefaultJavaVersionFlowEvent>()
                    .defaultFailureEvent(SET_DEFAULT_JAVA_VERSION_FAILED_EVENT)

                    .from(INIT_STATE)
                    .to(SET_DEFAULT_JAVA_VERSION_STATE)
                    .event(SET_DEFAULT_JAVA_VERSION_EVENT)
                    .defaultFailureEvent()

                    .from(SET_DEFAULT_JAVA_VERSION_STATE)
                    .to(SET_DEFAULT_JAVA_VERSION_FINISED_STATE)
                    .event(SET_DEFAULT_JAVA_VERSION_FINISHED_EVENT)
                    .defaultFailureEvent()

                    .from(SET_DEFAULT_JAVA_VERSION_FINISED_STATE)
                    .to(FINAL_STATE)
                    .event(SET_DEFAULT_JAVA_VERSION_FINALIZED_EVENT)
                    .defaultFailureEvent()
                    .build();

    protected SetDefaultJavaVersionFlowConfig() {
        super(SetDefaultJavaVersionFlowState.class, SetDefaultJavaVersionFlowEvent.class);
    }

    @Override
    protected List<Transition<SetDefaultJavaVersionFlowState, SetDefaultJavaVersionFlowEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    public FlowEdgeConfig<SetDefaultJavaVersionFlowState, SetDefaultJavaVersionFlowEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public SetDefaultJavaVersionFlowEvent[] getEvents() {
        return SetDefaultJavaVersionFlowEvent.values();
    }

    @Override
    public SetDefaultJavaVersionFlowEvent[] getInitEvents() {
        return new SetDefaultJavaVersionFlowEvent[] {
                SET_DEFAULT_JAVA_VERSION_EVENT
        };
    }

    @Override
    public String getDisplayName() {
        return "Set default Java version";
    }

    @Override
    public UsageProto.CDPClusterStatus.Value getUseCaseForFlowState(Enum<? extends FlowState> flowState) {
        if (INIT_STATE.equals(flowState)) {
            return SET_DEFAULT_JAVA_VERSION_STARTED;
        } else if (SET_DEFAULT_JAVA_VERSION_FINISED_STATE.equals(flowState)) {
            return SET_DEFAULT_JAVA_VERSION_FINISHED;
        } else if (flowState.toString().endsWith("FAILED_STATE")) {
            return SET_DEFAULT_JAVA_VERSION_FAILED;
        } else {
            return UNSET;
        }
    }
}
