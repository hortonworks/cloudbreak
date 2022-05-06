package com.sequenceiq.consumption.flow.consumption.storage.event;

import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.flow.reactor.api.event.BaseFlowEvent;

import reactor.rx.Promise;

public class StorageConsumptionCollectionEvent extends BaseFlowEvent {

    public StorageConsumptionCollectionEvent(String selector, Long resourceId, String resourceCrn) {
        super(selector, resourceId, resourceCrn);
    }

    public StorageConsumptionCollectionEvent(String selector, Long resourceId, String resourceCrn, Promise<AcceptResult> accepted) {
        super(selector, resourceId, resourceCrn, accepted);
    }

    @Override
    public String toString() {
        return "StorageConsumptionCollectionEvent{} " + super.toString();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private String resourceCrn;

        private String selector;

        private Long resourceId;

        private Promise<AcceptResult> accepted;

        private Builder() {
        }

        public Builder withSelector(String selector) {
            this.selector = selector;
            return this;
        }

        public Builder withResourceId(Long resourceId) {
            this.resourceId = resourceId;
            return this;
        }

        public Builder withAccepted(Promise<AcceptResult> accepted) {
            this.accepted = accepted;
            return this;
        }

        public Builder withResourceCrn(String resourceCrn) {
            this.resourceCrn = resourceCrn;
            return this;
        }

        public StorageConsumptionCollectionEvent build() {
            return new StorageConsumptionCollectionEvent(selector, resourceId, resourceCrn, accepted);
        }
    }
}
