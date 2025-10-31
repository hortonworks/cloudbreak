package com.sequenceiq.cloudbreak.core.flow2.cluster.update.publicdns;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.update.publicdns.UpdatePublicDnsEntriesFlowEvent.UPDATE_PUBLIC_DNS_ENTRIES_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.update.publicdns.UpdatePublicDnsEntriesFlowEvent.UPDATE_PUBLIC_DNS_ENTRIES_FAILURE_HANDLED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.update.publicdns.UpdatePublicDnsEntriesFlowEvent.UPDATE_PUBLIC_DNS_ENTRIES_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.update.publicdns.UpdatePublicDnsEntriesFlowEvent.UPDATE_PUBLIC_DNS_ENTRIES_SUCCEEDED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.update.publicdns.UpdatePublicDnsEntriesFlowEvent.UPDATE_PUBLIC_DNS_ENTRIES_TRIGGER_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.update.publicdns.UpdatePublicDnsEntriesFlowState.FINAL_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.update.publicdns.UpdatePublicDnsEntriesFlowState.INIT_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.update.publicdns.UpdatePublicDnsEntriesFlowState.UPDATE_PUBLIC_DNS_ENTRIES_FAILED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.update.publicdns.UpdatePublicDnsEntriesFlowState.UPDATE_PUBLIC_DNS_ENTRIES_FINISHED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.update.publicdns.UpdatePublicDnsEntriesFlowState.UPDATE_PUBLIC_DNS_ENTRIES_IN_PEM_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.flow2.StackStatusFinalizerAbstractFlowConfig;
import com.sequenceiq.flow.core.config.RetryableFlowConfiguration;

@Component
public class UpdatePublicDnsEntriesFlowConfig extends StackStatusFinalizerAbstractFlowConfig<UpdatePublicDnsEntriesFlowState, UpdatePublicDnsEntriesFlowEvent>
        implements RetryableFlowConfiguration<UpdatePublicDnsEntriesFlowEvent> {

    private static final List<Transition<UpdatePublicDnsEntriesFlowState, UpdatePublicDnsEntriesFlowEvent>> TRANSITIONS =
            new Transition.Builder<UpdatePublicDnsEntriesFlowState, UpdatePublicDnsEntriesFlowEvent>()
                    .defaultFailureEvent(UPDATE_PUBLIC_DNS_ENTRIES_FAILED_EVENT)

                    .from(INIT_STATE).to(UPDATE_PUBLIC_DNS_ENTRIES_IN_PEM_STATE).event(UPDATE_PUBLIC_DNS_ENTRIES_TRIGGER_EVENT)
                    .defaultFailureEvent()

                    .from(UPDATE_PUBLIC_DNS_ENTRIES_IN_PEM_STATE).to(UPDATE_PUBLIC_DNS_ENTRIES_FINISHED_STATE).event(UPDATE_PUBLIC_DNS_ENTRIES_SUCCEEDED_EVENT)
                    .defaultFailureEvent()

                    .from(UPDATE_PUBLIC_DNS_ENTRIES_FINISHED_STATE).to(FINAL_STATE).event(UPDATE_PUBLIC_DNS_ENTRIES_FINISHED_EVENT)
                    .defaultFailureEvent()
                    .build();

    public UpdatePublicDnsEntriesFlowConfig() {
        super(UpdatePublicDnsEntriesFlowState.class, UpdatePublicDnsEntriesFlowEvent.class);
    }

    @Override
    protected List<Transition<UpdatePublicDnsEntriesFlowState, UpdatePublicDnsEntriesFlowEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    public FlowEdgeConfig<UpdatePublicDnsEntriesFlowState, UpdatePublicDnsEntriesFlowEvent> getEdgeConfig() {
        return new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, UPDATE_PUBLIC_DNS_ENTRIES_FAILED_STATE, UPDATE_PUBLIC_DNS_ENTRIES_FAILURE_HANDLED_EVENT);
    }

    @Override
    public UpdatePublicDnsEntriesFlowEvent[] getEvents() {
        return UpdatePublicDnsEntriesFlowEvent.values();
    }

    @Override
    public UpdatePublicDnsEntriesFlowEvent[] getInitEvents() {
        return new UpdatePublicDnsEntriesFlowEvent[]{
                UPDATE_PUBLIC_DNS_ENTRIES_TRIGGER_EVENT
        };
    }

    @Override
    public String getDisplayName() {
        return "Update public DNS entries for all the nodes of the cluster.";
    }

    @Override
    public UpdatePublicDnsEntriesFlowEvent getRetryableEvent() {
        return UPDATE_PUBLIC_DNS_ENTRIES_FAILURE_HANDLED_EVENT;
    }
}
