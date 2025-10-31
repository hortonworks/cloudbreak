package com.sequenceiq.cloudbreak.core.flow2.cluster.update.publicdns;

import com.sequenceiq.cloudbreak.reactor.api.event.cluster.update.publicdns.UpdatePublicDnsEntriesInPemFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.update.publicdns.UpdatePublicDnsEntriesInPemFinished;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;

public enum UpdatePublicDnsEntriesFlowEvent implements FlowEvent {

    UPDATE_PUBLIC_DNS_ENTRIES_TRIGGER_EVENT,
    UPDATE_PUBLIC_DNS_ENTRIES_FAILED_EVENT(EventSelectorUtil.selector(UpdatePublicDnsEntriesInPemFailed.class)),
    UPDATE_PUBLIC_DNS_ENTRIES_FAILURE_HANDLED_EVENT,
    UPDATE_PUBLIC_DNS_ENTRIES_SUCCEEDED_EVENT(EventSelectorUtil.selector(UpdatePublicDnsEntriesInPemFinished.class)),
    UPDATE_PUBLIC_DNS_ENTRIES_FINISHED_EVENT;

    private final String event;

    UpdatePublicDnsEntriesFlowEvent() {
        event = name();
    }

    UpdatePublicDnsEntriesFlowEvent(String event) {
        this.event = event;
    }

    @Override
    public String event() {
        return event;
    }
}
