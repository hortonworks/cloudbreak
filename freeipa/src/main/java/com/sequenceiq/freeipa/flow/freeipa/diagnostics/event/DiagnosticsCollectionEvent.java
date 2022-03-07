package com.sequenceiq.freeipa.flow.freeipa.diagnostics.event;

import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.common.model.diagnostics.DiagnosticParameters;
import com.sequenceiq.flow.reactor.api.event.BaseFlowEvent;

import reactor.rx.Promise;

public class DiagnosticsCollectionEvent extends BaseFlowEvent {

    private final DiagnosticParameters parameters;

    public DiagnosticsCollectionEvent(String selector, Long resourceId, String resourceCrn, DiagnosticParameters parameters) {
        super(selector, resourceId, resourceCrn);
        this.parameters = parameters;
    }

    public DiagnosticsCollectionEvent(String selector, Long resourceId, String resourceCrn, Promise<AcceptResult> accepted, DiagnosticParameters parameters) {
        super(selector, resourceId, resourceCrn, accepted);
        this.parameters = parameters;
    }

    public static DiagnosticsCollectionEventBuilder builder() {
        return new DiagnosticsCollectionEventBuilder();
    }

    public DiagnosticParameters getParameters() {
        return parameters;
    }

    @Override
    public String toString() {
        return "DiagnosticsCollectionEvent{" +
                "parameters=" + parameters +
                "} " + super.toString();
    }

    public static final class DiagnosticsCollectionEventBuilder {

        private String resourceCrn;

        private String selector;

        private Long resourceId;

        private Promise<AcceptResult> accepted;

        private DiagnosticParameters parameters;

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

        public DiagnosticsCollectionEvent build() {
            return new DiagnosticsCollectionEvent(selector, resourceId, resourceCrn, accepted, parameters);
        }
    }
}
