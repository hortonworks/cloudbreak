package com.sequenceiq.datalake.flow.java;


import static com.sequenceiq.datalake.flow.java.SetDatalakeDefaultJavaVersionFlowEvent.SET_DATALAKE_DEFAULT_JAVA_VERSION_EVENT;
import static com.sequenceiq.datalake.flow.java.SetDatalakeDefaultJavaVersionFlowEvent.SET_DATALAKE_DEFAULT_JAVA_VERSION_FAILED_EVENT;
import static com.sequenceiq.datalake.flow.java.SetDatalakeDefaultJavaVersionFlowEvent.SET_DATALAKE_DEFAULT_JAVA_VERSION_FAIL_HANDLED_EVENT;
import static com.sequenceiq.datalake.flow.java.SetDatalakeDefaultJavaVersionFlowEvent.SET_DATALAKE_DEFAULT_JAVA_VERSION_FINALIZED_EVENT;
import static com.sequenceiq.datalake.flow.java.SetDatalakeDefaultJavaVersionFlowEvent.SET_DATALAKE_DEFAULT_JAVA_VERSION_FINISHED_EVENT;
import static com.sequenceiq.datalake.flow.java.SetDatalakeDefaultJavaVersionFlowState.FINAL_STATE;
import static com.sequenceiq.datalake.flow.java.SetDatalakeDefaultJavaVersionFlowState.INIT_STATE;
import static com.sequenceiq.datalake.flow.java.SetDatalakeDefaultJavaVersionFlowState.SET_DATALAKE_DEFAULT_JAVA_VERSION_FAILED_STATE;
import static com.sequenceiq.datalake.flow.java.SetDatalakeDefaultJavaVersionFlowState.SET_DATALAKE_DEFAULT_JAVA_VERSION_FINISED_STATE;
import static com.sequenceiq.datalake.flow.java.SetDatalakeDefaultJavaVersionFlowState.SET_DATALAKE_DEFAULT_JAVA_VERSION_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;

@Component
public class SetDatalakeDefaultJavaVersionFlowConfig
        extends AbstractFlowConfiguration<SetDatalakeDefaultJavaVersionFlowState, SetDatalakeDefaultJavaVersionFlowEvent> {

    public static final AbstractFlowConfiguration.FlowEdgeConfig<SetDatalakeDefaultJavaVersionFlowState, SetDatalakeDefaultJavaVersionFlowEvent> EDGE_CONFIG =
            new AbstractFlowConfiguration.FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, SET_DATALAKE_DEFAULT_JAVA_VERSION_FAILED_STATE,
                    SET_DATALAKE_DEFAULT_JAVA_VERSION_FAIL_HANDLED_EVENT);

    private static final List<AbstractFlowConfiguration.Transition<SetDatalakeDefaultJavaVersionFlowState, SetDatalakeDefaultJavaVersionFlowEvent>>
            TRANSITIONS = new AbstractFlowConfiguration.Transition.Builder<SetDatalakeDefaultJavaVersionFlowState, SetDatalakeDefaultJavaVersionFlowEvent>()
                    .defaultFailureEvent(SET_DATALAKE_DEFAULT_JAVA_VERSION_FAILED_EVENT)

                    .from(INIT_STATE)
                    .to(SET_DATALAKE_DEFAULT_JAVA_VERSION_STATE)
                    .event(SET_DATALAKE_DEFAULT_JAVA_VERSION_EVENT)
                    .defaultFailureEvent()

                    .from(SET_DATALAKE_DEFAULT_JAVA_VERSION_STATE)
                    .to(SET_DATALAKE_DEFAULT_JAVA_VERSION_FINISED_STATE)
                    .event(SET_DATALAKE_DEFAULT_JAVA_VERSION_FINISHED_EVENT)
                    .defaultFailureEvent()

                    .from(SET_DATALAKE_DEFAULT_JAVA_VERSION_FINISED_STATE)
                    .to(FINAL_STATE)
                    .event(SET_DATALAKE_DEFAULT_JAVA_VERSION_FINALIZED_EVENT)
                    .defaultFailureEvent()
                    .build();

    protected SetDatalakeDefaultJavaVersionFlowConfig() {
        super(SetDatalakeDefaultJavaVersionFlowState.class, SetDatalakeDefaultJavaVersionFlowEvent.class);
    }

    @Override
    protected List<AbstractFlowConfiguration.Transition<SetDatalakeDefaultJavaVersionFlowState, SetDatalakeDefaultJavaVersionFlowEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    public AbstractFlowConfiguration.FlowEdgeConfig<SetDatalakeDefaultJavaVersionFlowState, SetDatalakeDefaultJavaVersionFlowEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public SetDatalakeDefaultJavaVersionFlowEvent[] getEvents() {
        return SetDatalakeDefaultJavaVersionFlowEvent.values();
    }

    @Override
    public SetDatalakeDefaultJavaVersionFlowEvent[] getInitEvents() {
        return new SetDatalakeDefaultJavaVersionFlowEvent[] {
                SET_DATALAKE_DEFAULT_JAVA_VERSION_EVENT
        };
    }

    @Override
    public String getDisplayName() {
        return "Set default Java version for datalake";
    }
}