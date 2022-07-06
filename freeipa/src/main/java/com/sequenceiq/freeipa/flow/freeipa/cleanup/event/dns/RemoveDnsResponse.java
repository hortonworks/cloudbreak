package com.sequenceiq.freeipa.flow.freeipa.cleanup.event.dns;

import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.CleanupEvent;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.event.AbstractCleanupEvent;

public class RemoveDnsResponse extends AbstractCleanupEvent {

    private final Set<String> dnsCleanupSuccess;

    private final Map<String, String> dnsCleanupFailed;

    public RemoveDnsResponse(Long stackId) {
        super(stackId);
        dnsCleanupSuccess = null;
        dnsCleanupFailed = null;
    }

    public RemoveDnsResponse(CleanupEvent cleanupEvent, Set<String> dnsCleanupSuccess, Map<String, String> dnsCleanupFailed) {
        super(cleanupEvent);
        this.dnsCleanupSuccess = dnsCleanupSuccess;
        this.dnsCleanupFailed = dnsCleanupFailed;
    }

    @JsonCreator
    public RemoveDnsResponse(
            @JsonProperty("selector") String selector,
            @JsonProperty("cleanupEvent") CleanupEvent cleanupEvent,
            @JsonProperty("dnsCleanupSuccess") Set<String> dnsCleanupSuccess,
            @JsonProperty("dnsCleanupFailed") Map<String, String> dnsCleanupFailed) {
        super(selector, cleanupEvent);
        this.dnsCleanupSuccess = dnsCleanupSuccess;
        this.dnsCleanupFailed = dnsCleanupFailed;
    }

    public Set<String> getDnsCleanupSuccess() {
        return dnsCleanupSuccess;
    }

    public Map<String, String> getDnsCleanupFailed() {
        return dnsCleanupFailed;
    }

    @Override
    public String toString() {
        return "RemoveDnsResponse{" +
                "dnsCleanupSuccess=" + dnsCleanupSuccess +
                ", dnsCleanupFailed=" + dnsCleanupFailed +
                '}';
    }
}
