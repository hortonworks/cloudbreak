package com.sequenceiq.freeipa.flow.freeipa.cleanup.event.dns;

import java.util.Map;
import java.util.Set;

import com.sequenceiq.freeipa.flow.freeipa.cleanup.CleanupEvent;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.event.AbstractCleanupEvent;

public class RemoveDnsResponse extends AbstractCleanupEvent {

    private Set<String> dnsCleanupSuccess;

    private Map<String, String> dnsCleanupFailed;

    public RemoveDnsResponse(Long stackId) {
        super(stackId);
    }

    public RemoveDnsResponse(CleanupEvent cleanupEvent, Set<String> dnsCleanupSuccess, Map<String, String> dnsCleanupFailed) {
        super(cleanupEvent);
        this.dnsCleanupSuccess = dnsCleanupSuccess;
        this.dnsCleanupFailed = dnsCleanupFailed;
    }

    public RemoveDnsResponse(String selector, CleanupEvent cleanupEvent, Set<String> dnsCleanupSuccess,
            Map<String, String> dnsCleanupFailed) {
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
