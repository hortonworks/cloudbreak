package com.sequenceiq.cloudbreak.core.flow2.diagnostics.event;

import java.util.Set;

import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.common.model.diagnostics.DiagnosticParameters;
import com.sequenceiq.flow.reactor.api.event.BaseFlowEvent;

import reactor.rx.Promise;

public class DiagnosticsCollectionEvent extends BaseFlowEvent {

    private final DiagnosticParameters parameters;

    private final Set<String> hosts;

    private final Set<String> hostGroups;

    private final Set<String> excludeHosts;

    public DiagnosticsCollectionEvent(String selector, Long resourceId, String resourceCrn, DiagnosticParameters parameters,
            Set<String> hosts, Set<String> hostGroups, Set<String> excludeHosts) {
        super(selector, resourceId, resourceCrn);
        this.parameters = parameters;
        this.hosts = hosts;
        this.hostGroups = hostGroups;
        this.excludeHosts = excludeHosts;
    }

    public DiagnosticsCollectionEvent(String selector, Long resourceId, String resourceCrn, Promise<AcceptResult> accepted,
            DiagnosticParameters parameters, Set<String> hosts, Set<String> hostGroups, Set<String> excludeHosts) {
        super(selector, resourceId, resourceCrn, accepted);
        this.parameters = parameters;
        this.hosts = hosts;
        this.hostGroups = hostGroups;
        this.excludeHosts = excludeHosts;
    }

    public static DiagnosticsCollectionEventBuilder builder() {
        return new DiagnosticsCollectionEventBuilder();
    }

    public DiagnosticParameters getParameters() {
        return parameters;
    }

    public Set<String> getHosts() {
        return hosts;
    }

    public Set<String> getHostGroups() {
        return hostGroups;
    }

    public Set<String> getExcludeHosts() {
        return excludeHosts;
    }

    @Override
    public String toString() {
        return "DiagnosticsCollectionEvent{" +
                "parameters=" + parameters +
                ", hosts=" + hosts +
                ", hostGroups=" + hostGroups +
                ", excludeHosts=" + excludeHosts +
                "} " + super.toString();
    }

    public static final class DiagnosticsCollectionEventBuilder {

        private String resourceCrn;

        private String selector;

        private Long resourceId;

        private Promise<AcceptResult> accepted;

        private DiagnosticParameters parameters;

        private Set<String> hosts;

        private Set<String> hostGroups;

        private Set<String> excludeHosts;

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

        public DiagnosticsCollectionEventBuilder withParameters(DiagnosticParameters parameters) {
            this.parameters = parameters;
            return this;
        }

        public DiagnosticsCollectionEventBuilder withHosts(Set<String> hosts) {
            this.hosts = hosts;
            return this;
        }

        public DiagnosticsCollectionEventBuilder withHostGroups(Set<String> hostGroups) {
            this.hostGroups = hostGroups;
            return this;
        }

        public DiagnosticsCollectionEventBuilder withExcludeHosts(Set<String> excludeHosts) {
            this.excludeHosts = excludeHosts;
            return this;
        }

        public DiagnosticsCollectionEvent build() {
            return new DiagnosticsCollectionEvent(selector, resourceId, resourceCrn, accepted,
                    parameters, hosts, hostGroups, excludeHosts);
        }
    }
}
