package com.sequenceiq.freeipa.flow.freeipa.cleanup;

import static com.sequenceiq.freeipa.flow.freeipa.cleanup.FreeIpaCleanupEvent.CLEANUP_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.cleanup.FreeIpaCleanupEvent.CLEANUP_FAILED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.cleanup.FreeIpaCleanupEvent.CLEANUP_FAILURE_HANDLED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.cleanup.FreeIpaCleanupEvent.CLEANUP_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.cleanup.FreeIpaCleanupEvent.REMOVE_DNS_ENTRIES_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.cleanup.FreeIpaCleanupEvent.REMOVE_DNS_FAILED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.cleanup.FreeIpaCleanupEvent.REMOVE_HOSTS_FAILED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.cleanup.FreeIpaCleanupEvent.REMOVE_HOSTS_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.cleanup.FreeIpaCleanupEvent.REMOVE_ROLES_FAILED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.cleanup.FreeIpaCleanupEvent.REMOVE_ROLES_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.cleanup.FreeIpaCleanupEvent.REMOVE_USERS_FAILED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.cleanup.FreeIpaCleanupEvent.REMOVE_USERS_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.cleanup.FreeIpaCleanupEvent.REMOVE_VAULT_ENTRIES_FAILED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.cleanup.FreeIpaCleanupEvent.REMOVE_VAULT_ENTRIES_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.cleanup.FreeIpaCleanupEvent.REVOKE_CERTS_FAILED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.cleanup.FreeIpaCleanupEvent.REVOKE_CERTS_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.cleanup.FreeIpaCleanupState.CLEANUP_FAILED_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.cleanup.FreeIpaCleanupState.CLEANUP_FINISHED_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.cleanup.FreeIpaCleanupState.FINAL_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.cleanup.FreeIpaCleanupState.INIT_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.cleanup.FreeIpaCleanupState.REMOVE_DNS_ENTRIES_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.cleanup.FreeIpaCleanupState.REMOVE_HOSTS_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.cleanup.FreeIpaCleanupState.REMOVE_ROLES_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.cleanup.FreeIpaCleanupState.REMOVE_USERS_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.cleanup.FreeIpaCleanupState.REMOVE_VAULT_ENTRIES_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.cleanup.FreeIpaCleanupState.REVOKE_CERTS_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.flow.core.config.AbstractFlowConfiguration.Transition.Builder;
import com.sequenceiq.flow.core.config.RetryableFlowConfiguration;
import com.sequenceiq.freeipa.flow.StackStatusFinalizerAbstractFlowConfig;

@Component
public class FreeIpaCleanupFlowConfig extends StackStatusFinalizerAbstractFlowConfig<FreeIpaCleanupState, FreeIpaCleanupEvent>
        implements RetryableFlowConfiguration<FreeIpaCleanupEvent> {

    private static final FreeIpaCleanupEvent[] FREEIPA_INIT_EVENTS = {CLEANUP_EVENT};

    private static final FlowEdgeConfig<FreeIpaCleanupState, FreeIpaCleanupEvent> EDGE_CONFIG =
            new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, CLEANUP_FAILED_STATE, CLEANUP_FAILURE_HANDLED_EVENT);

    private static final List<Transition<FreeIpaCleanupState, FreeIpaCleanupEvent>> TRANSITIONS =
            new Builder<FreeIpaCleanupState, FreeIpaCleanupEvent>().defaultFailureEvent(CLEANUP_FAILED_EVENT)
            .from(INIT_STATE).to(REVOKE_CERTS_STATE).event(CLEANUP_EVENT).defaultFailureEvent()
            .from(REVOKE_CERTS_STATE).to(REMOVE_HOSTS_STATE).event(REVOKE_CERTS_FINISHED_EVENT).failureEvent(REVOKE_CERTS_FAILED_EVENT)
            .from(REMOVE_HOSTS_STATE).to(REMOVE_DNS_ENTRIES_STATE).event(REMOVE_HOSTS_FINISHED_EVENT).failureEvent(REMOVE_HOSTS_FAILED_EVENT)
            .from(REMOVE_DNS_ENTRIES_STATE).to(REMOVE_VAULT_ENTRIES_STATE).event(REMOVE_DNS_ENTRIES_FINISHED_EVENT).failureEvent(REMOVE_DNS_FAILED_EVENT)
            .from(REMOVE_VAULT_ENTRIES_STATE).to(REMOVE_USERS_STATE).event(REMOVE_VAULT_ENTRIES_FINISHED_EVENT).failureEvent(REMOVE_VAULT_ENTRIES_FAILED_EVENT)
            .from(REMOVE_USERS_STATE).to(REMOVE_ROLES_STATE).event(REMOVE_USERS_FINISHED_EVENT).failureEvent(REMOVE_USERS_FAILED_EVENT)
            .from(REMOVE_ROLES_STATE).to(CLEANUP_FINISHED_STATE).event(REMOVE_ROLES_FINISHED_EVENT).failureEvent(REMOVE_ROLES_FAILED_EVENT)
            .from(CLEANUP_FINISHED_STATE).to(FINAL_STATE).event(CLEANUP_FINISHED_EVENT).defaultFailureEvent()
            .build();

    public FreeIpaCleanupFlowConfig() {
        super(FreeIpaCleanupState.class, FreeIpaCleanupEvent.class);
    }

    @Override
    protected List<Transition<FreeIpaCleanupState, FreeIpaCleanupEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    public FlowEdgeConfig<FreeIpaCleanupState, FreeIpaCleanupEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public FreeIpaCleanupEvent[] getEvents() {
        return FreeIpaCleanupEvent.values();
    }

    @Override
    public FreeIpaCleanupEvent[] getInitEvents() {
        return FREEIPA_INIT_EVENTS;
    }

    @Override
    public String getDisplayName() {
        return "Cleanup FreeIPA";
    }

    @Override
    public FreeIpaCleanupEvent getRetryableEvent() {
        return EDGE_CONFIG.getFailureHandled();
    }
}
