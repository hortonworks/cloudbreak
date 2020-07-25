package com.sequenceiq.cloudbreak.core.flow2.diagnostics.event;

import java.util.Map;
import java.util.Set;

import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.flow.reactor.api.event.BaseFlowEvent;

import reactor.rx.Promise;

public class DiagnosticsCollectionEvent extends BaseFlowEvent {

    private final Map<String, Object> parameters;

    private final Set<String> hosts;

    private final Set<String> instanceGroups;

    DiagnosticsCollectionEvent(String selector, Long resourceId, String resourceCrn, Map<String, Object> parameters,
            Set<String> hosts, Set<String> instanceGroups) {
        super(selector, resourceId, resourceCrn);
        this.parameters = parameters;
        this.hosts = hosts;
        this.instanceGroups = instanceGroups;
    }

    DiagnosticsCollectionEvent(String selector, Long resourceId, String resourceCrn, Promise<AcceptResult> accepted,
            Map<String, Object> parameters, Set<String> hosts, Set<String> instanceGroups) {
        super(selector, resourceId, resourceCrn, accepted);
        this.parameters = parameters;
        this.hosts = hosts;
        this.instanceGroups = instanceGroups;
    }

    public static DiagnosticsCollectionEventBuilder builder() {
        return new DiagnosticsCollectionEventBuilder();
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public Set<String> getHosts() {
        return hosts;
    }

    public Set<String> getInstanceGroups() {
        return instanceGroups;
    }

    public static final class DiagnosticsCollectionEventBuilder {

        private String resourceCrn;

        private String selector;

        private Long resourceId;

        private Promise<AcceptResult> accepted;

        private Map<String, Object> parameters;

        private Set<String> hosts;

        private Set<String> instanceGroups;

        private DiagnosticsCollectionEventBuilder() {
        }

        public DiagnosticsCollectionEventBuilder withSelector(String selector) {
            this.selector = selector;
            return this;
        }

        public DiagnosticsCollectionEventBuilder withResourceId(Long resourceId) {
            this.resourceId = resourceId;
            return this;
        }

        public DiagnosticsCollectionEventBuilder withResourceCrn(String resourceCrn) {
            this.resourceCrn = resourceCrn;
            return this;
        }

        public DiagnosticsCollectionEventBuilder withAccepted(Promise<AcceptResult> accepted) {
            this.accepted = accepted;
            return this;
        }

        public DiagnosticsCollectionEventBuilder withParameters(Map<String, Object> parameters) {
            this.parameters = parameters;
            return this;
        }

        public DiagnosticsCollectionEventBuilder withHosts(Set<String> hosts) {
            this.hosts = hosts;
            return this;
        }

        public DiagnosticsCollectionEventBuilder withInstanceGroups(Set<String> instanceGroups) {
            this.instanceGroups = instanceGroups;
            return this;
        }

        public DiagnosticsCollectionEvent build() {
            return new DiagnosticsCollectionEvent(selector, resourceId, resourceCrn, accepted,
                    parameters, hosts, instanceGroups);
        }
    }
}
