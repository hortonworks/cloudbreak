package com.sequenceiq.datalake.flow.update.publicdns;

import com.sequenceiq.datalake.flow.update.publicdns.handler.WaitDatalakeUpdateDnsEntriesResponse;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;

public enum DatalakeUpdatePublicDnsEntriesFlowEvent implements FlowEvent {

    DATALAKE_UPDATE_PUBLIC_DNS_ENTRIES_EVENT,
    DATALAKE_UPDATE_PUBLIC_DNS_ENTRIES_FINISHED_EVENT(EventSelectorUtil.selector(WaitDatalakeUpdateDnsEntriesResponse.class)),
    DATALAKE_UPDATE_PUBLIC_DNS_ENTRIES_FINALIZED_EVENT,
    DATALAKE_UPDATE_PUBLIC_DNS_ENTRIES_FAIL_HANDLED_EVENT,
    DATALAKE_UPDATE_PUBLIC_DNS_ENTRIES_FAILED_EVENT(EventSelectorUtil.selector(DatalakeUpdatePublicDnsEntriesFailedEvent.class));

    private final String event;

    DatalakeUpdatePublicDnsEntriesFlowEvent() {
        this.event = name();
    }

    DatalakeUpdatePublicDnsEntriesFlowEvent(String event) {
        this.event = event;
    }

    @Override
    public String event() {
        return event;
    }
}
