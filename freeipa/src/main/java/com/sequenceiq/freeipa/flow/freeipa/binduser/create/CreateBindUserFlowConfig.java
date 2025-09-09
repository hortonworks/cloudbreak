package com.sequenceiq.freeipa.flow.freeipa.binduser.create;

import static com.sequenceiq.freeipa.flow.freeipa.binduser.create.CreateBindUserState.CREATE_BIND_USER_FAILED_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.binduser.create.CreateBindUserState.CREATE_BIND_USER_FINISHED_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.binduser.create.CreateBindUserState.CREATE_KERBEROS_BIND_USER_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.binduser.create.CreateBindUserState.CREATE_LDAP_BIND_USER_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.binduser.create.CreateBindUserState.FINAL_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.binduser.create.CreateBindUserState.INIT_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.binduser.create.event.CreateBindUserFlowEvent.CREATE_BIND_USER_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.binduser.create.event.CreateBindUserFlowEvent.CREATE_BIND_USER_FAILED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.binduser.create.event.CreateBindUserFlowEvent.CREATE_BIND_USER_FAILURE_HANDLED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.binduser.create.event.CreateBindUserFlowEvent.CREATE_BIND_USER_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.binduser.create.event.CreateBindUserFlowEvent.CREATE_KERBEROS_BIND_USER_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.binduser.create.event.CreateBindUserFlowEvent.CREATE_LDAP_BIND_USER_FINISHED_EVENT;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.flow.core.config.RetryableFlowConfiguration;
import com.sequenceiq.freeipa.flow.StackStatusFinalizerAbstractFlowConfig;
import com.sequenceiq.freeipa.flow.freeipa.binduser.create.event.CreateBindUserFlowEvent;

@Component
public class CreateBindUserFlowConfig extends StackStatusFinalizerAbstractFlowConfig<CreateBindUserState, CreateBindUserFlowEvent>
        implements RetryableFlowConfiguration<CreateBindUserFlowEvent> {

    private static final CreateBindUserFlowEvent[] INIT_EVENTS = {CREATE_BIND_USER_EVENT};

    private static final FlowEdgeConfig<CreateBindUserState, CreateBindUserFlowEvent> EDGE_CONFIG =
            new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, CREATE_BIND_USER_FAILED_STATE, CREATE_BIND_USER_FAILURE_HANDLED_EVENT);

    private static final List<Transition<CreateBindUserState, CreateBindUserFlowEvent>> TRANSITIONS =
            new Transition.Builder<CreateBindUserState, CreateBindUserFlowEvent>().defaultFailureEvent(CREATE_BIND_USER_FAILED_EVENT)
                    .from(INIT_STATE).to(CREATE_KERBEROS_BIND_USER_STATE).event(CREATE_BIND_USER_EVENT).defaultFailureEvent()
                    .from(CREATE_KERBEROS_BIND_USER_STATE).to(CREATE_LDAP_BIND_USER_STATE).event(CREATE_KERBEROS_BIND_USER_FINISHED_EVENT).defaultFailureEvent()
                    .from(CREATE_LDAP_BIND_USER_STATE).to(CREATE_BIND_USER_FINISHED_STATE).event(CREATE_LDAP_BIND_USER_FINISHED_EVENT).defaultFailureEvent()
                    .from(CREATE_BIND_USER_FINISHED_STATE).to(FINAL_STATE).event(CREATE_BIND_USER_FINISHED_EVENT).defaultFailureEvent()
                    .build();

    public CreateBindUserFlowConfig() {
        super(CreateBindUserState.class, CreateBindUserFlowEvent.class);
    }

    @Override
    public CreateBindUserFlowEvent[] getInitEvents() {
        return INIT_EVENTS;
    }

    @Override
    public CreateBindUserFlowEvent[] getEvents() {
        return CreateBindUserFlowEvent.values();
    }

    @Override
    public String getDisplayName() {
        return "Bind user creation";
    }

    @Override
    public FlowEdgeConfig<CreateBindUserState, CreateBindUserFlowEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    protected List<Transition<CreateBindUserState, CreateBindUserFlowEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    public CreateBindUserFlowEvent getRetryableEvent() {
        return EDGE_CONFIG.getFailureHandled();
    }
}
