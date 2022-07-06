package com.sequenceiq.freeipa.flow.freeipa.cleanup.event.cert;

import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.CleanupEvent;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.event.AbstractCleanupEvent;

public class RevokeCertsResponse extends AbstractCleanupEvent {

    private final Set<String> certCleanupSuccess;

    private final Map<String, String> certCleanupFailed;

    protected RevokeCertsResponse(Long stackId) {
        super(stackId);
        certCleanupSuccess = null;
        certCleanupFailed = null;
    }

    public RevokeCertsResponse(CleanupEvent cleanupEvent, Set<String> certCleanupSuccess, Map<String, String> certCleanupFailed) {
        super(cleanupEvent);
        this.certCleanupSuccess = certCleanupSuccess;
        this.certCleanupFailed = certCleanupFailed;
    }

    @JsonCreator
    public RevokeCertsResponse(
            @JsonProperty("selector") String selector,
            @JsonProperty("cleanupEvent") CleanupEvent cleanupEvent,
            @JsonProperty("certCleanupSuccess") Set<String> certCleanupSuccess,
            @JsonProperty("certCleanupFailed") Map<String, String> certCleanupFailed) {
        super(selector, cleanupEvent);
        this.certCleanupSuccess = certCleanupSuccess;
        this.certCleanupFailed = certCleanupFailed;
    }

    public Set<String> getCertCleanupSuccess() {
        return certCleanupSuccess;
    }

    public Map<String, String> getCertCleanupFailed() {
        return certCleanupFailed;
    }

    @Override
    public String toString() {
        return "RevokeCertsResponse{" +
                "certCleanupSuccess=" + certCleanupSuccess +
                ", certCleanupFailed=" + certCleanupFailed +
                '}';
    }
}
