package com.sequenceiq.cloudbreak.core.flow2.cmdiagnostics.event;

import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.common.model.diagnostics.CmDiagnosticsParameters;
import com.sequenceiq.flow.reactor.api.event.BaseFlowEvent;

import reactor.rx.Promise;

public class CmDiagnosticsCollectionEvent extends BaseFlowEvent {

    private final CmDiagnosticsParameters parameters;

    public CmDiagnosticsCollectionEvent(String selector, Long resourceId, String resourceCrn, CmDiagnosticsParameters parameters) {
        super(selector, resourceId, resourceCrn);
        this.parameters = parameters;
    }

    public CmDiagnosticsCollectionEvent(String selector, Long resourceId, String resourceCrn, Promise<AcceptResult> accepted,
            CmDiagnosticsParameters parameters) {
        super(selector, resourceId, resourceCrn, accepted);
        this.parameters = parameters;
    }

    public CmDiagnosticsParameters getParameters() {
        return parameters;
    }

    public static CmDiagnosticsCollectionEventBuilder builder() {
        return new CmDiagnosticsCollectionEventBuilder();
    }

    public static final class CmDiagnosticsCollectionEventBuilder {

        private String resourceCrn;

        private String selector;

        private Long resourceId;

        private Promise<AcceptResult> accepted;

        private CmDiagnosticsParameters parameters;

        private CmDiagnosticsCollectionEventBuilder() {
        }

        public CmDiagnosticsCollectionEventBuilder withSelector(String selector) {
            this.selector = selector;
            return this;
        }

        public CmDiagnosticsCollectionEventBuilder withResourceCrn(String resourceCrn) {
            this.resourceCrn = resourceCrn;
            return this;
        }

        public CmDiagnosticsCollectionEventBuilder withResourceId(Long resourceId) {
            this.resourceId = resourceId;
            return this;
        }

        public CmDiagnosticsCollectionEventBuilder withAccepted(Promise<AcceptResult> accepted) {
            this.accepted = accepted;
            return this;
        }

        public CmDiagnosticsCollectionEventBuilder withParameters(CmDiagnosticsParameters parameters) {
            this.parameters = parameters;
            return this;
        }

        public CmDiagnosticsCollectionEvent build() {
            return new CmDiagnosticsCollectionEvent(selector, resourceId, resourceCrn, accepted, parameters);
        }
    }
}
