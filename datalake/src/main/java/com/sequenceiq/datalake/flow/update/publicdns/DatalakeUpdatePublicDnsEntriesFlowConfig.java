package com.sequenceiq.datalake.flow.update.publicdns;

import static com.sequenceiq.datalake.flow.update.publicdns.DatalakeUpdatePublicDnsEntriesFlowEvent.DATALAKE_UPDATE_PUBLIC_DNS_ENTRIES_EVENT;
import static com.sequenceiq.datalake.flow.update.publicdns.DatalakeUpdatePublicDnsEntriesFlowEvent.DATALAKE_UPDATE_PUBLIC_DNS_ENTRIES_FAILED_EVENT;
import static com.sequenceiq.datalake.flow.update.publicdns.DatalakeUpdatePublicDnsEntriesFlowEvent.DATALAKE_UPDATE_PUBLIC_DNS_ENTRIES_FAIL_HANDLED_EVENT;
import static com.sequenceiq.datalake.flow.update.publicdns.DatalakeUpdatePublicDnsEntriesFlowEvent.DATALAKE_UPDATE_PUBLIC_DNS_ENTRIES_FINALIZED_EVENT;
import static com.sequenceiq.datalake.flow.update.publicdns.DatalakeUpdatePublicDnsEntriesFlowEvent.DATALAKE_UPDATE_PUBLIC_DNS_ENTRIES_FINISHED_EVENT;
import static com.sequenceiq.datalake.flow.update.publicdns.DatalakeUpdatePublicDnsEntriesFlowState.DATALAKE_UPDATE_PUBLIC_DNS_ENTRIES_FAILED_STATE;
import static com.sequenceiq.datalake.flow.update.publicdns.DatalakeUpdatePublicDnsEntriesFlowState.DATALAKE_UPDATE_PUBLIC_DNS_ENTRIES_FINISHED_STATE;
import static com.sequenceiq.datalake.flow.update.publicdns.DatalakeUpdatePublicDnsEntriesFlowState.DATALAKE_UPDATE_PUBLIC_DNS_ENTRIES_STATE;
import static com.sequenceiq.datalake.flow.update.publicdns.DatalakeUpdatePublicDnsEntriesFlowState.FINAL_STATE;
import static com.sequenceiq.datalake.flow.update.publicdns.DatalakeUpdatePublicDnsEntriesFlowState.INIT_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;

@Component
public class DatalakeUpdatePublicDnsEntriesFlowConfig
        extends AbstractFlowConfiguration<DatalakeUpdatePublicDnsEntriesFlowState, DatalakeUpdatePublicDnsEntriesFlowEvent> {

    public static final FlowEdgeConfig<DatalakeUpdatePublicDnsEntriesFlowState, DatalakeUpdatePublicDnsEntriesFlowEvent> EDGE_CONFIG =
            new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, DATALAKE_UPDATE_PUBLIC_DNS_ENTRIES_FAILED_STATE,
                    DATALAKE_UPDATE_PUBLIC_DNS_ENTRIES_FAIL_HANDLED_EVENT);

    private static final List<AbstractFlowConfiguration.Transition<DatalakeUpdatePublicDnsEntriesFlowState, DatalakeUpdatePublicDnsEntriesFlowEvent>>
            TRANSITIONS = new AbstractFlowConfiguration.Transition.Builder<DatalakeUpdatePublicDnsEntriesFlowState, DatalakeUpdatePublicDnsEntriesFlowEvent>()
            .defaultFailureEvent(DATALAKE_UPDATE_PUBLIC_DNS_ENTRIES_FAILED_EVENT)

            .from(INIT_STATE)
            .to(DATALAKE_UPDATE_PUBLIC_DNS_ENTRIES_STATE)
            .event(DATALAKE_UPDATE_PUBLIC_DNS_ENTRIES_EVENT)
            .defaultFailureEvent()

            .from(DATALAKE_UPDATE_PUBLIC_DNS_ENTRIES_STATE)
            .to(DATALAKE_UPDATE_PUBLIC_DNS_ENTRIES_FINISHED_STATE)
            .event(DATALAKE_UPDATE_PUBLIC_DNS_ENTRIES_FINISHED_EVENT)
            .defaultFailureEvent()

            .from(DATALAKE_UPDATE_PUBLIC_DNS_ENTRIES_FINISHED_STATE)
            .to(FINAL_STATE)
            .event(DATALAKE_UPDATE_PUBLIC_DNS_ENTRIES_FINALIZED_EVENT)
            .defaultFailureEvent()
            .build();

    protected DatalakeUpdatePublicDnsEntriesFlowConfig() {
        super(DatalakeUpdatePublicDnsEntriesFlowState.class, DatalakeUpdatePublicDnsEntriesFlowEvent.class);
    }

    @Override
    protected List<Transition<DatalakeUpdatePublicDnsEntriesFlowState, DatalakeUpdatePublicDnsEntriesFlowEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    public FlowEdgeConfig<DatalakeUpdatePublicDnsEntriesFlowState, DatalakeUpdatePublicDnsEntriesFlowEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public DatalakeUpdatePublicDnsEntriesFlowEvent[] getEvents() {
        return DatalakeUpdatePublicDnsEntriesFlowEvent.values();
    }

    @Override
    public DatalakeUpdatePublicDnsEntriesFlowEvent[] getInitEvents() {
        return new DatalakeUpdatePublicDnsEntriesFlowEvent[] {
            DATALAKE_UPDATE_PUBLIC_DNS_ENTRIES_EVENT
        };
    }

    @Override
    public String getDisplayName() {
        return "Update public DNS entries for Datalake";
    }
}
